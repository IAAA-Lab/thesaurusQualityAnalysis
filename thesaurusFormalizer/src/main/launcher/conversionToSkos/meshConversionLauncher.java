package conversionToSkos;

import org.springframework.batch.core.launch.support.CommandLineJobRunner;

/**
 * Carga el tesauro mesh en un tdb2
 */
public class meshConversionLauncher {
	// trabajo de spring a ejecutar
	private static final String[] jobC = {
		"conversionToSkos/meshConversionJob.xml","conversionToSkosJob" };
	public static void main(String[] args)  throws Exception {
		CommandLineJobRunner.main(jobC);
	}

}
