package tools.wordnetManager;

import java.util.ArrayList;
import java.util.List;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.PointerUtils;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.list.PointerTargetNode;
import net.didion.jwnl.data.list.PointerTargetNodeList;
import net.didion.jwnl.dictionary.Dictionary;
import tools.dataStructure.SynsetPlusLevel;


/**
 * manager de la libreria de Wordnet
 * basado en el del thmanager
 */
public class EnglishWordnetManager {
	//fichero de propiedades de jwnl
	private static final String jwnlPropFile = "englishWordnet_exactMatchAlignment.xml";
	
	/*********************************************************/
	/**
	 * inicializa las variables necesarias apra acceder a wordnet
	 */
	public EnglishWordnetManager(){
		try {
			if(!JWNL.isInitialized()){
				JWNL.initialize(getClass().getResourceAsStream(jwnlPropFile));
			}
		} catch (Exception ex) {throw new RuntimeException();}
	}
	
	/*********************************************************/
	/**
	 * busca un concepto a ver si esta en wordnet y devuelve sus synsets
	 */
	public List<Synset> getSynsets (String lemma){
		List<Synset> listSyn= new ArrayList<Synset>();
		try{
			IndexWord indexW = Dictionary.getInstance().lookupIndexWord(POS.NOUN,lemma);
			if(indexW!=null){
				for (int i = 1; i <= indexW.getSenseCount(); i++) {
					listSyn.add(indexW.getSense(i));
				}
			}
			return listSyn;
		}catch (Exception er){er.printStackTrace();throw new RuntimeException();}
	}
	
	/*********************************************************************************/
	/**
	 * dado un id de synset devuelve el synset
	 */
	public Synset getSynset (long id){
		try{
			return Dictionary.getInstance().getSynsetAt(POS.NOUN,id);
		}catch(Exception er){throw new RuntimeException(er);}
	}
	
	/***********************************************************************************/
	/**
	 * devuelve los synsets superiores a uno dado indicando el nivel en el que lo ecuentra
	 */
	public List<SynsetPlusLevel> getHypernymsTransitiveWithLevel(Synset syn,int level, int branch){
		List<SynsetPlusLevel> ancestors = new ArrayList<SynsetPlusLevel>();		
		try{		
			//obtenemos los padres del concepto indicado
			PointerTargetNodeList dhyperList = PointerUtils.getInstance().getDirectHypernyms(syn);
			for(int i =0;i< dhyperList.size();i++){ 
		        //a�adimos cada padre a la lista de antecesores
				Synset ancestor = ((PointerTargetNode) dhyperList.get(i)).getSynset();
				ancestors.add(new SynsetPlusLevel(ancestor,level,branch));
				//anadimos todos los antecesores del padre
				List<SynsetPlusLevel> partAncs = getHypernymsTransitiveWithLevel(ancestor,level+1,branch);
				ancestors.addAll(partAncs);
			}
			return ancestors;	
		}catch (Exception er){throw new RuntimeException();}
	}
	
	/***********************************************************************************/
	/**
	 * devuelve los synsets superiores a uno dado indicando el nivel en el que lo ecuentra
	 */
	public List<Synset> getDirectHypernym(Synset syn){	
		List<Synset> ancestors = new ArrayList<Synset>();		
		try{		
			//obtenemos los padres del concepto indicado
			PointerTargetNodeList dhyperList = PointerUtils.getInstance().getDirectHypernyms(syn);
			for(int i =0;i< dhyperList.size();i++){ 		     
				ancestors.add(((PointerTargetNode) dhyperList.get(i)).getSynset());								
			}
			return ancestors;	
		}catch (Exception er){throw new RuntimeException();}
	}
	
	
	
	
	/***********************************************************************************/
	/**
	 * devuelve los synsets superiores a uno dado indicando el nivel en el que lo ecuentra
	 * incluido el mismo
	 */
	public List<Synset> getHypernymsTransitive(Synset syn){
		List<Synset> ancestors = new ArrayList<Synset>();
		//a�adimos el concepto a procesar (es su ancestro)
		ancestors.add(syn);
		//obtenemos los padres del concepto indicado
		try {
			PointerTargetNodeList dhyperList = PointerUtils.getInstance().getDirectHypernyms(syn);
			for(int i =0;i< dhyperList.size();i++){ 		
				ancestors.addAll(getHypernymsTransitive(((PointerTargetNode) dhyperList.get(i)).getSynset()));
			}
		} catch (JWNLException e) {e.printStackTrace();}
		return ancestors;	
	}
	
	
	/***********************************************************************************/
	/**
	 * devuelve los synsets de los que uno es parte
	 */
	public List<Synset> getDirectHolonyms(Synset syn){
		List<Synset> ancestors = new ArrayList<Synset>();		
		try{		
			//obtenemos los padres del concepto indicado
			PointerTargetNodeList dhyperList = PointerUtils.getInstance().getHolonyms(syn);
			for(int i =0;i< dhyperList.size();i++){ 		     
				ancestors.add(((PointerTargetNode) dhyperList.get(i)).getSynset());								
			}
			return ancestors;	
		}catch (Exception er){throw new RuntimeException();}
	}
	
	/***********************************************************************************/
	/**
	 * devuelve los synsets de los que uno es parte a uno dado indicando el nivel en el que lo ecuentra
	 * incluido el mismo
	 */
	public List<Synset> getHolonymsTransitive(Synset syn){
		List<Synset> ancestors = new ArrayList<Synset>();
		//a�adimos el concepto a procesar (es su ancestro)
		ancestors.add(syn);
		//obtenemos los padres del concepto indicado
		try {
			PointerTargetNodeList dhyperList = PointerUtils.getInstance().getHolonyms(syn);
			for(int i =0;i< dhyperList.size();i++){ 		
				ancestors.addAll(getHolonymsTransitive(((PointerTargetNode) dhyperList.get(i)).getSynset()));
			}
		} catch (JWNLException e) {e.printStackTrace();}
		return ancestors;	
	}
	
	
	
}
