package thesaurusFormalizer.alignment.relationsRedefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import net.didion.jwnl.data.Synset;
import thesaurusFormalizer.rdfManager.ThesFormalizerRDFPropertyManager;
import tools.wordnetManager.EnglishWordnetManager;

/**
 * Coge todos los conceptos y los compara entre si para encontrar relaciones entre ellos
 * no explicitamente definidas como BT/NT y que puedan ser usadas apra reestructurar el tesaruo
 * ESTA EN DESARROLLO
 */
public class ItemProcessor_RelationsCreator implements ItemProcessor<Resource, Resource>{
	
	private static final Property skosBroaderProp = ThesFormalizerRDFPropertyManager.skosBroaderProp;
	private static final Property exact_wordnetMatch = ThesFormalizerRDFPropertyManager.exact_wordnetMatch;
	private static final Property isa_wordnetMatch = ThesFormalizerRDFPropertyManager.isa_wordnetMatch;
	
	private static final Property posIsPartOfProp = ThesFormalizerRDFPropertyManager.posIsPartOfProp;
	private static final Property posHasPartProp = ThesFormalizerRDFPropertyManager.posHasPartProp;
	private static final Property posSubClassOfProp = ThesFormalizerRDFPropertyManager.posSubClassOfProp;
	private static final Property posHasSubclassProp = ThesFormalizerRDFPropertyManager.posHasSubclassProp;
	
	private static EnglishWordnetManager jwnl = new EnglishWordnetManager();

	/************************************************************************/
	/**
	 * Coge todos los conceptos y los compara entre si para encontrar relaciones entre ellos
	 * no explicitamente definidas como BT/NT y que puedan ser usadas apra reestructurar el tesaruo
	 * ESTA EN DESARROLLO
	 */
	public Resource process(Resource item) throws Exception {	
		//la primera ejecucion precalculamos todo lo que se va a usar
		initiateRelationFinder(item);
		
		//obtenemos todos los conceptos no relacionados de uno dado
		List<Resource> unrelated = new LinkedList<Resource>(); 
		for(Resource res:concepts){
			if (!res.equals(item) && !isNarrower(item,res) && !isNarrower(res,item)){
				unrelated.add(res);
			}
		}
		
		//buscamos su sentido en wordnet
		Synset sitem = getSynset(item);
		for(Resource res:unrelated){
			Synset sres = getSynset(res);
		
			if(sres!=null && sitem!=null && !sres.equals(sitem) && "exact".equals(type.get(res))){
				//miramos si tienen relación de subclase
				boolean addsc=false, addpo=false;
				if(isSubclassOf(sitem,sres)){
					addsc=true;
				}
				//miramos si tienen relación de parte
				if(isPartOf(sitem,sres)){
					addpo=true;
				}	
				
				synchronized(this){
					if(addsc){
						item.addProperty(posSubClassOfProp, res);
						res.addProperty(posHasSubclassProp, item);
					}
					if(addpo){
						item.addProperty(posIsPartOfProp, res);
						res.addProperty(posHasPartProp, item);
					}
				}
			}
		}
		compressRelations(item,posSubClassOfProp,posHasSubclassProp);
		compressRelations(item,posIsPartOfProp,posHasPartProp);
		return item;
	}
	
	/******************************************************************/
	/**
	 * mira las relacions isA y parte y si entre ellas son jerarquicas elimina las generales
	 */
	private void compressRelations(Resource item, Property prop, Property inve){
		if(item.hasProperty(prop)){
			List<Resource> invalid = new ArrayList<Resource>();
			for(Statement st:item.listProperties(prop).toList()){
				for(Statement st2:item.listProperties(prop).toList()){
					if(st.getResource()!=st2.getResource() && isNarrower(st2.getResource(),st.getResource())){
						invalid.add(st.getResource());
						break;
					}
				}
			}
			for(Resource inv:invalid){
				item.getModel().remove(item, prop, inv);
				item.getModel().remove(inv, inve, item);
			}
		}
	}
		
	/***********************************************************/
	/**
	 * obtenemos el synset con el que stá alineado el concepto
	 */
	private Synset getSynset(Resource item){
		return synsets.get(item);
	}
	
	/*******************************************************************/
	/**
	 * identificamos si itsyn es sublcase de de relsyn en wordnet
	 */
	private boolean isSubclassOf (Synset itsyn, Synset relsyn){
		if(jwnl.getHypernymsTransitive(itsyn).contains(relsyn)){return true;}
		return false;
	}
	
	/*******************************************************************/
	/**
	 * identificamos si itsyn es parte de de relsyn en wordnet
	 */
	private boolean isPartOf (Synset itsyn, Synset relsyn){
		if(jwnl.getHolonymsTransitive(itsyn).contains(relsyn)){return true;}
		return false;
	}
		
	/****************************************************************/
	/**
	 * si ti es narrower de res
	 */
	private boolean isNarrower(Resource it, Resource res){
		if(broaders.get(it).contains(res)){
			return true;
		}
		return false;
	}
	
	/***********************************************************************/
	/***********************************************************************/
	/**
	 * Función que extrae del modelo información reusada en todos los pasos de
	 * identificación de las relacione
	 */
	private boolean initiated = false;
	private List <Resource> concepts = null;
	private HashMap <Resource, List<Resource>> broaders = null;
	private HashMap <Resource,Synset> synsets = null;
	private HashMap <Resource,String> type = null;
	
	private synchronized void initiateRelationFinder(Resource item){
		if(!initiated){
			Model modelo = item.getModel();
			concepts = modelo.listSubjects().toList();
			broaders =  getAllBroaders(modelo);
			type = new HashMap<Resource,String>();
			synsets =  getconceptSynsets(modelo, type);
			initiated=true;
		}	
	}
	
	/***********************************************************************/
	/**
	 * obtenemos todos los broaders de todos los conceptos
	 */
	private HashMap <Resource, List<Resource>> getAllBroaders(Model modelo){
		HashMap<Resource,List<Resource>> result = new HashMap<Resource,List<Resource>>();
		for(Resource res:modelo.listSubjects().toList()){
			result.put(res, getBroaders(res, new ArrayList<Resource>()));
		}
		return result;
	}
	
	/***********************************************************************/
	/**
	 * obtenemos los broaders de un concepto.
	 */
	private List<Resource> getBroaders(Resource it, List<Resource> previous){
		List<Resource> broad = new ArrayList<Resource>();
		for(Statement st: it.listProperties(skosBroaderProp).toList()){
			if(!previous.contains(st.getResource())){
				broad.add(st.getResource());
				previous.add(st.getResource());
				broad.addAll(getBroaders(st.getResource(),previous));
			}else{
				System.err.println(st.getResource().getURI());
			}
		}
		return broad;
	}
	
	/***********************************************************************/
	/**
	 * obtenemos los synsets de todos los conceptos del tesauro
	 */
	private HashMap<Resource,Synset> getconceptSynsets(Model modelo, HashMap<Resource,String> type){
		HashMap<Resource,Synset> result = new HashMap<Resource,Synset>();
		for(Resource res:modelo.listSubjects().toList()){
			Synset s =null;
			String types = null;
			if(res.hasProperty(exact_wordnetMatch)){
				s = jwnl.getSynset(Long.parseLong(res.getProperty(exact_wordnetMatch).getString()));
				types ="exact";
			}else if(res.hasProperty(isa_wordnetMatch)){
				s = jwnl.getSynset(Long.parseLong(res.getProperty(isa_wordnetMatch).getString()));	
				types ="isa";
			}
			
			result.put(res, s);
			type.put(res, types);
		}
		return result;
	}
}
