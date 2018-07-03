/**
 * 
 */
/**
 * @author dayanydc
 *
 */
package prueba;

import java.io.FileWriter;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;

public class traductor {

	@Autowired
	public static void main(String[] args) throws Exception {

		Model model = ModelFactory.createDefaultModel();
		model.read("data/input/thesauri/openUp.ttl", "TURTLE");
		List<Resource> concepts = model.listSubjects().toList();
		FileWriter fw = new FileWriter("data/input/thesauri/openUpEx.ttl");
		String label_obj;
		String lang_esp = "Language: Spanish";
		String lang_es = "Language: es";
		String lang_en = "Language: en";
		String lang_eng = "Language: English";
		
		String lang_fr = "Language: fr";
		String lang_fren = "Language: french";
		String value;

		Property Note = model.createProperty("http://www.w3.org/2004/02/skos/core#note");
		Property PrefLabelProp = model.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");

		for (Resource res : concepts) {
			for (Statement st : res.listProperties(Note).toList()) {

				label_obj = st.getObject().toString();

				if (label_obj.equals(lang_en) || label_obj.equals(lang_eng) ) {
					value = res.getProperty(PrefLabelProp).getObject().toString();
					res.removeAll(PrefLabelProp);
					res.addProperty(PrefLabelProp, value, "en");
					// System.out.println(res.getProperty(PrefLabelProp).getObject().toString());

				}
				if (label_obj.equals(lang_es) || label_obj.equals(lang_esp)) {
					value = res.getProperty(PrefLabelProp).getObject().toString();
					res.removeAll(PrefLabelProp);
					res.addProperty(PrefLabelProp, value, "es");
					// System.out.println(res.getProperty(PrefLabelProp).getObject().toString());

				}
				if (label_obj.equals(lang_fr) || label_obj.equals(lang_fren)) {
					value = res.getProperty(PrefLabelProp).getObject().toString();
					res.removeAll(PrefLabelProp);
					res.addProperty(PrefLabelProp, value, "fr");
					// System.out.println(res.getProperty(PrefLabelProp).getObject().toString());

				}
			}
		}
		model.write(fw);
		fw.close();
	}
}
