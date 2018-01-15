package thesaurusFormalizer.alignment.senseSelection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import net.didion.jwnl.data.Synset;
import rdfProcessing.Bean_ModelContainer;
import thesaurusFormalizer.rdfManager.ThesFormalizerRDFPropertyManager;
import tools.wordnetManager.EnglishWordnetManager;

/**
 * procesa los synsets que no han sido filtrados
 * y selecciona el má adecuado en funcion de si comparten jerarquia
 * con otros synsets ya seleccionados para otros recursos 
 */
public class ItemProcessor_ContextSynsetSelection implements ItemProcessor<Resource, Resource>, StepExecutionListener{
	private static final Property exact_wordnetMatch = ThesFormalizerRDFPropertyManager.exact_wordnetMatch; 
	private static final Property isa_wordnetMatch = ThesFormalizerRDFPropertyManager.isa_wordnetMatch;
	private static final Property inner_possibleLabel_Synset = ThesFormalizerRDFPropertyManager.inner_possibleLabel_Synset;
	
	private static final Property skosHasTopConcProp = ThesFormalizerRDFPropertyManager.skosHasTopConcProp;
	private static final Property skosNarrowerProp = ThesFormalizerRDFPropertyManager.skosNarrowerProp;
	
	
	private Bean_ModelContainer model=null;
	
	private HashMap<String, Double> ocurrencias = new HashMap<String, Double>();
	private HashMap<String, Double> distancia = new HashMap<String, Double>();
	private HashMap<String,HashMap<String, Double>> ocurrenciasBranch = new HashMap<String,HashMap<String, Double>>();
	private HashMap<String,HashMap<String, Double>> distanciaBranch = new HashMap<String,HashMap<String, Double>>();
	
	
	//modelo acceso a wordnet
	private EnglishWordnetManager jwnl = new EnglishWordnetManager();
	
	/**
	 * procesa los synsets que no han sido filtrados
	 * y selecciona el má adecuado en funcion de si comparten jerarquia
	 * con otros synsets ya seleccionados para otros recursos 
	 */
	public Resource process(Resource item) throws Exception {
		Statement possible = item.getProperty(inner_possibleLabel_Synset);
		
		if(possible!=null){
			String[] matches=possible.getString().split("\\|");
			
			
			
			//obtenemos cuantas veces el concepto es padre de otros ya asignados
			//si no hemos encontrado que sea padre de nada miramos de cuantos su padre lo es
			//y si aún así no lo es, vemos de cuantos lo es su abuelo
			//2 = abuelos, 3 =bisabuelo
			List<String> value = null;
			int count =-1;
			for(;count<2;){
				value = getBestSelectionBasedOnNeighbourghs(matches,++count,item.getURI());
				if(value.size()==1){break;}
			}
				
			//dependiendo del numero de coincidencias encontrado creamos directamente
			//el enlace o reducimos los posibles synsets para posteriores procesos
			if(value.size()==1){
				String type = possible.getLiteral().getDatatypeURI();
				if(type.endsWith("_O")||type.endsWith("_S" )){
					item.addLiteral(exact_wordnetMatch, item.getModel().createTypedLiteral(value.get(0),"http://www.iaaa.es/hierarchyContext_"+count+"_Match"));
				}else{
					item.addLiteral(isa_wordnetMatch, item.getModel().createTypedLiteral(value.get(0),"http://www.iaaa.es/hierarchyContext_"+count+"_Match"));
				}
				item.removeAll(inner_possibleLabel_Synset);
			}else if(value.size()>1){
				String text =value.get(0);
				value.remove(0); for(String s:value){text+="|"+s;}
				String type = possible.getLiteral().getDatatypeURI();
				int separator= type.lastIndexOf("_");
				type = type.substring(0,separator)+"_hierarchy_"+count+"_"+type.substring(separator);
				item.removeAll(inner_possibleLabel_Synset);
				item.addProperty(inner_possibleLabel_Synset, item.getModel().createTypedLiteral(text,type));
			}
		}
		
		return item;
	}
	
	
	
	
	/*******************************************************************/
	/**
	 * devuelve el mejor synset de entre los posibles teniendo en cuenta
	 * cuantos ya alineados hay a su alrrededor
	 */
	private List<String> getBestSelectionBasedOnNeighbourghs(String[] matches, int neighborLebel, String item){
		HashMap<Synset,String> ancestors = new HashMap<Synset,String>();
		//guardamos los synsets a obtener su relevancia en una lista
		for(String match:matches){	
			ancestors.put(jwnl.getSynset(Long.parseLong(match)),match);
		}
		
		//obtenemos los synsets antecesores a la distancia indicada
		for(int i=0;i<neighborLebel;i++){
			HashMap<Synset,String> fathers = new HashMap<Synset,String>();
			for(Synset conc:ancestors.keySet()){
				for(Synset syn: jwnl.getDirectHypernym(conc)){
					fathers.put(syn, ancestors.get(conc));
				}
			}
			ancestors=fathers;
		}
		
		//buscamos el más adecuado de entre los synsets de su rama
		//si hay mas de uno miramos cual es que tiene los conceptos relacionados más cerca.
		List<String> closestSynsets = getClosestSynsets(ancestors,ocurrenciasBranch.get(item));
		if(closestSynsets.size()>1){
			closestSynsets = selectClosestAlternative(closestSynsets, distanciaBranch.get(item));
		//si no hay elegimos el que tenga más en todo el tesauro
		}else if(closestSynsets.size()==0){
			closestSynsets = getClosestSynsets(ancestors,ocurrencias);
			if(closestSynsets.size()>1){
				closestSynsets = selectClosestAlternative(closestSynsets, distancia);
			}
		}
		
		return closestSynsets;
	}
	
	/****************************************************************
	/**
	 * obtiene el/los synsets mas cercanos a los ya alienados de cuerdo a
	 * los pesos indicados
	 */
	private List<String> getClosestSynsets(HashMap<Synset,String> ancestors, HashMap<String, Double> ocurr){
		List<String> closestSynsets = new ArrayList<String>();
		double maxCount=0.01;
		for(Synset possible:ancestors.keySet()){
			double veces =0;
			String posibleId = Long.toString(possible.getOffset());
			if (ocurr!= null && ocurr.containsKey(posibleId)){
				veces = ocurr.get(posibleId);
			}
			if(veces>maxCount){
				maxCount=veces;
				closestSynsets.clear();
				closestSynsets.add(ancestors.get(possible));
			}else if(veces==maxCount){
				closestSynsets.add(ancestors.get(possible));
			}
		}
		return closestSynsets;
		
	}
	
	/******************************************************************/
	/**
	 * si sigue habiendo mas de uno miramos cual es que tiene los conceptos
	 * relacionados más cerca.
	 */
	private List<String> selectClosestAlternative(List<String> value, HashMap<String, Double> distan){
		double dist =10000;
		List<String> resultado = new ArrayList<String>();
		for(String val:value){
			if(distan.containsKey(val)){
				if(distan.get(val)<dist){
					resultado.clear();
					resultado.add(val);
					dist=distan.get(val);
				}else if (distan.get(val)==dist){
					resultado.add(val);
				}
			}
		}
		return resultado;
	}
	
	/**********************************************************/
	/**
	 * carga el modelo y extrae todos los synsets de todos los conceptos ya procesados
	 */
	public void beforeStep(StepExecution stepExecution) {
		Model mod = model.startOrContinueTransactionOnModel();
		
		//contamos cuantas veces cada concento de wordnet es padre de uno alineado
		obtainWordNetSynsetWeighGivenSet(mod.listSubjects().toList(),ocurrencias,distancia);
		
		//obtenemos para cada concepto el listado de todos los conceptos en la misma rama
		//y el conteo de wordnet respecto de esa rama
		
		for(RDFNode item:mod.listObjectsOfProperty(skosHasTopConcProp).toList()){
			List<Resource> conceptsInBranch = getNarrowers(item.asResource());
			
			HashMap<String, Double> ocur = new HashMap<String, Double>();
			HashMap<String, Double> dist = new HashMap<String, Double>();
			obtainWordNetSynsetWeighGivenSet(conceptsInBranch,ocur,dist);
			
			for(Resource res:conceptsInBranch){
				ocurrenciasBranch.put(res.getURI(), ocur);
				distanciaBranch.put(res.getURI(), ocur);
			}
		}
		
		
	}
	
	/***********************************************************************************
	/**
	 * dado un cocnjunto de conceptos obtiene el peso de los de wordnet y su distancia media
	 */
	private void obtainWordNetSynsetWeighGivenSet(List<Resource> concepts, HashMap<String, Double> ocurr, HashMap<String, Double> dist){
		
		//contamos cuantas veces cada concento de wordnet es padre de uno alineado
		//CAMBIADO, CREO QUE HABIA UN ERROR ANTES, SI DA PROBLEMAS REVISAR A FONDO
		//SE USABA TODO EL MODELO EN LUGAR DE LOS CONCEPTOS PROPORCIONADOS COMO PARAMETRO
		for(Resource item:concepts){	
			Statement exact = item.getProperty(exact_wordnetMatch);
			Statement isa = item.getProperty(isa_wordnetMatch);
			if((exact==null) && (isa == null)){ continue;}
			String match = exact!=null ? exact.getString(): isa.getString();
			
			List<Synset> hyps= jwnl.getHypernymsTransitive(jwnl.getSynset(Integer.valueOf(match)));
			double distance =0;
			for(Synset hyp:hyps){
				String h  = Long.toString(hyp.getOffset());
				if(!ocurr.containsKey(h)){
					ocurr.put(h, 0.0);
					dist.put(h, 0.0);
				}
				ocurr.put(h, ocurr.get(h)+1);
				dist.put(h, dist.get(h)+(distance++));
			}
		}
		//obtenemos la distancia medida de cada concepto de wordnet a los alineados
		//por debajo
		for(String key:ocurr.keySet()){
			dist.put(key, dist.get(key)/ocurr.get(key));
		}
	}
	
	
	/**
	 * obtiene los narrower transitivo de un concepto
	 */
	private List<Resource> getNarrowers(Resource concept){
		List<Resource> res = new ArrayList<Resource>();
		res.add(concept);
		for(Statement st :concept.listProperties(skosNarrowerProp).toList()){
			res.addAll(getNarrowers(st.getResource()));
		}
		return res;
	}
 

	/**
	 * No hace nada
	 */
	public ExitStatus afterStep(StepExecution stepExecution) {return null;}


	/*******************************************************************/
	/*******************************************************************/
	/**
	 * propiedades del item processor
	 */
	public void setModel(Bean_ModelContainer modelo) {
		this.model = modelo;
	}
	
	
}
