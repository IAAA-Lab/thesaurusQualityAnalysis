package thesaurusFormalizer.extraction;

import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;

import rdfProcessing.Bean_ModelContainer;
import thesaurusFormalizer.rdfManager.ThesFormalizerRDFPropertyManager;

/**
 * escribe las etiquetas extraidas para cada idioma en el modelo de jena separadas por |
 */
public class ItemWriter_Mem_ThesConceptLabelExtractor implements ItemWriter<ExtractedLabelInfo> {
	
	//modelo a procesar
	private Bean_ModelContainer model;
	private static Property inner_conceptNoun = ThesFormalizerRDFPropertyManager.inner_conceptNoun; 
	
	/***********************************************************/
	/**
	 * escribe las etiquetas extraidas para cada idioma en el modelo de jena separadas por |
	 */
	public void write(List<? extends ExtractedLabelInfo> items) throws Exception {
		Model mod = model.startOrContinueTransactionOnModel();
		for(ExtractedLabelInfo item:items){
			if(item.getLabelVariants()==null){continue;}
			String result=""; 
			for (String lab: item.getLabelVariants()){
				result+=lab+"|";
			}
			result= result.substring(0, result.length()-1);
			Resource concept = mod.getResource(item.getUri());
			Literal labs = mod.createLiteral(result, item.getLang());
			mod.add(concept,inner_conceptNoun ,labs);
		}
	}	
	
	
	/************************************************************/
	/**
	 * metodos no necesarios
	 */
	public void open(ExecutionContext executionContext) throws ItemStreamException {}
	public void update(ExecutionContext executionContext)throws ItemStreamException {}
	public void close() throws ItemStreamException {}
	
	/************************************************************/
	/**
	 * propiedades del bean
	 */
	public void setModel(Bean_ModelContainer model) {this.model = model;}
	
}
