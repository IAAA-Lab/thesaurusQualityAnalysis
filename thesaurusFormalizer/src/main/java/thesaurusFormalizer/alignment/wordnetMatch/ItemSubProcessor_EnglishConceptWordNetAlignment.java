package thesaurusFormalizer.alignment.wordnetMatch;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;

import net.didion.jwnl.data.Synset;
import tools.wordnetManager.EnglishWordnetManager;

/**
 * extrae los possibles synsets de las keywords en ingles
 */
public class ItemSubProcessor_EnglishConceptWordNetAlignment extends ItemSubProcessor_AbstractWordNetAlignment implements ItemProcessor<ExtractedSynsetInfo, ExtractedSynsetInfo>{

	//libería de wordnet
	private static EnglishWordnetManager jwnl = new EnglishWordnetManager();
	
	/**************************************************************/
	/**
	 * devuelve una lista con los synsets identificados o null si no hay
	 */
	protected List<String> getWordNetAlignments(String label){
		List<String>alignments = null;
		List<Synset> possibleSyns = jwnl.getSynsets(label);
		if (possibleSyns.size()>0){	
			alignments= new ArrayList<String>();
			for(Synset s: possibleSyns){
				alignments.add(Long.toString(s.getOffset()));
			}			
		}
		return alignments;
	}
	
}
