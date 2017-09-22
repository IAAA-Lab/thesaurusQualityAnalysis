package thesaurusFormalizer.alignment.wordnetMatch;

import java.util.HashMap;
import java.util.Properties;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import org.apache.jena.rdf.model.Statement;

/**
 * dependiendo del idioma de una etiqueta redirecciona a las tareas de 
 * procesamiento que sean adecuadas
 */
public class ItemProcessor_ThesConceptOntologyAlignment implements ItemProcessor<Statement, ExtractedSynsetInfo>{

	@Autowired
	private ApplicationContext appContext;
	
	//procesadores especificos para cada idioma
	private HashMap<String,ItemProcessor<ExtractedSynsetInfo, ExtractedSynsetInfo>> extractores = new HashMap<String,ItemProcessor<ExtractedSynsetInfo, ExtractedSynsetInfo>>();
	
	/*******************************************************************************/
	/**
	 * dependiendo del idioma de una etiqueta redirecciona a las tareas de 
	 * procesamiento que sean adecuadas
	 */
	
	public ExtractedSynsetInfo process(Statement item) throws Exception {		
		if(extractores.containsKey(item.getLanguage())){
			return extractores.get(item.getLanguage()).process(new ExtractedSynsetInfo(item.getSubject().getURI(),item.getString(), item.getLanguage()));
		}	
		return null;
	}

	/***********************************************************/
	/**
	 * propiedades del bean
	 */
	@SuppressWarnings("unchecked")
	public void setLangDependentExtractors(Properties langDependentExtractors) {
		for(Object lang:langDependentExtractors.keySet()){
			String processorBean = langDependentExtractors.getProperty((String)lang);
			extractores.put((String)lang, (ItemProcessor<ExtractedSynsetInfo, ExtractedSynsetInfo>)appContext.getBean(processorBean));
		}
	}
}
