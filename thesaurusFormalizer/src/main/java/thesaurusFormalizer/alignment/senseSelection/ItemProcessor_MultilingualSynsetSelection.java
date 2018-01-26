package thesaurusFormalizer.alignment.senseSelection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.springframework.batch.item.ItemProcessor;

import net.didion.jwnl.data.Synset;
import thesaurusFormalizer.rdfManager.ThesFormalizerRDFPropertyManager;
import tools.wordnetManager.EnglishWordnetManager;

/**
 * dado los synsets obtenidos de diferentes idiomas los integra para determinar
 * el adecuado
 */
public class ItemProcessor_MultilingualSynsetSelection implements ItemProcessor<Resource, Resource>{

	//propiedades de rdf
	private static final Property inner_conceptSynset = ThesFormalizerRDFPropertyManager.inner_conceptSynset; 
	private static final Property exact_wordnetMatch = ThesFormalizerRDFPropertyManager.exact_wordnetMatch; 
	private static final Property isa_wordnetMatch = ThesFormalizerRDFPropertyManager.isa_wordnetMatch;
	private static final Property inner_possibleLabel_Synset = ThesFormalizerRDFPropertyManager.inner_possibleLabel_Synset;
	private static final Property unalignable_wordnet = ThesFormalizerRDFPropertyManager.unalignable_wordnet;
	
	//modelo acceso a wordnet
	private EnglishWordnetManager jwnl = new EnglishWordnetManager();
	
	//tipos
	private static enum LabelType{O,P,N,C,E};
	
	/***************************************************************************/
	/**
	 * dado los synsets obtenidos de diferentes idiomas los integra para determinar
	 * el adecuado
	 */
	public Resource process(Resource item) throws Exception {
		
		//obtenemos todos los synsets y el mejor tipo de calidad los disponibles en los synsets
		//saco los synsets para quedarme con las de mas calidad de entre todos los idiomas
		HashMap<String,List<List<String>>> allSynsets = getAllSynsets(item);
		LabelType bestType = getBestQualityType(allSynsets);
		HashMap<String,List<String>> posibleSynsPerLang = getBestSynsetsPerLanguage(allSynsets, bestType);
		
		
		//obtengo la interseccion direta, o en 1 solo idioma o el synset aparece dos o mas veces
		List <String> intersect = getDirectSynsetsIntersection(posibleSynsPerLang,"");
		
		//si la intersección está vacia busco antecesores comunes
		if(intersect.size()==0){
			int hierchyLevel=-1;
			while (intersect.size()==0 && ++hierchyLevel<4){
				intersect = getHierarchicalSynsetsIntersection(new ArrayList<List<String>>(posibleSynsPerLang.values()),"H"+hierchyLevel+"_",hierchyLevel);
			}
		}
		
		//si solo hay 1 synset lo guardamos con su relacion de acuerdo al tipo
		//la primera componente de intersect guarda el grado de interseccion
		if(intersect.size()==2){		
			String syn = intersect.get(1);	
			if(bestType==LabelType.O){
				item.addLiteral(exact_wordnetMatch, item.getModel().createTypedLiteral(syn,"http://www.iaaa.es/labMatch_InSec_"+intersect.get(0)));
			}else{
				item.addLiteral(isa_wordnetMatch, item.getModel().createTypedLiteral(syn,"http://www.iaaa.es/labMatch_InSec_"+intersect.get(0)));
			}
			return item;
		}
		
		//si la intersección es multiple tratamos de filtrar los synsets con otros
		//de menos calidad
		if(intersect.size()>2){
			intersect = filterSynsetsUsingLowerQualityOnes(intersect,allSynsets,bestType);
		}
		
		//si la intersección es multiple tratamos de filtrar los synsets con otros
		//de menos calidad(por cercania)
		if(intersect.size()>2){
			intersect = filterSynsetsUsingLowerQualityOnesProximity(intersect,allSynsets,bestType,3);
		}
		
		//si la intersección es multiple, nos la guardamos para los siguientes pasos
		if(intersect.size()>2){
			String deep = intersect.get(0); intersect.remove(0);
			String syns = ""; for(String ints:intersect){syns=syns+ints+"|";}
			item.addLiteral(inner_possibleLabel_Synset, item.getModel().createTypedLiteral(syns.substring(0,syns.length()-1),"http://www.iaaa.es/labMatch_InSec_"+deep+"_"+bestType.toString()));
			return item;
		}
		
		//si aun asi no hay intersección marcamos el concepto como no alineable por conflicto de interseccion
		//o no alineado si no hay synsets originales
		if(posibleSynsPerLang.values().size()==0){
			item.addLiteral(unalignable_wordnet, "No synsets found in any language");
		}else{
			//si la interseccion es vacia pero hay elementos generamos la union
			String union="";
			for(List<String> syns:posibleSynsPerLang.values()){
				for(String syn: syns.subList(1, syns.size())){
					union+=syn+"|";
				}
			}
			union = union.substring(0,union.length()-1);
			item.addLiteral(inner_possibleLabel_Synset, item.getModel().createTypedLiteral(union,"http://www.iaaa.es/labMatch_Conflict"));		
			item.addLiteral(unalignable_wordnet, "Empty language intersection - Meaning conflict");
		}
			
		return item;
		
	}
	
	/****************************************************************************/
	/**busca si de entre los mejores synsets puede seleccionar alguno filtrando en base a otros
	 * de menor calidad
	 */
	private List<String> filterSynsetsUsingLowerQualityOnes(List<String> goodSyns, HashMap<String,List<List<String>>> allSyns, LabelType bestType){
		Set<String> result = new HashSet<String>();
		for(List<List<String>> synsetsPerLang: allSyns.values()){
			for (List<String> synsetsPerLabel: synsetsPerLang){
				if (LabelType.valueOf(synsetsPerLabel.get(0))!=bestType){
					for(String syn:goodSyns){
						if(synsetsPerLabel.contains(syn)){
							result.add(syn);
						}
					}
				}
			}
		}
		if(result.size()>0){
			return new ArrayList<String>(result);
		}
		return goodSyns;
	}
	
	/*****************************************************************************/
	/**
	 * busca si de entre los mejores synses puede seleccionar alguno filtrando en base
	 * a cercania con otros de menor calidad
	 */
	private List<String> filterSynsetsUsingLowerQualityOnesProximity(List<String> goodSyns, HashMap<String,List<List<String>>> allSyns, LabelType bestType, int level){
		//obtenemos todos los synsets de menor calidad que los elejidos
		Set<String> syns = new HashSet<String>();
		for(List<List<String>> synsetsPerLang: allSyns.values()){
			for (List<String> synsetsPerLabel: synsetsPerLang){
				syns.addAll(synsetsPerLabel.subList(1, synsetsPerLabel.size()));
			}
		}
		syns.removeAll(goodSyns);
		
		//obtenemos los hipernios de cada synset de alta probabilidad
		//si coinciden con alguno de baja lo añadimos
		Set<String> result = new HashSet<String>();
		for(String s:goodSyns.subList(1, goodSyns.size())){
			List<Synset> transit = jwnl.getHypernymsTransitive(jwnl.getSynset(Long.valueOf(s)));
			for(int i=0;i<level;i++){
				if(i<transit.size() && syns.contains(Long.toString(transit.get(i).getOffset()))){
					result.add(s);break;
				}
			}
		}
		
		//devuelve el subconjuto seleccionados de synsets o todos si no puede filtrar
		if(result.size()>0){return new ArrayList<String>(result);}
		return goodSyns;
	}
	
	/**********************************************************************/
	/**
	 * devuelve todos los synsets del recruso por idioma
	 */
	private HashMap<String,List<List<String>>> getAllSynsets(Resource item){
		HashMap<String,List<List<String>>> allSynsets = new HashMap<String,List<List<String>>>();
		List<Statement> stL = item.listProperties(inner_conceptSynset).toList();
		for(Statement stm:stL){
			String lang = stm.getLanguage();
			List<String> posibleSyns = new ArrayList<String>(Arrays.asList(stm.getString().split("\\|")));
			if(!allSynsets.containsKey(lang)){allSynsets.put(lang, new ArrayList<List<String>>());}
			allSynsets.get(lang).add(posibleSyns);
		}
		return allSynsets;
	}
	
	/*****************************************************************/
	/**
	 * obtengo el mejor tipo de calidad de entre los disponibles en los synsets
	 */
	private LabelType getBestQualityType(HashMap<String,List<List<String>>> allSynsets){
		LabelType bestCode = LabelType.E;
		for(List<List<String>> synsetsPerLang: allSynsets.values()){
			for (List<String> synsetsPerLabel: synsetsPerLang){
				LabelType code = LabelType.valueOf(synsetsPerLabel.get(0));
				if(code.compareTo(bestCode)<0){
					bestCode=code;
				}
			}
		}
		return bestCode;
	}
	
	
	/*********************************************************************/
	/**
	 * saco los synsets para quedarme con las de mas calidad de entre todos los idiomas (el tipo indicado)
	 */
	private HashMap<String, List<String>> getBestSynsetsPerLanguage(HashMap<String,List<List<String>>> allSynsets, LabelType bestCode){	
		HashMap<String,List<String>> bestAlignSynsetPerLang = new HashMap<String,List<String>>();
		for(String lang: allSynsets.keySet()){
			List<List<String>> synsetsPerLang = allSynsets.get(lang);
			for (List<String> synsetsPerLabel: synsetsPerLang){
				if(LabelType.valueOf(synsetsPerLabel.get(0))==bestCode){
					List<String> synsToAdd = new ArrayList<String>(synsetsPerLabel);
					if(!bestAlignSynsetPerLang.containsKey(lang)){
						bestAlignSynsetPerLang.put(lang,synsToAdd);
					}else{
						synsToAdd.remove(0);
						bestAlignSynsetPerLang.get(lang).addAll(synsToAdd);
					}
				}
			}
		}
		
		return bestAlignSynsetPerLang;
	}
	
	/***************************************************************************/
	/**
	 * obtiene la intersección directade los synsets.
	 * la interseccion es directa, o tiene en un solo idioma o solo devuelve
	 * aquellos synsets que aparecen dos o mas veces
	 */
	private List<String> getDirectSynsetsIntersection(HashMap<String,List<String>> synsets, String base){
		//si solo hay sentidos en un idioma nos los quedamos
		//si hay en varios idiomas calculamos la intersección
		List <String> intersect = new ArrayList<String>();
		if(synsets.values().size()==1){
			intersect = new ArrayList<String>(synsets.values().iterator().next());
			intersect.remove(0);
			intersect.add(0,base+"1");
		}else{
			intersect = getBestDirectIntersection(new ArrayList<List<String>>(synsets.values()),base);
			
		}
		return intersect;
	}
	
	/**
	 * obtiene la mejor intersección directa possible no vacia de las listas dadas
	 * si no hay interseccion entre todas lo repite con un ivel menos sucesivametne hasta 2
	 */
	private List<String> getBestDirectIntersection(List<List<String>> synsets, String base){
		//calculamos la intersección completa con todos los idiomas
		List<String> intersect = getIntersection (synsets);
		if(intersect.size()>0){
			intersect.add(0, base+Integer.toString(synsets.size()));
			return intersect;
		}
		
		//si la interseccion completa es vacía hacemos todas las intersecciones
		//eliminado uno de los elementos, si no dos, y asi hasta interseccioens de pares
		//nos quedamos con la union de las intersecciones del mismo nivel que encontremos
		if(synsets.size()>2){
			int maxIntrSize=0;
			Set<String> posintr = new HashSet<String>();
			//generamos todas las combinaciones de synsets -1
			for(int i=0;i<synsets.size();i++){
				List<List<String>> reduced = new ArrayList<List<String>>(synsets);
				reduced.remove(i);
				List<String> foundInt = getBestDirectIntersection(reduced,base);
				if(foundInt.size()>0){
					int localIntrSize;
					if(maxIntrSize<(localIntrSize=Integer.valueOf(foundInt.get(0)))){
						maxIntrSize =localIntrSize;
						posintr.clear();
						posintr.addAll(foundInt.subList(1, foundInt.size()));
					}else if(maxIntrSize==localIntrSize){
						posintr.addAll(foundInt.subList(1, foundInt.size()));
					}
				}
			}
			if(posintr.size()>0){
				intersect.addAll(posintr);
				intersect.add(0,base+Integer.toString(maxIntrSize));
			}			
		}
		return intersect;
	}
	
	/**
	 * obtenemos la intersección entre n listas de sunsets
	 */
	private List<String> getIntersection(List<List<String>> synsets){
		//calculamos la intersección de los synsets,
		Set<String> intersect = new HashSet<String>();
		for (List<String> syns: synsets){
			if(intersect.size()==0){
				intersect.addAll(syns.subList(1, syns.size()));
			}else{
				intersect.retainAll(syns.subList(1, syns.size()));
			}
		}
		return new ArrayList<String>(intersect);
	}
	
	/*********************************************************************/
	/**
	 * realiza la intersección entre listas de synsets pero en lugar de intersección
	 * normal tiene en cuenta la rama de wordnet a la que pertenece cada synset
	 */
	private List<String> getHierarchicalSynsetsIntersection(List<List<String>> synsets, String base, int level){
		//calculamos la intersección completa con todos los idiomas
		List<String> intersect = getHierarchicalIntersection (synsets,level);
		if(intersect.size()>0){
			intersect.add(0, base+Integer.toString(synsets.size()));
			return intersect;
		}
		
		//si la interseccion completa es vacía hacemos todas las intersecciones
		//eliminado uno de los elementos, si no dos, y asi hasta interseccioens de pares
		//nos quedamos con la union de las intersecciones del mismo nivel que encontremos
		if(synsets.size()>2){
			int maxIntrSize=0;
			Set<String> posintr = new HashSet<String>();
			//generamos todas las combinaciones de synsets -1
			for(int i=0;i<synsets.size();i++){
				List<List<String>> reduced = new ArrayList<List<String>>(synsets);
				reduced.remove(i);
				List<String> foundInt = getHierarchicalSynsetsIntersection(reduced,base,level);
				if(foundInt.size()>0){
					int localIntrSize;
					if(maxIntrSize<(localIntrSize=Integer.valueOf(foundInt.get(0)))){
						maxIntrSize =localIntrSize;
						posintr.clear();
						posintr.addAll(foundInt.subList(1, foundInt.size()));
					}else if(maxIntrSize==localIntrSize){
						posintr.addAll(foundInt.subList(1, foundInt.size()));
					}
				}
			}
			if(posintr.size()>0){
				intersect.addAll(posintr);
				intersect.add(0,base+Integer.toString(maxIntrSize));
			}			
		}
		return intersect;
	}
	
	/**
	 * obtenemos la intersección jerarquica entre n listas de synsets
	 */
	private List<String> getHierarchicalIntersection(List<List<String>> synsets, int level){
		//para cada synset generamos toda su jerarquía
		List<List<List<String>>> synsetshier = new ArrayList<List<List<String>>>();
		for(List<String> langSyn : synsets){		
			List<List<String>> upperSynsCol = new ArrayList<List<String>>();
			for(String syn: langSyn.subList(1, langSyn.size())){
				Synset s= jwnl.getSynset(Integer.parseInt(syn));
				List<Synset> hypern = jwnl.getHypernymsTransitive(s);
				
				List<String> uppSyns= new ArrayList<String>();
				uppSyns.add(syn);
				for(Synset hy:hypern){
					uppSyns.add(Long.toString(hy.getOffset()));
				}
				upperSynsCol.add(uppSyns);
			}
			synsetshier.add(upperSynsCol);
		}
		
		//si en vez de querer buscar la intersección queremos buscar
		//antepasados comunes de cierto nivel, tenemos que borrar de cada jerarquía ese
		//numero de conceptos
		for(List<List<String>> langSyn : synsetshier){
			for(List<String> syn: langSyn){
				for(int i=0;i<level && syn.size()>0;i++){
					syn.remove(0);
				}
			}
		}
		
		//calculamos la intersección de los synsets,
		//cogemos cada synset y lo buscamos en la jerarquía de los demas idiomas
		//si está en todas => intersecta
		Set<String> inters = new HashSet<String>();
		for(int i=0; i<synsetshier.size();i++){
			//obtenemos los synsets de un idioma
			List<String> lanSyns =  new ArrayList<String>();
			for (List<String>synHier:synsetshier.get(i)){
				lanSyns.add(synHier.get(0));
			}
			//miramos si ocurren en todas las demas jerarquías
			for(int k=0; k<synsetshier.size();k++){
				if(i!=k){
					Set<String> intersect = new HashSet<String>();
					List<List<String>> synlang2 = synsetshier.get(k);
					for (List<String>tointersect:synlang2){
						List<String> partialInters = new ArrayList<String>(lanSyns);
						partialInters.retainAll(tointersect);
						intersect.addAll(partialInters);
					}
					lanSyns=new ArrayList<String>(intersect);
				}
			}
			inters.addAll(lanSyns);										
		}			
		return new ArrayList<String>(inters);
	}	
}
