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
public class ItemSubProcessor_FrenchLabelExtractor extends ItemSubProcessor_AbstractLabelExtractor{
	
	//tagger a usar
	private String frenchPosModel ="data/input/opennlp/opennlp-fr-pos-nicolas-hernandez.bin";
	
	//tag de las preposiciones
	private String frenchPosPreposName ="P";
	private String frenchPosPreposNameB ="P+D";
	
	
	/*******************************************************************************/
	/**
	 * carga los parsers necesarios para la extracción
	 */
	
	public ItemSubProcessor_FrenchLabelExtractor() throws Exception{
		tagger = new POSTaggerME(new POSModel(new FileInputStream(frenchPosModel)));
	}
	
	/**************************************************************/
	/**
	 * devolvemos la posicion de la proposicion o -1 si no esta
	 */
	protected boolean isPreposition(String POStokens, String label){
		if(POStokens.equals(frenchPosPreposName)|| 
				POStokens.equals(frenchPosPreposNameB)||
				label.equals("d")){return true;}	
		return false;
	}
	
	/**************************************************************/
	/**
	 * selecciona el adjetivo menos significativo
	 */
	protected int posAdjective(List<String> POStokensAsL){
		if(POStokensAsL.size()==1){return -1;}
		if(!POStokensAsL.get(0).startsWith("N")){return 0;}
		return POStokensAsL.size()-1;
	}
	
	/***************************************************************/
	/**
	 * elimina el plural del texto indicado
	 */
	protected String stemPlural(String texto){
		return StemmingTools.stemFrenchPlurals(texto);
	}
	
}
