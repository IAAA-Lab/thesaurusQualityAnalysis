package thesaurusFormalizer.alignment.senseSelection;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import net.didion.jwnl.data.Synset;
import thesaurusFormalizer.rdfManager.ThesFormalizerRDFPropertyManager;
import tools.nounExtraction.NounExtractionTools;
import tools.wordnetManager.EnglishWordnetManager;

/**
 * Usa una collección de datos clasificados con los conceptos del tesauro
 * para elegir el sentido adecuado para los conceptos no desambiguados
 */
public class ItemProcessor_MdCollectionSynsetSelection implements ItemProcessor<Resource, Resource>{

	//pares de kewyords + nombres y ocurrencias en la colección
	private HashMap<String,HashMap<String,Integer>> mapMetadata;
	
	// extractor de nombres
	private NounExtractionTools net = new NounExtractionTools();
	
	//gestor de wordnet
	private EnglishWordnetManager wordnet = new EnglishWordnetManager();
	
	//propiedades rdf
	private static final Property inner_possibleLabel_Synset = ThesFormalizerRDFPropertyManager.inner_possibleLabel_Synset;	
	private static final Property exact_wordnetMatch = ThesFormalizerRDFPropertyManager.exact_wordnetMatch; 
	private static final Property isa_wordnetMatch = ThesFormalizerRDFPropertyManager.isa_wordnetMatch;
	
	int cont=0;
	
	/***************************************************************************/
	/**
	 * dado los synsets obtenidos de diferentes idiomas los integra para determinar
	 * el adecuado
	 */
	@SuppressWarnings("unchecked")
	public Resource process(Resource item) throws Exception {	
		//obtenemos los sustantivos de la colección asociados al concepto del tesauro
		//y sus posibles synsets entre los que queremos elegir
		String uri = item.getURI();
		HashMap<String,Integer> nouns = mapMetadata.get(uri);
		Statement possible = item.getProperty(inner_possibleLabel_Synset);
		
		if( possible==null){return item;}
		System.out.println(++cont);
		
		//si no hay nouns o synsets terminamos
		if(nouns==null || possible==null){return item;}
			
		
		
		//obtenemos los sustantivos en cada definición de cada synset posible del concepto
		String[] matches=possible.getString().split("\\|");
		HashMap<String,Integer>[] synNouns = (HashMap<String,Integer>[]) new HashMap[matches.length];
		for(int i=0; i< matches.length; i++){
			Synset syn =wordnet.getSynset(Long.parseLong(matches[i]));
			String synsDef = cleanWNSynsetDefinition(syn.getGloss().toLowerCase());
			synNouns[i] =net.extractNounsFromEnglishText(synsDef);
		}
			
		//analizamos la similitud entre los sustantivos sacados de la colección y los
		//sustantivos de los synsets y nos quedamos con los synsets de mayor similaridad
		List<String> suitableSynset = new ArrayList <String>();
		double maxSimilarity =0;
		for(int i=0; i< synNouns.length; i++){
			double knSynSimilarity = measureSynsetSimilarity(nouns,synNouns[i]);
			if(knSynSimilarity>maxSimilarity){
				maxSimilarity=knSynSimilarity;
				suitableSynset.clear();
				suitableSynset.add(matches[i]);
			}else if(knSynSimilarity==maxSimilarity){
				suitableSynset.add(matches[i]);
			}
		}
		
		
		//dependiendo del numero de coincidencias encontrado creamos directamente
		//el enlace o reducimos los posibles synsets para posteriores procesos
		if(suitableSynset.size()==1){
			String type = possible.getLiteral().getDatatypeURI();
			if(type.endsWith("_O")||type.endsWith("_S" )){
				item.addLiteral(exact_wordnetMatch, item.getModel().createTypedLiteral(suitableSynset.get(0),"http://www.iaaa.es/mdCollectionContext_Match"));
			}else{
				item.addLiteral(isa_wordnetMatch, item.getModel().createTypedLiteral(suitableSynset.get(0),"http://www.iaaa.es/mdCollectionContext_Match"));
			}
			item.removeAll(inner_possibleLabel_Synset);
		}else if(suitableSynset.size()>1){
			String text =suitableSynset.get(0);
			suitableSynset.remove(0);
			for(String s:suitableSynset){
				text+="|"+s;
			}
			String type = possible.getLiteral().getDatatypeURI();
			item.removeAll(inner_possibleLabel_Synset);
			int separator= type.lastIndexOf("_");
			type = type.substring(0,separator)+"_mdCollection_"+type.substring(separator);
			item.addProperty(inner_possibleLabel_Synset, item.getModel().createTypedLiteral(text,type));
		}
		
		return item;
	}
	
	/****************************************************************************/
	/****************************************************************************/
	/**
	 * borra los ejemplos de las definicionas, ya que introducen palabras que confunden
	 */
	private String cleanWNSynsetDefinition(String definition){
		String redText;
		do{
			redText = definition;
			definition = deleteTextBetweenChars (definition, "\"", "\"");
			definition = deleteTextBetweenChars (definition, "(", ")");
		}while(!definition.equals(redText));					
		return definition;
	}
	
	/****************************************************************************/
	/**
	 * borra el texto entre dos caracteres dados ej:"" or (). 
	 */
	private String deleteTextBetweenChars (String text, String car1, String car2){
		int index = text.indexOf(car1);
		int indexEnd =text.indexOf(car2,index+1);
		if(indexEnd>=0){
			text = text.substring(0,index)+ text.substring(indexEnd+1);
		}
		return text;
	}
		
	/****************************************************************************/
	/****************************************************************************/
	/**
	 * calculamos la similaridad de las dos colecciones de sustantivos 
	 * usando una variante del coseno directo
	 */
	private double measureSynsetSimilarity (HashMap<String,Integer> kwSust, HashMap<String,Integer> synsetSust){
		//calculamos la similaridad usando una variante del coseno directo
		
		//1 -obtenemos la raiz de la suma de las ocurrencias en cada colleción
		double denom = getModule(kwSust)*getModule(synsetSust);
		
		//2 - calculamos la suma de la intersección de las colecciones
		double num =0;
		for(String s: kwSust.keySet()){
			if(synsetSust.containsKey(s)){
				num+=kwSust.get(s)*synsetSust.get(s);
			}
		}	
		return denom!=0 ? num/denom : 0;
	}
	
	/**************************************************************************/
	/**
	 * calcula el modulo de una coleccion de numeros, necesario para el calculo 
	 * del coseno directo
	 */
	private double getModule(HashMap<String,Integer> col){
		double result =0;
		for(Integer i:col.values()){
			result += i*i;
		}
		return Math.sqrt(result);
		
	}
	
	/****************************************************************/
	/****************************************************************/
	/**
	 * propiedades del tasklet
	 */
	@SuppressWarnings("unchecked")
	public void setMetadataHash(String metadataHashFile){		
		try {
			ObjectInputStream entrada=new ObjectInputStream(new FileInputStream(metadataHashFile));
			mapMetadata =(HashMap<String,HashMap<String,Integer>>)entrada.readObject();
			entrada.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}					
	}
}
