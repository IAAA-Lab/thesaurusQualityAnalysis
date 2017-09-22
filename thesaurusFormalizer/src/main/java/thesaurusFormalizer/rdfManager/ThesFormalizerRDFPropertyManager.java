package thesaurusFormalizer.rdfManager;

import org.apache.jena.rdf.model.Property;

import rdfManager.RDFPropertyManager;

/**
 * contiene las propiedades rdf usadas exclusivamente en este proyecto
 * @author jlacasta
 *
 */
public class ThesFormalizerRDFPropertyManager extends RDFPropertyManager{
	// propiedades interna usada para el proceso
	public static final String formalizerBaseUri = "http://iaaa.cps.unizar.es/formalizer#";
	public static final Property inner_conceptNoun = tempModel.createProperty(formalizerBaseUri, "inner_conceptNoun");
	public static final Property inner_conceptSynset = tempModel.createProperty(formalizerBaseUri, "inner_conceptSynset");
	public static final Property exact_wordnetMatch = tempModel.createProperty(formalizerBaseUri, "exact_wordnetMatch");
	public static final Property isa_wordnetMatch = tempModel.createProperty(formalizerBaseUri, "isa_wordnetMatch");
	public static final Property inner_possibleLabel_Synset = tempModel.createProperty(formalizerBaseUri, "inner_possibleLabel_Synset");
	public static final Property unalignable_wordnet = tempModel.createProperty(formalizerBaseUri, "unalignable_wordnet");
	public static final Property hasSubclass = tempModel.createProperty(formalizerBaseUri, "hasSubclass");
	public static final Property instanceOf = tempModel.createProperty(formalizerBaseUri, "instanceOf");
	public static final Property hasInstance = tempModel.createProperty(formalizerBaseUri, "has instance");
	
	public static final Property posIsPartOfProp = tempModel.createProperty(formalizerBaseUri, "posIsPartOf");
	public static final Property posHasPartProp = tempModel.createProperty(formalizerBaseUri, "posHasPart");
	public static final Property posSubClassOfProp = tempModel.createProperty(formalizerBaseUri, "posSubClassOf");
	public static final Property posHasSubclassProp = tempModel.createProperty(formalizerBaseUri, "posHasSubclass");
		
	
	
}
