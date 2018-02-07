package thesaurusFormalizer.rdfManager;

import java.io.File;
import java.io.FileInputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import rdfManager.JenaModelManager;
import rdfManager.RDFPropertyManager;
import rdfProcessing.Bean_ModelContainer;

/**
 * dado un tesauro con Bt genera las nt y viceversa
 * @author jlacasta
 *
 */
public class Tasklet_GenerateInverseNTBT implements Tasklet{
	private Bean_ModelContainer model=null;
	
	/***************************************************************/
	/**
	 * Lee un rdf y genera un tdb con el contenido de su modelo por defecto 
	 */
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		Model modelo = model.startOrContinueTransactionOnModel();
		
		StmtIterator it= modelo.listStatements(null,RDFPropertyManager.skosBroaderProp,(RDFNode)null);
		while(it.hasNext()) {
			Statement i = it.next();
			modelo.add(i.getResource(), RDFPropertyManager.skosNarrowerProp, i.getSubject());
		}
		
		it= modelo.listStatements(null,RDFPropertyManager.skosNarrowerProp,(RDFNode)null);
		while(it.hasNext()) {
			Statement i = it.next();
			modelo.add(i.getResource(), RDFPropertyManager.skosBroaderProp, i.getSubject());
		}
		
		model.finishTransactionOnModel();
		return RepeatStatus.FINISHED;
	}

	/***************************************************************/
	/**
	 * propiedades del bean
	 */
	public void setModel(Bean_ModelContainer model) {
		this.model = model;
	}
	
	
	
	
}
