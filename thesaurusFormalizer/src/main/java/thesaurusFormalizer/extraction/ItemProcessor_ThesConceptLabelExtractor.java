package thesaurusFormalizer.extraction;

import java.util.HashMap;
import java.util.Properties;

import org.apache.jena.rdf.model.Statement;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * dependiendo del idioma de una etiqueta redirecciona a las tareas de 
 * procesamiento que sean adecuadas
 */
public class ItemProcessor_ThesConceptLabelExtractor implements ItemProcessor<Statement, ExtractedLabelInfo>{

	@Autowired
	private ApplicationContext appContext;
	
	//procesadores especificos para cada idioma
	private HashMap<String,ItemProcessor<ExtractedLabelInfo, ExtractedLabelInfo>> extractores = new HashMap<String,ItemProcessor<ExtractedLabelInfo, ExtractedLabelInfo>>();
	
	/*******************************************************************************/
	/**
	 * dependiendo del idioma de una etiqueta redirecciona a las tareas de 
	 * procesamiento que sean adecuadas
	 */
	public ExtractedLabelInfo process(Statement item) throws Exception {	
		if(extractores.containsKey(item.getLanguage())){	
			return extractores.get(item.getLanguage()).process(new ExtractedLabelInfo(item.getSubject().getURI(),item.getString(), item.getLanguage()));
		}	
		return null;
	}

	/***********************************************************/
	/**
	 * propiedades del bean
	 */
	@SuppressWarnings("unchecked")
	public void setLangDependentExtractors(Properties langDependentExtractors) {
		for(Object lang:langDependentExtractors. keySet()){
			String processorBean = langDependentExtractors.getProperty((String)lang);		
			extractores.put((String)lang,(ItemProcessor<ExtractedLabelInfo, ExtractedLabelInfo>)appContext.getBean(processorBean));
		}
	}
}
