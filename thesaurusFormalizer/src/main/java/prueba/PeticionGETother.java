/**
 * 
 */
/**
 * @author dayanydc
 *
 */
package prueba;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;



public class PeticionGETother {

	public static void main(String[] args) throws Exception {
		
		try{
			 			 		
			 			
			 URL url;			 
			 URLConnection con;
			 FileWriter fw= new FileWriter("D:\\europeana\\openup.rdf");	
			 Model m = ModelFactory.createDefaultModel();
			 Model m1=null;
			 String buffer="";
			 String linea="";
			 String a="http://openup.nhm-wien.ac.at/commonNames/";
			 				    
			 for (int i = 1; i <=700999; i++) {
					
					// Creando un objeto URL
					url = new URL(a+i);
					
					// Realizando la petición GET
					con = url.openConnection();
					
					// Leyendo el resultado
					 BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));					 	 
					 buffer="";
					 System.out.println("se esta imprimiendo :" +i);
			         while ((linea = in.readLine()) != null) {			            	
			            	
			            	buffer+=linea;        	
			            				                
			         }			            
			            m1 = ModelFactory.createDefaultModel();
			            m1.read(new ByteArrayInputStream(buffer.getBytes()), null);			            
			            m.add(m1);
			            
			 } 			 
			   m.write(fw);		
		       fw.close();
		 		  
		} catch (Exception e) {
		    e.printStackTrace();
		    }	
 }
}
	



