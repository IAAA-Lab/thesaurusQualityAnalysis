/**
 * 
 */
/**
 * @author dayanydc
 *
 */
package prueba;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;

import org.jsfr.json.JsonPathListener;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferGson;
import org.jsfr.json.ParsingContext;
import org.junit.Test;





public class DplaTest {

	static String JSON_FILE = "D:\\dpla\\nypl.json";
	static String OUTPUT_FILE= "D:\\dpla\\auth.txt";
//	static String JSON_Exp = "$..originalRecord..subject..authority";
	static String JSON_Exp = "$..sourceResource..subject";
	
/*	@Test
	public void jsonPathStreamingTest() throws Exception {
		File archivo = new File(JSON_FILE);	
		FileReader fis = new FileReader(archivo);
		
		JsonSurfer jsonSurfer = JsonSurferGson.INSTANCE;
        Collection<Object> authority = jsonSurfer.collectAll(fis, JSON_Exp);
        System.out.println(authority.size());
        
        FileOutputStream var = new FileOutputStream(OUTPUT_FILE);
        PrintStream var2 = new PrintStream(var);
        for(Object auth:authority){        	
        	var2.println(auth);        	
        }        
       var2.close();
	}  */ 
	
	@Test
	public void jsonPathStreamingTest() throws Exception {
		File archivo = new File(JSON_FILE);	
		FileReader fis = new FileReader(archivo);
		FileOutputStream var = new FileOutputStream(OUTPUT_FILE);
        final PrintStream var2 = new PrintStream(var);
		
		JsonSurfer jsonSurfer = JsonSurferGson.INSTANCE;
				
        jsonSurfer.configBuilder()
                .bind(JSON_Exp, new JsonPathListener() {
                    public void onValue(Object value, ParsingContext context) {
                                          	
                    //	System.out.println(value);
                      var2.println(value);
                    }
                })
                .buildAndSurf(fis);
        var2.close();
	}
	
}
