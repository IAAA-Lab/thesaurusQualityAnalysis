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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;



public class PeticionGET {

	public static void main(String[] args) throws Exception {
		
		try{
			 String sDirectorio = "D:\\europeana\\httpeuropeana.txt";
			 File archivo = new File(sDirectorio);
			 FileReader fr = new FileReader(archivo);
			 BufferedReader br = new BufferedReader(fr);				
			 String li;
			 String a = null;			
			 URL url;			 
			 URLConnection con;
			 FileWriter fw= new FileWriter("D:\\europeana\\europeana.rdf");	
			 Model m = ModelFactory.createDefaultModel();
			 Model m1=null;
			 String buffer="";
			 String linea="";		
			 				    
			 while ((li = br.readLine()) != null) {
					a = li;
					// Creando un objeto URL
					url = new URL(a);
					
					// Realizando la petición GET
					con = url.openConnection();
					
					// Leyendo el resultado
					 BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));					 	 
					 buffer="";
			         while ((linea = in.readLine()) != null) {			            	
			            	
			            	buffer+=linea;        	
			            				                
			         }			            
			            m1 = ModelFactory.createDefaultModel();
			            m1.read(new ByteArrayInputStream(buffer.getBytes()), null);			            
			            m.add(m1);
			            
			 } 			 
			   m.write(fw);		
		       br.close();
		       fw.close();
		 		  
		} catch (Exception e) {
		    e.printStackTrace();
		    }	
 }
}
	



