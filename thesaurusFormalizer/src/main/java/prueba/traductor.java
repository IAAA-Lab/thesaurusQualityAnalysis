/**
 * 
 */
/**
 * @author dayanydc
 *
 */
package prueba;


import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RIOT;

import rdfManager.JenaModelManager;

public class traductor {

	public static void main(String[] args) throws Exception {
	
		InputStream in = new FileInputStream("data/input/thesauri/iconclass.nt");
		String file = "data/input/thesauri/iconclass.rdf";
		//InputStream in = new FileInputStream("data/input/thesauri/AATOut_Sources.nt");
		//FileWriter out = new FileWriter("data/input/thesauri/AATOut_Sources.rdf");
		//InputStream in = new FileInputStream("data/input/thesauri/TGNOut_Sources.nt");
		//String file = "data/input/thesauri/TGNOut_Sources.rdf";
		//InputStream in = new FileInputStream("data/input/thesauri/ULANOut_Sources.nt");
		//String file = "data/input/thesauri/ULANOut_Sources.rdf";
					
	    Model model = ModelFactory.createDefaultModel(); // creates an in-memory Jena Model
	    model.read(in,null,"N-TRIPLES"); // parses an InputStream assuming RDF in N-TRIPLES format
	    JenaModelManager.saveJenaModel(model, file);
	}
}


