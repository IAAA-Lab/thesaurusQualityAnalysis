/**
 * 
 */
/**
 * @author dayanydc
 *
 */
package prueba;


import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import rdfManager.JenaModelManager;

public class traductor {

	public static void main(String[] args) throws Exception {
	
		InputStream in = new FileInputStream("data/input/thesauri/mesh2018.nt");
		String file = "data/input/thesauri/mesh2018.rdf";					
	    Model model = ModelFactory.createDefaultModel(); // creates an in-memory Jena Model
	    model.read(in,null,"N-TRIPLES"); // parses an InputStream assuming RDF in N-TRIPLES format
	    JenaModelManager.saveJenaModel(model, file);
	}
}


