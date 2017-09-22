package thesaurusFormalizer.alignment.wordnetMatch;

import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import thesaurusFormalizer.rdfManager.ThesFormalizerRDFPropertyManager;

/**
 * escribe las etiquetas extraidas para cada idioma en el modelo de jena separadas por |
 */
public class ItemWriter_Mem_ThesConceptOntologyAlignment implements ItemWriter<ExtractedSynsetInfo> {
	
	//modelo a procesar
	private Model model;
	private static Property inner_conceptSynset = ThesFormalizerRDFPropertyManager.inner_conceptSynset; 
	
	/***********************************************************/
	/**
	 * escribe las etiquetas extraidas para cada idioma en el modelo de jena separadas por |
	 */
	public void write(List<? extends ExtractedSynsetInfo> items) throws Exception {
		for(ExtractedSynsetInfo item:items){
			List<String> align = item.getAlignments();
			if(align!=null){
				String result=item.getType();			
				for (String synid: align){result+="|"+synid;}				
				Resource concept = model.getResource(item.getUri());
				Literal labs = model.createLiteral(result, item.getLang());
				model.add(concept,inner_conceptSynset ,labs);
			}
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
	public void setModel(Model model) {this.model = model;}
	
}
