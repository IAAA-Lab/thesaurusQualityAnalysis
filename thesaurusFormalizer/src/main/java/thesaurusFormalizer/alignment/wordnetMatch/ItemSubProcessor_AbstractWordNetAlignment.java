package thesaurusFormalizer.alignment.wordnetMatch;

import java.util.List;

import org.springframework.batch.item.ItemProcessor;

/**
 * extrae los possibles synsets de las keywords en ingles
 */
public abstract class ItemSubProcessor_AbstractWordNetAlignment implements ItemProcessor<ExtractedSynsetInfo, ExtractedSynsetInfo>{

	/*******************************************************************************/
	/**
	 * busca alineamiento de los conceptos en wordent en base a comparación de sus etiquetas
	 */
	public ExtractedSynsetInfo process(ExtractedSynsetInfo item) throws Exception {		
		String[] labels = item.getLabels().split("\\|");
		
		//buscamos synsets de mas calidad a menos calidad
		//en cuanto encontramos uno- es el que devolvemos
		for(String label: labels){
			try{
				String[]comp = label.split("\"");
				item.setType(comp[0]);
				List<String>alignments= getWordNetAlignments(comp[1]);
				if (alignments!=null){item.setAlignments(alignments);break;}
			}catch(Exception er){
				//esto ocurre cuando hay algún caso no tradao en la etapa de extracción de etiquetas
				System.err.println(item.getLabels());
			}
		}	
		return item;
	}
	
	/**************************************************************/
	/**
	 * devuelve una lista con los synsets identificados o null si no hay
	 */
	protected abstract List<String> getWordNetAlignments(String label);
	
}
