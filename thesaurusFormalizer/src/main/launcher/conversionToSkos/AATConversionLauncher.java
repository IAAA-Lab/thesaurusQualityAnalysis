package conversionToSkos;

import org.springframework.batch.core.launch.support.CommandLineJobRunner;

/**
 * Carga el tesauro gemet en un tdb2
 */
public class AATConversionLauncher {
	// trabajo de spring a ejecutar
	private static final String[] jobC = {
		"conversionToSkos/AATConversionJob.xml","conversionToSkosJob" };
	public static void main(String[] args)  throws Exception {
		CommandLineJobRunner.main(jobC);
	}

}
