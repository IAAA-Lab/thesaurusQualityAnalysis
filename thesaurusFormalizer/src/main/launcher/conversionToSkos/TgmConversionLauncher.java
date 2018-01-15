package conversionToSkos;

import org.springframework.batch.core.launch.support.CommandLineJobRunner;

/**
 * Carga el tesauro tgm en un tdb2
 */
public class TgmConversionLauncher {
	// trabajo de spring a ejecutar
	private static final String[] jobC = {
		"conversionToSkos/tgmConversionJob.xml","conversionToSkosJob" };
	public static void main(String[] args)  throws Exception {
		CommandLineJobRunner.main(jobC);
	}

}
