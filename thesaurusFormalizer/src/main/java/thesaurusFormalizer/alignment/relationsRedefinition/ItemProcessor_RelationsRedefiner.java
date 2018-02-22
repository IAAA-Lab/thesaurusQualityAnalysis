package thesaurusFormalizer.alignment.relationsRedefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.springframework.batch.item.ItemProcessor;

import net.didion.jwnl.data.Synset;
import rdfManager.JenaModelManager;
import thesaurusFormalizer.rdfManager.ThesFormalizerRDFPropertyManager;
import tools.wordnetManager.EnglishWordnetManager;

/**
 * Coge las relaciones NT/BT de los conceptos y las RT y las redefine de acuerto 
 * a las relaciones identificadas en Dolce
 */
public class ItemProcessor_RelationsRedefiner implements ItemProcessor<Resource, Resource>{
	
	private static final Property skosBroaderProp = ThesFormalizerRDFPropertyManager.skosBroaderProp;
	private static final Property skosRelatedProp = ThesFormalizerRDFPropertyManager.skosRelatedProp;
	private static final Property rdfsSubclassOfProp = ThesFormalizerRDFPropertyManager.rdfsSubclassOfProp;
	private static final Property rdfsSubPropOfProp = ThesFormalizerRDFPropertyManager.rdfsSubPropOfProp;
	
	private static final Property rdfsDomainProp = ThesFormalizerRDFPropertyManager.rdfsDomainProp;
	private static final Property rdfsRangeProp = ThesFormalizerRDFPropertyManager.rdfsRangeProp;
	private static final Property owlInverseOfProp = ThesFormalizerRDFPropertyManager.owlInverseOfProp;
	private static final Property dctIsPartOfProp = ThesFormalizerRDFPropertyManager.dctIsPartOfProp;
	private static final Property dctHasPartProp = ThesFormalizerRDFPropertyManager.dctHasPartProp;
	
	private static final Property exact_wordnetMatch = ThesFormalizerRDFPropertyManager.exact_wordnetMatch;
	private static final Property isa_wordnetMatch = ThesFormalizerRDFPropertyManager.isa_wordnetMatch;
	private static final Property hasSubclass = ThesFormalizerRDFPropertyManager.hasSubclass;
	
	
	private Model dolceModel=null;
	private static EnglishWordnetManager jwnl = new EnglishWordnetManager();
	
	/************************************************************************/
	/**
	 * Coge las relaciones NT/BT de los conceptos y las RT y las redefine de acuerto 
	 * a las relaciones identificadas en Dolce
	 */
	public Resource process(Resource item) throws Exception {
			
		//redefinimos los broader/narrower
		processRelation(item,skosBroaderProp);
		
		//hacemos lo mismo para los related
		processRelation(item,skosRelatedProp);		
		
		return item;
	}
	
	/***********************************************************/
	/**
	 * procesamos el tipo de relación indicado
	 */
	public void processRelation(Resource item, Property relation){
		//miramos si el concepto está alineado con wordnet
		Synset itsyn, relsyn;
		if((itsyn=getSynset(item))==null){return;}
		
		//miramos si el concepto está alineado con dolce
		Resource dolceItem =null;
		List<Resource> ups = getDolceSuperConcepts(item); 
		if(ups.size()!=0){dolceItem = dolceModel.getResource(ups.get(0).getURI());}	
		
		for (Statement st:item.listProperties(relation).toList()){
			if((relsyn=getSynset(st.getResource()))==null){continue;}
			//buscamos relaciones jerarquicas en wordnet
			if(isSubclassOf(itsyn,relsyn)){
				item.addProperty(rdfsSubclassOfProp, st.getResource());
				st.getResource().addProperty(hasSubclass,item);
				continue;
			}else if (isSubclassOf(relsyn,itsyn)){
				st.getResource().addProperty(rdfsSubclassOfProp,item);
				item.addProperty(hasSubclass, st.getResource());
				continue;
			}
			
			//buscamos relaciones part-of
			if(isPartOf(itsyn,relsyn)){
				item.addProperty(dctIsPartOfProp, st.getResource());
				st.getResource().addProperty(dctHasPartProp,item);
				continue;
			}else if(isPartOf(relsyn,itsyn)){
				st.getResource().addProperty(dctIsPartOfProp,item);
				item.addProperty(dctHasPartProp, st.getResource());
				continue;
			}
				
			//si no hemos encontrado nada en wordnet miramos en dolce si hay alineamiento
			if(dolceItem==null){continue;}
			ups = getDolceSuperConcepts(st.getResource()); if(ups.size()==0){continue;}
			Resource relItem = dolceModel.getResource(ups.get(0).getURI());
			
			//buscamos todos los padres de cada relación
			List<Resource> conHie = getAllSuperConcept(dolceItem);
			HashMap<Resource,Integer> conResD = resDistance;
			List<Resource> relHie = getAllSuperConcept(relItem);
			HashMap<Resource,Integer> relResD = resDistance;
			
			//buscamos relación de supertipo
			if(conHie.subList(1,conHie.size()).contains(relHie.get(0))){
				item.addProperty(rdfsSubclassOfProp, st.getResource());
				st.getResource().addProperty(hasSubclass,item);
				continue;
			}else if (relHie.subList(1,relHie.size()).contains(conHie.get(0))){
				st.getResource().addProperty(rdfsSubclassOfProp,item);
				item.addProperty(hasSubclass, st.getResource());
				continue;
			}
			
			//buscamos otras posibles relaciones
			List<Property> posRelations = getOtherRelation(conHie, relHie,conResD,relResD);
			//consideramos que los terminos mas especificos tienen una relacion
			//of o in con sus superoriores
			if(relation.equals(skosBroaderProp)){
				posRelations = removeInverses(posRelations);
				posRelations = compactRelations(posRelations);
			}
			for(Property p:posRelations ){
				item.addProperty(p, st.getResource());
			}
		}			
	}
	
	/***********************************************************/
	/**
	 * obtenemos el synset con el que stá alineado el concepto
	 */
	private Synset getSynset(Resource item){
		if(item.hasProperty(exact_wordnetMatch)){
			return jwnl.getSynset(Long.parseLong(item.getProperty(exact_wordnetMatch).getString()));
		}else if(item.hasProperty(isa_wordnetMatch)){
			return jwnl.getSynset(Long.parseLong(item.getProperty(isa_wordnetMatch).getString()));	
		}
		return null;
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
	
	/**************************************************************/
	/**
	 * remove inverse relations of on e in
	 */
	private List<Property> removeInverses(List<Property> posRelations){
		List<Property> toDelete = new ArrayList<Property> ();
		for(Property p:posRelations){
			String uri = p.getURI(); 
			if (uri.endsWith("-of")||uri.endsWith("-in")){
				List<Statement> inverses = dolceModel.listStatements(p.asResource(),owlInverseOfProp,(RDFNode)null).toList();
				if(inverses.size()>0){
					toDelete.add(dolceModel.getProperty(inverses.get(0).getResource().getURI()));
				}else{
					inverses = dolceModel.listStatements(null,owlInverseOfProp,p.asResource()).toList();
					if(inverses.size()>0){
						toDelete.add(dolceModel.getProperty(inverses.get(0).getSubject().getURI()));
					}
				}
				
			}
		}
		List<Property> result = new ArrayList<Property>();
		for(Property p:posRelations){
			if(!toDelete.contains(p)){
				result.add(p);
			}
		}
		return result;
	}
	
	/****************************************************************/
	/**
	 * select remove from the list subrelations of other existent ones
	 */
	private List<Property> compactRelations(List<Property> posRelations){
		List<Property> toDelete = new ArrayList<Property> ();
		//select relations with father
		for(Property p:posRelations){
			List<Resource> conHie = getAllSuperProps(p.asResource());
			for(Property q:posRelations){
				if(!q.equals(p) && conHie.contains(q.asResource())){
					toDelete.add(p); break;
				}
			}
		}
		//delete these relations
		List<Property> result = new ArrayList<Property>();
		for(Property p:posRelations){
			if(!toDelete.contains(p)){
				result.add(p);
			}
		}
		
		//if result is more than 1 relation select the relation that is father of more ones
		if(result.size()>1){
			List<Property> result2 = new ArrayList<Property>();
			int max=0;
			for(Property res:result){
				int maxloc=0;
				for(Property p:posRelations){
					if(getAllSuperProps(p.asResource()).contains(res.asResource())){
						maxloc++;
					}	
				}
				if(maxloc>max){result2.clear();result2.add(res);max=maxloc;
				}else if(maxloc==max){result2.add(res);}
			}
			result=result2;
		}
		
		return result;
	}
	
	/****************************************************************/
	/**
	 * devuelve todas las propiedades de las que una propiedad es subpropiedad
	 */
	private List<Resource> getAllSuperProps(Resource res){
		List<Resource> fathers = new ArrayList<Resource> ();
		for(Statement st: res.listProperties(rdfsSubPropOfProp).toList()){
			fathers.add(st.getResource());
			fathers.addAll(getAllSuperProps(st.getResource()));
		}
		return fathers;
	}
	
	/***********************************************************/
	/**
	 * dados dos recursos de dolce selecciona la mejor relación no de sublcase
	 * (es decir no directa si no definida a traves de dominios y rangos)
	 */
	private List<Property> getOtherRelation(List<Resource> conHie, List<Resource> relHie,HashMap<Resource,Integer> conResD,HashMap<Resource,Integer> relResD){		
		//obtenemos las relaciones jerarquicas (subclass) de dolce
		List<Property> relations =new ArrayList<Property>(); int di=1000, dj=1000;
		for(Resource cH:conHie){
			int i = conResD.get(cH);
			for(Resource rH:relHie){
				int j = relResD.get(rH);
				List<Property> relationsPos = getOtherRelation(cH,rH);
				if(relationsPos.size()>0 && ((i+j<di+dj)||(i+j==di+dj && Math.abs(i-j)<Math.abs(di-dj)))){
					relations =relationsPos;di=i;dj=j;
				}
			}
		}
		return relations;
	}
	/**
	 *  Devuelve las relaciones de dolce que tengan el dominio y rango indicados
	 */
	private List<Property> getOtherRelation(Resource domain, Resource range){	
		
		List<Property> props = new ArrayList<Property>();
		for(Statement st: dolceModel.listStatements(null,rdfsDomainProp,domain).toList()){
			Resource prop =st.getSubject();
			if(dolceModel.contains(prop, rdfsRangeProp, range)){
				props.add(dolceModel.getProperty(prop.getURI()));
			}
		}
		return props;
	}
	
	/***********************************************************************/
	/**
	 * returns the super Class of a concept (if is in Dolce), if there is not it return null
	 * 1 level, no transitive
	 */
	private List<Resource> getDolceSuperConcepts(Resource item){
		List<Resource> supers = new ArrayList<Resource>();
		for (Statement st:item.listProperties(rdfsSubclassOfProp).toList()){
			Resource res = st.getResource();
			if(res.isURIResource() && res.getURI().contains("www.loa-cnr.it")){
				supers.add(st.getResource());
			}
		}
		return supers;
	}
	
	/*********************************************************/
	/**
	 * returns the super Class of a concept, if there is not it return null
	 */
	private HashMap<Resource,Integer> resDistance;
	private List<Resource> getAllSuperConcept(Resource item){
		resDistance= new HashMap<Resource,Integer>();
		int contador=0;
		List<Resource> resL = new ArrayList<Resource>(); 
		resL.add(item); resDistance.put(item,contador++);
		
		List<Resource> uppers = new ArrayList<Resource>(resL);
		boolean continuar = true;
		while (continuar){
			List<Resource> newUpps = new ArrayList<Resource>();
			for(Resource upper:uppers){
					newUpps.addAll(getDolceSuperConcepts(upper));
			}
			if(newUpps.size()==0){break;}
			for(Resource n:newUpps){resDistance.put(n,contador);}contador++;
			resL.addAll(newUpps);uppers=newUpps;
		}
		return resL;
	}
	
	/***************************************************************/
	/**
	 * propiedades del tasklet
	 */
	public void setDolceModel(String dolceModel) throws Exception{
		this.dolceModel = JenaModelManager.loadJenaModel(dolceModel);
	}
}
