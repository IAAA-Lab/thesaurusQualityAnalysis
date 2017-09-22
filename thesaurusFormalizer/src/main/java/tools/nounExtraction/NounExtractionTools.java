package tools.nounExtraction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.creole.ANNIEConstants;
import gate.creole.SerialAnalyserController;
import gate.util.persistence.PersistenceManager;

/**
 * extrae nombres de un texto
 */
public class NounExtractionTools {

	// analizadores de gate usados
	private SerialAnalyserController annieController;
	
	/***************************************************/
	/**
	 * constructor del extractor de nombres, define los parsers usados
	 */
	public NounExtractionTools(){
		try {
			// inicializamos gate y el modulo de procesamiento de annie para extraer los nombres
			if(!Gate.isInitialised()){Gate.setGateHome(new File(".")); Gate.init();}		
			annieController = (SerialAnalyserController) PersistenceManager.loadObjectFromFile(new File(new File(
							Gate.getPluginsHome(), ANNIEConstants.PLUGIN_DIR),ANNIEConstants.DEFAULT_FILE));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	/**
	 * extrae los nombres de un texto en ingles y el número de ocurrencias
	 */
	public HashMap<String,Integer> extractNounsFromEnglishText(String text) throws Exception{
		//creamos el corpus y lo porcesamos con annie
		Corpus corpus = Factory.newCorpus("Nouns finder");
		corpus.add(Factory.newDocument(new String(text)));
		annieController.setCorpus(corpus); annieController.execute();
		
		//extraemos los nombres del documento. apuntandonos cuantas veces aparece cada uno 
		HashMap<String,Integer> nounsCount = new HashMap<String,Integer>();
		for(String noun: getListOfNouns((Document) corpus.get(0))){
			if(!nounsCount.containsKey(noun)){nounsCount.put(noun, 0);}
			nounsCount.put(noun, nounsCount.get(noun)+1);
		}	
		corpus.cleanup();corpus.clear();
		return nounsCount;
	}
	
	/********************************************************************/
	/**
	 * extrae los tokens de tipo nombre (singular y plural)
	 */
	private List<String> getListOfNouns(Document processedDocument) throws Exception {
		//obtenemos todos los token y de ellos nos quedamos con los nombres (sing and plural)
		AnnotationSet setOfTokens = processedDocument.getAnnotations().get("Token");
		List<String> listOfNouns = new ArrayList<String>();	
		for (gate.Annotation annotation : setOfTokens) {
			Object category = annotation.getFeatures().get("category");
			if (category.equals("NN") || category.equals("NNS") ) {
				listOfNouns.add(processedDocument.getContent().getContent(annotation.getStartNode().getOffset(),
						annotation.getEndNode().getOffset()).toString());
			}
		}
		return listOfNouns;
	}
	
}
