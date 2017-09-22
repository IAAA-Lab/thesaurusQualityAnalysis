package thesaurusFormalizer.preprocesing;

import java.io.File;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

/**
 * Detecta la existencia de un fichero de resultados para determinar si 
 * el flujo normal de los pasos puede continuar o no
 * Se usa para detectar si el preprocesamiento de los datos se ha hecho y si no hacerlo automaticamente
 */
public class Decider_FileExistenceDetection  implements JobExecutionDecider {
	//fichero a detectar su existencia
	File fichero;
	
	public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        if (fichero==null || !fichero.exists()) {
            return FlowExecutionStatus.FAILED;
        }else {
        	return FlowExecutionStatus.COMPLETED;
        }
    }

	/**********************************************************************/
	/**
	 * propiedades del bean
	 */
	public void setFichero(String fichero) {
		this.fichero = new File(fichero);
	}
}
