package thesaurusFormalizer.alignment.dolceMatch;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.springframework.batch.item.ItemProcessor;

import net.didion.jwnl.data.Synset;
import thesaurusFormalizer.rdfManager.ThesFormalizerRDFPropertyManager;
import tools.dataStructure.SynsetPlusLevel;
import tools.wordnetManager.EnglishWordnetManager;

/**
 * Realizamos el alienamiento entere cualquier cocnepto de wordnet y dolce
 * en base a los alieamientos de conceptos de alto nivel definidos en wonderweb
 */
public class ItemProcessor_DolceMatchSelection implements ItemProcessor<Resource, Resource>{

	//modelo acceso a wordnet
	private EnglishWordnetManager jwnl = new EnglishWordnetManager();	
	
	//hash destino con el alineamiento preparado para su uso
	HashMap<String,String> wnDolce;
	
	//propiedades de rdf usadas
	private static final Property exact_wordnetMatch = ThesFormalizerRDFPropertyManager.exact_wordnetMatch; 
	private static final Property isa_wordnetMatch = ThesFormalizerRDFPropertyManager.isa_wordnetMatch;
	
	private static final Property rdfsSubclassOfProp = ThesFormalizerRDFPropertyManager.rdfsSubclassOfProp;
	
	/**
	 * Realizamos el alienamiento entere cualquier cocnepto de wordnet y dolce
	 * en base a los alieamientos de conceptos de alto nivel definidos en wonderweb
	 */
	public Resource process(Resource item) throws Exception {
		//obtenemos el synsets seleccionado del concepto (equ exacta o sublcase)
		String synset =null;
		if (item.hasProperty(exact_wordnetMatch)){		
			synset = item.getProperty(exact_wordnetMatch).getString();
		}else if(item.hasProperty(isa_wordnetMatch)){
			synset = item.getProperty(isa_wordnetMatch).getString();
		}else{
			return item;
		}
		
		//subimos en la jerarquía de wordnet hasta encontrar el/los mascercanos
		//que estan alineados con dolce
		Synset toalign = jwnl.getSynset(Long.valueOf(synset));
		int level =1000; List<String> dolceUris = new ArrayList<String>();
		List <SynsetPlusLevel> allHyperns = jwnl.getHypernymsTransitiveWithLevel(toalign,-1,-1);
		for (SynsetPlusLevel ns:allHyperns){
			String posAlign = Long.toString(ns.get_synset().getOffset());
			if(wnDolce.containsKey(posAlign)){
				if (ns.get_level()<level){
					level = ns.get_level();
					dolceUris.clear(); dolceUris.add(wnDolce.get(posAlign));
				}else if(ns.get_level()==level){
					dolceUris.add(wnDolce.get(posAlign));
				}
			}
		}
		
		//añadimos los alineamientos encontrados
		for(String dolceU:dolceUris){
			item.addProperty(rdfsSubclassOfProp, item.getModel().createResource(dolceU));
		}
		
		return item;
	}
	
	/**************************************************************/
	/**
	 * propiedades del tesklet
	 */
	@SuppressWarnings("unchecked")
	public void setWNDolceAlignHash(String wNDolceAlignHash) {
		try {
			ObjectInputStream entrada=new ObjectInputStream(new FileInputStream(wNDolceAlignHash));
			wnDolce =(HashMap<String,String>)entrada.readObject();
			entrada.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
}
