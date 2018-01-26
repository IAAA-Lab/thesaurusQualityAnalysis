/**
 * 
 */
/**
 * @author dayanydc
 *
 */
package prueba;

import java.io.FileWriter;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class LeerFicheroXML {

	public static void main(String[] args) throws Exception {
		
		try{
		
		// Se crea la instancia del builder necesario para leer el fichero xml		
		  	
		  DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();	      
	      DocumentBuilder db= dbf.newDocumentBuilder();
	    
	    // Se lee el fichero xml
	      String XML_FILE = "C:\\Users\\dayanydc\\Desktop\\europeana\\oai-pmh.xml";	
	      Document doc = db.parse(XML_FILE);
	      FileWriter fw= new FileWriter("D:\\europeana\\httpeuropeana.txt");
	      PrintWriter pw = new PrintWriter(fw);
	    // Para mostrar nodo raiz
	      
	   /*doc.getDocumentElement().normalize();
	      System.out.println("Root element :" + doc.getDocumentElement().getNodeName());*/
	      
	      NodeList nList = doc.getElementsByTagName("dc:identifier");
	      
	      	      	      
	      for (int temp = 0; temp < nList.getLength(); temp++) {
	          Node nNode = nList.item(temp);	     	
	          pw.println(nNode.getTextContent());
	            
	      }     
	      pw.close();		  
		} catch (Exception e) {
		    e.printStackTrace();
		    }			
 }
}
	



