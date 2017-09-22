package thesaurusFormalizer.rdfManager;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import org.apache.jena.rdf.model.Model;

import rdfManager.JenaModelManager;
/**
 * carga un modelo de rdf y lo guarda en el contexto con el nombre indicado
 */
public class Tasklet_LoadRDFModelIntoJobContext implements Tasklet{
	
	//fichero fuente
	private String sourceCol;
	
	//modelo (bean)
	private Model model;
	
	/************************************************************/
	/**
	 * aplica tecnicas de inferencia para enriquecer la informacion de los servicios
	 * con la de las capas y viceversa
	 */
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		model = JenaModelManager.loadJenaModel(model,sourceCol,null);
		return RepeatStatus.FINISHED;
	}	
	
	/***************************************************************/
	/**
	 * Propiedades del tasklet
	 */
	public void setSourceCol(String sourceCol) {this.sourceCol = sourceCol;}
	public void setModel(Model model) {this.model = model;}
}