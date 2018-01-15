package conversionToSkos;

import org.springframework.batch.core.launch.support.CommandLineJobRunner;

/**
 * Carga el tesauro AuthoritiesSubjects en un tdb2
 */
public class AuthoritiesSubjectsConversionLauncher {
	// trabajo de spring a ejecutar
	private static final String[] jobC = {
		"conversionToSkos/authoritiesSubjectsConversionJob.xml","conversionToSkosJob" };
	public static void main(String[] args)  throws Exception {
		CommandLineJobRunner.main(jobC);
	}

}
