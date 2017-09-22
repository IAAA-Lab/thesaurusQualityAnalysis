package thesaurusFormalizer.extraction;

import java.io.FileInputStream;
import java.util.List;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import tools.stemming.StemmingTools;

/**
 * copia la etiqueta completa al resultado
 * necesario si se queire intentar direct matching
 */
public class ItemSubProcessor_EnglishLabelExtractor extends ItemSubProcessor_AbstractLabelExtractor{
	
	//base de conocimento del tagger
	private String englishPosModel ="data/input/opennlp/opennlp-en-pos-maxent.bin";
	
	//part of speech tag used for prepositions in english
	private String englishPosPreposName ="IN";
	
	
	/*******************************************************************************/
	/**
	 * carga los parsers necesarios para la extracción
	 */
	
	public ItemSubProcessor_EnglishLabelExtractor() throws Exception{
		tagger = new POSTaggerME(new POSModel(new FileInputStream(englishPosModel)));
	}
	
	/**************************************************************/
	/**
	 * selecciona el adjetivo menos significativo
	 */
	protected int posAdjective(List<String> POStokensAsL){
		return POStokensAsL.size()==1 ? -1 : 0;
	}
	
	/**************************************************************/
	/**
	 * devolvemos la posicion de la proposicion o -1 si no esta
	 * inside y outside no son preposiciones aunque lo diga la libreria
	 */
	protected boolean isPreposition(String POStokens, String label){
		if(label.equals("inside")||label.equals("outside")){return false;}
		if(POStokens.equals(englishPosPreposName)){return true;}	
		return false;
	}
	
	/***************************************************************/
	/**
	 * elimina el plural del texto indicado
	 */
	protected String stemPlural(String texto){
		return StemmingTools.stemEnglishPlurals(texto);
	}
	
}
