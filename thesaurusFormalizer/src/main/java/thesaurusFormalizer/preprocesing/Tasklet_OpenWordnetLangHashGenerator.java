package thesaurusFormalizer.preprocesing;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import tools.wordnetManager.OpenMultilingualWordnetManager;

/**
 * genera una hastable con label-synsets para agilizar la identificación de alineamientos
 */
public class Tasklet_OpenWordnetLangHashGenerator implements Tasklet{

	private String sourceFile, destFile;
	
	/************************************************************************/
	/**
	 * analiza los alineamientos de un tesauro con wordnet
	 */
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		OpenMultilingualWordnetManager.generateWnetHashtable(sourceFile,destFile);
		return RepeatStatus.FINISHED;
	}
	
	/**********************************************************************/
	/**
	 * propiedades del tasklet
	 */
	public void setSourceFile(String sourceFile) {this.sourceFile = sourceFile;}
	public void setDestFile(String destFile) {this.destFile = destFile;}
	
}
