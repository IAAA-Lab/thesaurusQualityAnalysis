package thesaurusFormalizer.extraction;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

import rdfManager.RDFPropertyManager;

/**
 * lee de un modelo de jena ya cargado con un tesaruo todas sus etiquetas
 * y las pasa para procesamiento
 */
public class ItemReader_Mem_ThesConceptLabelExtractor implements ItemReader<org.apache.jena.rdf.model.Statement>, ItemStream {
	//modelo a procesar
	private Model model;
	
	//etiquetas del modelo
	private Iterator<Statement> etiquetasIt;
	
	//propiedades RDF
	private static Property skosPrefLabelProp = RDFPropertyManager.skosPrefLabelProp;	
	private static Property skosAltLabelProp = RDFPropertyManager.skosAltLabelProp;	
		
	/***********************************************************/
	/**
	 * lee una etiqueta de un concepto del modelo
	 */
	public Statement read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if (etiquetasIt.hasNext()) {return etiquetasIt.next();}
		return null;
	}

	/************************************************************/
	/**
	 * carga las etiquetas del modelo (prefs y alts)
	 */
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		List<Statement> etiquetas = model.listStatements(null, skosPrefLabelProp, (RDFNode)null).toList();
		etiquetas.addAll(model.listStatements(null, skosAltLabelProp, (RDFNode)null).toList());
		etiquetasIt = etiquetas.iterator();
	}

	/************************************************************/
	/**
	 * no hace nada
	 */
	public void close() throws ItemStreamException {}
	public void update(ExecutionContext executionContext)throws ItemStreamException {}

	/************************************************************/
	/**
	 * propiedades del bean
	 */
	public void setModel(Model model) {this.model = model;}	
}
