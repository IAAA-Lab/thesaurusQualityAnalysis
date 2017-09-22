package thesaurusFormalizer;

import org.springframework.batch.core.launch.support.CommandLineJobRunner;

/**
 * Lanzador del programa de extracción de nombres de etiquetas de conceptos
 */
public class SBA_EuroGEOSSFormalizationLauncher {
	// trabajo de spring a ejecutar
	private static final String[] jobC = {
			"thesaurusFormalizer/formalizationJobs/SBA_EuroGEOSSFormalizationJob.xml",
			"SBA_EuroGEOSSFormalizationJob" };

	/*****************************************************************/
	/**
	 * Lanzador del programa de extracción de nombres de etiquetas de conceptos
	 */
	public static void main(String[] args) throws Exception {
		CommandLineJobRunner.main(jobC);
	}

}
