package thesaurusFormalizer.alignment.wordnetMatch;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;

import tools.wordnetManager.OpenMultilingualWordnetManager;

/**
 * extrae los possibles synsets de las keywords en el idioma indicado
 */
public class ItemSubProcessor_ConceptOpenWordNetAlignment extends ItemSubProcessor_AbstractWordNetAlignment implements ItemProcessor<ExtractedSynsetInfo, ExtractedSynsetInfo>{

	//clsase de acceso a hash de un lenguaje de openwordnet
	private OpenMultilingualWordnetManager jwnl;
	
	/**************************************************************/
	/**
	 * devuelve una lista con los synsets identificados o null si no hay
	 */
	protected List<String> getWordNetAlignments(String label){
		List<String>alignments = null;
		List<Long> possibleSyns = jwnl.getSynsets(label);
		if (possibleSyns!=null && possibleSyns.size()>0){	
			alignments= new ArrayList<String>();
			for(Long s: possibleSyns){
				alignments.add(Long.toString(s));
			}			
		}
		return alignments;
	}

	/*******************************************************************************/
	/**
	 * propiedades del tasklet
	 */
	public void setLanguageSynsetHash(String languageSynsetHash) {jwnl = new OpenMultilingualWordnetManager(languageSynsetHash);}
}
