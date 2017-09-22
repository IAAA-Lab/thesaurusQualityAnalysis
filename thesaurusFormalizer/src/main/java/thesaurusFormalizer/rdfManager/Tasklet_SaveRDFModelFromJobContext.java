package thesaurusFormalizer.rdfManager;

import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import org.apache.jena.rdf.model.Model;

import rdfManager.JenaModelManager;

/**
 * aplica tecnicas de inferencia para enriquecer la informacion de los servicios
 * con la de las capas y viceversa
 */
public class Tasklet_SaveRDFModelFromJobContext implements Tasklet{
	
	//fichero destino
	private String destCol;
	
	//modelo (bean)
	private Model model;
	
	//namespaces
	private Properties namespaces=null;
	
	//cierre del modelo
	private boolean closeModel=false;
	
	/************************************************************/
	/**
	 * guarda un modelo de jena almacenado en el contexto del proyecto
	 */
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {	
		//borramos todos los prefijos existenes
		Map<String,String> pref = model.getNsPrefixMap();
		for(String key:pref.keySet()){
			model.removeNsPrefix(key);
		}
		
		//a�adimos los namespaces adecuados
		if(namespaces!=null){
			for (Object nsId:namespaces.keySet()){
				String nsURI = namespaces.getProperty((String)nsId);
				model.setNsPrefix((String)nsId,nsURI);
			}
		}
		
		//guardamos el modelo
		JenaModelManager.saveJenaModel(model, destCol);
		if(closeModel){model.close();}
		return RepeatStatus.FINISHED;
	}	
		
	/***************************************************************/
	/**
	 * Propiedades del tasklet
	 */
	public void setDestCol(String destCol) {
		this.destCol = destCol;
	}
	public void setModel(Model model) {
		this.model = model;
	}
	public void setNamespaces(Properties namespaces){	
		this.namespaces = namespaces;
	}
	public void setCloseModel(Boolean closeModel){	
		this.closeModel = closeModel;
	}
}
