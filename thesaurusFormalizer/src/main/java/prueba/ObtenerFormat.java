/**
 * 
 */
/**
 * @author dayanydc
 *
 */
package prueba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class ObtenerFormat {

public static void main(String[] args) throws Exception {
	 
try {
		
	   //leer fichero
	   String sDirectorio = "D:\\prueba\\european\\";
	   File archivo = new File (sDirectorio);
	   File[] ficheros = archivo.listFiles();
	   FileWriter fw= new FileWriter("d:\\prueba\\europeanFormat.txt");
	   PrintWriter pw = new PrintWriter(fw);
	   String comp="RDF";   
	   
	   for (int j=0;j<ficheros.length;j++){		
	   
	   FileReader fr = new FileReader (sDirectorio + ficheros[j].getName());
	   BufferedReader br = new BufferedReader(fr);
	   String output;	
	   String var= null;
	   
	   while ((output = br.readLine()) != null) {			
			 var= output;				
		}
	    fr.close();
		//Decoding JSON		
		JSONParser parser = new JSONParser();		
		JSONObject obj = (JSONObject)parser.parse(var);		
		JSONObject a = (JSONObject) obj.get("result");	   	
		JSONArray res = (JSONArray) (a.get("resources"));					
				
		for (int i = 0; i < res.size(); ++i) {
		    JSONObject rec = (JSONObject) res.get(i);		    
		    String formato = (String) rec.get("format");
		    
		    if(formato.equalsIgnoreCase(comp)){
		     pw.println(ficheros[j].getName());
		   }  
		}
				
	   }	
	     
	     fw.close();
		 Thread.sleep(1000);	   
			  		
		 
	} catch (ClientProtocolException e) {
		e.printStackTrace();

	} catch (IOException e) {
		e.printStackTrace();
	}
}

}