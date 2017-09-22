package thesaurusFormalizer.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;



import org.apache.commons.lang.StringUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import rdfManager.JenaModelManager;
import thesaurusFormalizer.rdfManager.ThesFormalizerRDFPropertyManager;
import tools.wordnetManager.EnglishWordnetManager;
import java.util.regex.*;

/**
 * analiza los alineamientos de un tesauro con wordnet
 */
public class Tasklet_ThesaurusFormalizationAnalyzer implements Tasklet{

	//propidades del tasklet
	private String fileToAnalize ="data/output/formalThes/formalUrbament.rdf";
	private String resultDir ="data/output/analysis";
	private String[] langsToAnalyze = new String[]{"en","es"};
	
	//modelo acceso a wordnet
	private EnglishWordnetManager jwnl = new EnglishWordnetManager();
	
	//propiedades de rdf
	private static final Property inner_conceptSynset = ThesFormalizerRDFPropertyManager.inner_conceptSynset; 
	private static final Property skosPrefLabelProp = ThesFormalizerRDFPropertyManager.skosPrefLabelProp; 
	private static final Property skosAltLabelProp = ThesFormalizerRDFPropertyManager.skosAltLabelProp; 
	private static final Property exact_wordnetMatch = ThesFormalizerRDFPropertyManager.exact_wordnetMatch; 
	private static final Property isa_wordnetMatch = ThesFormalizerRDFPropertyManager.isa_wordnetMatch; 
	private static final Property unalignable_wordnet = ThesFormalizerRDFPropertyManager.unalignable_wordnet; 
	private static final Property inner_possibleLabel_Synset = ThesFormalizerRDFPropertyManager.inner_possibleLabel_Synset;
	private static final Property inner_conceptNoun = ThesFormalizerRDFPropertyManager.inner_conceptNoun;
	private static final Property rdfsSubclassOfProp = ThesFormalizerRDFPropertyManager.rdfsSubclassOfProp;
	private static final Property skosNarrowerProp = ThesFormalizerRDFPropertyManager.skosNarrowerProp;
	private static final Property skosBroaderProp = ThesFormalizerRDFPropertyManager.skosBroaderProp;
	private static final Property skosRelatedProp = ThesFormalizerRDFPropertyManager.skosRelatedProp;
	private static final Property skosDefinitionProp = ThesFormalizerRDFPropertyManager.skosDefinitionProp;
	private static final Property skosHasTopConcProp = ThesFormalizerRDFPropertyManager.skosHasTopConcProp;
 
		
	/************************************************************************/
	/**
	 * analiza los alineamientos de un tesauro con wordnet
	 */
	//Set<Resource> narrows ;
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		
		Model modelo = JenaModelManager.loadJenaModel(fileToAnalize);
		String fileName = new File(fileToAnalize).getName();

		// narrows = getNarrows( modelo.getResource("http://www.urbamet.com/thesaurus/urban_planning_development"),modelo);
		
		List<Resource> concepts = modelo.listSubjects().toList();
		List<Resource> concepts2 = new ArrayList<Resource>();
		Resource esquema= null;
		for(Resource r: concepts){
			Statement st = r.getProperty(ThesFormalizerRDFPropertyManager.rdfTypeProp);
			if (st==null){continue;}
			Resource type = st.getResource();
			if(type.equals( ThesFormalizerRDFPropertyManager.skosConceptRes)){
				concepts2.add(r);
			}
			else {   				
				 esquema=r;
			}
				
		}
		concepts=concepts2;
		
		//obtenemos medidas de rendimiento de cada idioma
		System.out.println("languageDependentAlignMeasures");	
		for(String lang: langsToAnalyze){
			languageDependentAlignMeasures(lang, concepts,fileName);
		}
		
		System.out.println("simpleQualityMeasures");
		simpleQualityMeasures(concepts,fileName,esquema);
		
		//comprobamos que las relaciones nt no forman ciclos
		//System.out.println("detectIncorrectHierarchy");
		//detectIncorrectHierarchy(concepts);
		
		//comprobamos que los related no sean jerarquicos ni hermanos
		//System.out.println("detectIncorrectRTs");
		//detectIncorrectRTs(concepts);
		
		
		//guardamos todos los emparejamientos directos e indirectos con wnet de cada idioma ordenados por tipo
		System.out.println("storeAlignments");
		for(String lang: langsToAnalyze){
			storeAlignments(lang, concepts,true, fileName);
			storeAlignments(lang, concepts,false, fileName);
		}
		
		//guardamos medidas de alineamiento con wnet  solo con las etiquetas
		System.out.println("labelAlignmentMeasures");
		labelAlignmentMeasures(modelo, fileName);
		
		//guardamos los alineamientos con wnet conflictivos
		System.out.println("storeConflictAligns");
		storeConflictAligns(modelo, fileName);
		
		//guardamos los conceptos no desambiguados con wnet 
		System.out.println("storeUndisambiguatedAligns");
		storeUndisambiguatedAligns(modelo, fileName);
		
		//guardamos los alineamientos con dolce encontrados
		System.out.println("storeDolceAlignments");
		storeDolceAlignments(modelo, fileName);
		
		//guaradamos aquellos conceptos sin alineamiento con dolce y su causa
		System.out.println("storeDolceUnalignments");
		storeDolceUnalignments(modelo, fileName);
		
		//guardamos las relaciones encontradas en dolce entre los conceptos
		System.out.println("storeDolceRelations");
		storeDolceRelations(modelo,fileName);
		
		return RepeatStatus.FINISHED;
	}
	
	
	/***************************************************************/
	/**
	 * guardamos las relaciones encontradas en dolce entre los conceptos
	 */
	private void storeDolceRelations(Model modelo, String fileName){
		try { //mostramos las relaciones refinadas
			PrintStream informe = new PrintStream(new File(resultDir+File.separator+fileName+"_DolceRelations.txt"));
			storeDolceRelations (modelo, informe,"BT/NT",skosBroaderProp);
			storeDolceRelations (modelo, informe,"RT",skosRelatedProp);	
			informe.close();	
		}catch(Exception er){er.printStackTrace();}		
	}
	
	/**
	 * guardamos las relaciones encontradas en dolce entre los conceptos del tipo indicado
	 */
	private void storeDolceRelations(Model modelo, PrintStream informe, String relation, Property relprop){
		//obtenemos las relaciones del tipo indicado
		List<Statement> relToAnaly = modelo.listStatements(null, relprop, (RDFNode)null).toList();
		
		//separamos las relaciones en redefinidas y no redefinidas
		List<Statement> redefRel = new ArrayList<Statement>();
		List<Statement> noRedefRel = new ArrayList<Statement>();
		for (Statement s: relToAnaly){
			Resource source = s.getSubject(); Resource object = s.getResource();
			boolean found=false;
			for (Statement sr: modelo.listStatements(source, null, object).toList()){
				Property prop = sr.getPredicate();
				if (!prop.equals(skosNarrowerProp) && !prop.equals(skosBroaderProp) && !prop.equals(skosRelatedProp)  ){
					redefRel.add(s); found=true; break;
				}
			}
			if(!found){noRedefRel.add(s);}
		}
		
		//las no redefinidas las separamos en alineadas y no alineadas
		List<Statement> alignedRel = new ArrayList<Statement>();
		for (Statement s: noRedefRel){
			Resource source = s.getSubject(); Resource object = s.getResource();
			if(source.hasProperty(rdfsSubclassOfProp) && object.hasProperty(rdfsSubclassOfProp)){
				alignedRel.add(s);
			}
		}
			
		//mostramos un resumen de las medidas
		informe.println("-----------------------------------------------");
		informe.println("Numero de relaciones "+relation);
		informe.println("Numero original "+relation+": "+ relToAnaly.size());
		informe.println("Numero "+relation+" redefinidas: "+ redefRel.size());
		informe.println("Relaciones "+relation+" no identificadas "+alignedRel.size());
		informe.println("Relaciones "+relation+" no alineadas "+(noRedefRel.size()-alignedRel.size()));
		
		//obtenemos las nuevas relaciones que reemplazan a las viejas
		for(Statement s:redefRel){
			Resource source = s.getSubject(); Resource object = s.getResource();
			informe.println("-----------------------------------------------");
			informe.println(source.getURI()+" -"+relation+"- "+object.getURI());
			for (Statement sr: modelo.listStatements(source, null, object).toList()){
				Property prop = sr.getPredicate();
				if (!prop.equals(skosNarrowerProp) && !prop.equals(skosBroaderProp) && !prop.equals(skosRelatedProp)  ){
					informe.println("     "+prop.getURI());
				}
			}
		}
		
		//obtenemos las partes involucradas en la relación no definida
		informe.println("-----------------------------------------------");
		informe.println("Lista "+relation+" sin redefinir");
		informe.println("-----------------------------------------------");
		for(Statement s:alignedRel){
			Resource source = s.getSubject(); Resource object = s.getResource();
			informe.println("-----------------------------------------------");
			informe.println(source.getURI()+" -"+relation+"- "+object.getURI());
			
			for(Statement si: source.listProperties(rdfsSubclassOfProp).toList()){
				if(si.getResource().getURI().contains("DOLCE-Lite.owl")){
					informe.println("     "+si.getResource().getURI());break;
				}
			}
			for(Statement si: object.listProperties(rdfsSubclassOfProp).toList()){
				if(si.getResource().getURI().contains("DOLCE-Lite.owl")){
					informe.println("     "+si.getResource().getURI());break;
				}
			}
		}	
	}
	
	/***************************************************************/
	/**
	 * guarda los alineamientos con dolce encontrados para facilitar su revision
	 */
	private void storeDolceAlignments(Model modelo, String fileName){
		try { //mostramos los elementos alineados
			PrintStream informe = new PrintStream(new File(resultDir+File.separator+fileName+"_DolceAlignments.txt"));
			List<Resource> matches = modelo.listSubjectsWithProperty(rdfsSubclassOfProp).toList();
			int[] numDolceAligns = new int[5];
			for (Resource res:matches){
				informe.println(res.getURI());
				int numAligns =0;
				for(Statement s:res.listProperties(rdfsSubclassOfProp).toList()){
					if(s.getResource().getURI().contains("www.loa-cnr.it")){
						informe.println("   "+s.getResource().getURI());numAligns++;
					}
				}
				numDolceAligns[numAligns]++;
			}
			
			//mostramos un resumen de las medidas
			informe.println("-----------------------------------------------");
			informe.println("Numero de alineamientos con dolce");
			int total=0;
			for(int i=1;i<numDolceAligns.length;i++){
				total+=numDolceAligns[i];
				informe.println("alineamientos con "+i+"conceptos de dolce: "+numDolceAligns[i]);
			}
			int totalConc = modelo.listSubjects().toList().size();
			informe.println("Total conceptos: "+totalConc);
			informe.println("Total alineamientos: "+total);
			informe.println("No alineados: "+(totalConc-total));
			informe.close();	
		}catch(Exception er){er.printStackTrace();}
	}
	
	/***************************************************************/
	/**
	 * guarda los conceptos no alienados con dolce y la causa
	 */
	private void storeDolceUnalignments(Model modelo, String fileName){
		int noabyDolce =0,noabyMulti=0,noabySyn=0;
		try {
			PrintStream informe = new PrintStream(new File(resultDir+File.separator+fileName+"_IncompleteDolceAlignments.txt"));
			List<Resource> thesConc = modelo.listSubjects().toList();
			for (Resource st:thesConc){
				//añadimos los no alineados y las causas de su no alineamiento
				//no tiene synsets, tiene multiples synsets, o el synset no esta alineado con dolce
				if(!st.hasProperty(rdfsSubclassOfProp)){
					informe.println("------------------------------------------------------");
					informe.println(st.getURI());
					for (Statement st2: st.listProperties(skosPrefLabelProp).toList()){
						informe.print(" | "+st2.getLanguage()+": "+st2.getString());							
					}
					informe.println();
					
					//el synset no esta alineado con dolce
					if(st.hasProperty(isa_wordnetMatch)||st.hasProperty(exact_wordnetMatch)){
						informe.println("   El synset en wordnet no tiene alineamiento con dolce");					
						String synId = st.hasProperty(isa_wordnetMatch) ? st.getProperty(isa_wordnetMatch).getString() : st.getProperty(exact_wordnetMatch).getString();						
						Synset sy = jwnl.getSynset(Long.parseLong(synId));
						informe.println("  "+Arrays.toString(sy.getWords())+" "+sy.getGloss());
						noabyDolce++;
					//multiples synsets
					}else if (st.hasProperty(inner_possibleLabel_Synset)){			
						informe.println("   No ha sido posible seleccionar un único synset");	
						for(String synId:st.getProperty(inner_possibleLabel_Synset).getString().split("\\|")){
							Synset sy = jwnl.getSynset(Long.parseLong(synId));
							informe.println("  "+Arrays.toString(sy.getWords())+" "+sy.getGloss());		
						}
						noabyMulti++;
					//no hay synset
					}else{
						informe.println("   El concepto del tesauro no se ha alineado con wordnet");
						noabySyn++;
					}
				}
			}
			informe.println("----------------------------------------------");
			informe.println("Medidas de no alineados con dolce");
			informe.println("Total de no alineados por no mapping con dolce: "+noabyDolce);
			informe.println("Total de no alineados por existencia de multiples synsets: "+noabyMulti);
			informe.println("Total de no alineados por falta de synsets: "+noabySyn);
			
			informe.close();	
		}catch(Exception er){er.printStackTrace();}
	}
	
	
	/***************************************************************/
	/**
	 * guarda las medidas de alineamiento de etiquetas combinando informacion multilingue
	 */
	private void labelAlignmentMeasures(Model modelo, String fileName){
		try {
			PrintStream informe = new PrintStream(new File(resultDir+File.separator+fileName+"_WnetMultiLingAlignAnalysis.txt"));
			//obtenemos el numero de conceptos del modelo
			informe.println("Total number of concepts: "+modelo.listSubjects().toList().size());informe.println();
		
			//contamos los alineamientos exactos
			List<Statement> exactMatchs = modelo.listStatements(null,exact_wordnetMatch,(RDFNode)null).toList();
			HashMap<String,Integer> contadorTipos = new HashMap<String,Integer>();
			for (Statement st:exactMatchs){
					String type = st.getLiteral().getDatatypeURI();
					if(!contadorTipos.containsKey(type)){
						contadorTipos.put(type, 0);
					}
					contadorTipos.put(type, contadorTipos.get(type)+1);
			}
			informe.println("Number of exact matches: "+exactMatchs.size());
			for(String ty:contadorTipos.keySet()){
				informe.println("--- of type "+ty+" : "+contadorTipos.get(ty));
			}
			
			//contamos los alineamientos isa
			List<Statement> isaMatchs = modelo.listStatements(null,isa_wordnetMatch,(RDFNode)null).toList();
			contadorTipos.clear();
			for (Statement st:isaMatchs){
					String type = st.getLiteral().getDatatypeURI();
					if(!contadorTipos.containsKey(type)){
						contadorTipos.put(type, 0);
					}
					contadorTipos.put(type, contadorTipos.get(type)+1);
			}
			informe.println("Number of isa matches: "+isaMatchs.size());
			for(String ty:contadorTipos.keySet()){
				informe.println("--- of type "+ty+" : "+contadorTipos.get(ty));
			}
			
			//contamos los no alineados
			Hashtable<String, Integer> errors = new Hashtable<String, Integer>();
			for(Statement st:modelo.listStatements(null,unalignable_wordnet,(RDFNode)null).toList()){
				String error = st.getString();
				if(!errors.containsKey(error)){
					errors.put(error, 0);
				}
				errors.put(error, errors.get(error)+1);
				
			}
			for(String err: errors.keySet()){
				informe.println("Number of "+err+" : "+errors.get(err));
			}
			
			//calculamos la media de synsets por concepto
			int contador =0;
			for(Statement st:modelo.listStatements(null,inner_possibleLabel_Synset,(RDFNode)null).toList()){
				contador+=st.getString().split("\\|").length;
			}
			informe.println("Media de synsets por concepto : "+ ((double)(contador+exactMatchs.size()+isaMatchs.size()))/modelo.listSubjects().toList().size());
			informe.close();
		}catch(Exception er){er.printStackTrace();}
	}
	
	/***************************************************************/
	/**
	 * guarda las medidas sobre el alineamiento de synsets en un idioma
	 */
	private void languageDependentAlignMeasures(String lang, List<Resource> concepts, String fileName){
		try {
			File resdir = new File(resultDir);resdir.mkdirs();
			PrintStream informe = new PrintStream(new File(resultDir+File.separator+fileName+"_WnetAlignAnalysis"+"_"+lang+".txt"));
		
			//obtenemos el numero de conceptos del modelo
			informe.println("Total number of concepts: "+concepts.size());informe.println();
			
			//obtenemos todos las etiquetas que están alineados con wordnet gracias al idioma indicado
			//y sus synsets	
			int labCount =0, conceptalignCount =0;
			HashMap<Resource,List<List<String>>> alineamientos = new HashMap<Resource,List<List<String>>>();
			for(Resource res :concepts){
				for (Statement st:res.listProperties(inner_conceptSynset).toList()){
					if(st.getLanguage().equals(lang)){
						String[] syn = st.getString().split("\\|");
						if(alineamientos.get(res)==null){
							alineamientos.put(res,new ArrayList<List<String>>());
							conceptalignCount++;
						}
						alineamientos.get(res).add(Arrays.asList(syn));
						labCount++;
					}
				}
			}
			informe.println("Total number of aligned concepts: "+conceptalignCount);informe.println();		
			informe.println("Total number of labels: "+labCount);informe.println();
			
			//obtenemos el numero de alineamientos directos, por tipo, y en total
			Collection<List<List<String>>> alignsysn = alineamientos.values();
			informe.println("Total number of alignmments: "+ countAlignments (alignsysn,-1,null));
			informe.println("  Number of original label alignmments: "+ countAlignments (alignsysn,-1,"O"));
			informe.println("  Number of singular label alignmments: "+ countAlignments (alignsysn,-1,"S"));
			informe.println("  Number of propositional label alignmments: "+ countAlignments (alignsysn,-1,"P"));
			informe.println("  Number of propositional singular label alignmments: "+ countAlignments (alignsysn,-1,"PS"));
			informe.println("  Number of noun label alignmments: "+ countAlignments (alignsysn,-1,"N"));
			informe.println("  Number of noun singular label alignmments: "+ countAlignments (alignsysn,-1,"NS"));
			informe.println();
			informe.println("Total number of 1 sense alignmments: "+ countAlignments (alignsysn,1,null));
			informe.println("  Number of 1 sense original label alignmments: "+ countAlignments (alignsysn,1,"O"));
			informe.println("  Number of 1 sense singular label alignmments: "+ countAlignments (alignsysn,1,"S"));
			informe.println("  Number of 1 sense propositional label alignmments: "+ countAlignments (alignsysn,1,"P"));
			informe.println("  Number of 1 sense propositional singular label alignmments: "+ countAlignments (alignsysn,1,"PS"));
			informe.println("  Number of 1 sense noun label alignmments: "+ countAlignments (alignsysn,1,"N"));
			informe.println("  Number of 1 sense noun singular label alignmments: "+ countAlignments (alignsysn,1,"NS"));
			informe.println();
			informe.println("  Mean of synsets per concept : "+ ((double)countSynsets (alignsysn))/concepts.size());
		
			informe.close();
		} catch (FileNotFoundException e) {e.printStackTrace();}
	}
	
	//contamos fases proposicionales inglesas y numero de ajetivos
	//de etiquetas preferidas y alternativas
	
	private String englishPosModel ="data/input/opennlp/opennlp-en-pos-maxent.bin";
	private String englishPosPreposName ="IN";
	private String spanishPosModel ="data/input/opennlp/opennlp-es-pos-maxent-pos-universal-cavorite.model";
	private String frenchPosModel ="data/input/opennlp/opennlp-fr-pos-nicolas-hernandez.bin";
	
	/**
	 * Obtenemos todas las etiquetas de un tipo en un idioma
	 */
	private List<String> getLabels(List<Resource> concepts, Property prop, String lang){
		List<String> result = new ArrayList<String>();
		for(Resource res :concepts){	
			for (Statement st:res.listProperties(prop).toList()){		
				//separamos las etiquetas de todos los conceptos por idimoa
				String label = preprocessLabels(st.getString());
				if(st.getLanguage().equals(lang) && label != null && label.length()>0){
					result.add(label);
				}
			}
		}
		
	    return result;
	}
	
	/**
	 * obtenemos los singulares y plurales de las etiquetas de un idioma
	 */
	private List<String> getSingularLabels(List<String> lab, String lang,
			POSTaggerME englishPOSTag,POSTaggerME spanishPOSTag,POSTaggerME frenchPOSTag){
		List<String> singular = new ArrayList<String>();
		for(String st:lab){
			String label = removePropositionalText(st,lang,englishPOSTag,spanishPOSTag,frenchPOSTag);
			if(!label.endsWith("s")){
				singular.add(st);	
			}
		}
		return singular;
	}
	
	/**
	 * frases proposicionales en ingles
	 */
	private List<String> getPropPhrase(List<String> labels, POSTaggerME tagger, String propTag){
		List<String> prop = new ArrayList<String>();
		for(String label:labels){
			String[] tokens = SimpleTokenizer.INSTANCE.tokenize(label);
			String[] POStokens = tagger.tag(tokens);
			Set<String> pos = new HashSet<String>(Arrays.asList(POStokens));
			if(pos.contains(propTag)){
				prop.add(label);
			}
		}	
		return prop;
	}
	
	/**
	 * incica si la frase es compleja o no
	 */
	private List<String> getComplexPhrase(List<String> labels, POSTaggerME tagger){
		//en ("JJ|JJR|JJS|NN|NNP|NNPS|NNS")){ //es ("AO|AQ|NC|NP")){ //fr ("ADJ|NC|NPP")){
		List<String> prop = new ArrayList<String>();
		for(String label:labels){
			String[] tokens = SimpleTokenizer.INSTANCE.tokenize(label);
			String[] POStokens = tagger.tag(tokens);
			Set<String> pos = new HashSet<String>(Arrays.asList(POStokens));
			if(POStokens.length>4||pos.contains("CC")){
				prop.add(label);
			}
		}
		return prop;
	}
	
	/**
	 * incica si la frase tiene adverbios
	 */
	private List<String> getAdvPhrase(List<String> labels, POSTaggerME tagger){
		String [] advTags = {"RB","RBS","RBR","DT","RG","RN","DT","ADV","DET"};
		List<String> advTagsL = Arrays.asList(advTags);
		List<String> prop = new ArrayList<String>();
		for(String label:labels){
			String[] tokens = SimpleTokenizer.INSTANCE.tokenize(label);
			String[] POStokens = tagger.tag(tokens);
			Set<String> pos = new HashSet<String>(Arrays.asList(POStokens));
			pos.retainAll(advTagsL);
			if(pos.size()>0){
				prop.add(label);
			}
		}
		return prop;
	}
	
	/**
	 * obtenemos el número de relaciones RT
	 */
	private int getRelatedRelations(Model modelo){
		//obtenemos las relaciones del tipo RT
		return  modelo.listStatements(null, skosRelatedProp, (RDFNode)null).toList().size();
		
	}
	/**
	 * obtenemos el número de conceptos huérfanos
	 */
	
		private int getOrphanConcepts(Resource esquema, List<Resource> concepts ) {
		
		int result = 0;
		
		List<Statement> var= esquema.listProperties(ThesFormalizerRDFPropertyManager.skosHasTopConcProp).toList();
		List<Resource> TopConcept= new ArrayList<Resource>();;
		
		for(Statement st:var){
			
			TopConcept.add(st.getResource());			
		}
		
        for(Resource res:concepts){
			
			if(!TopConcept.contains(res) && !res.hasProperty(skosBroaderProp)){
				result ++;
				
			}			
		}
			  
		return result;
	}
	
	/**
	 * obtenemos el número de raíces
	 */
	
	private int gethasTopConc(Model modelo){
		
		return  modelo.listStatements(null, skosHasTopConcProp, (RDFNode)null).toList().size();
		
	}
	
	/**
	 * obtenemos el número de mayúsculas
	 */
	
	private List<String> getmayusculas(List<Resource> concepts, Property prop, String lang){
		List<String> result = new ArrayList<String>();
		
		Pattern p=Pattern.compile("^[A-Z].+");
		for(Resource res :concepts){	
			for (Statement st:res.listProperties(prop).toList()){	
				
				//separamos las etiquetas de todos los conceptos por idimoa
				 
				String label = st.getString();
				if(st.getLanguage().equals(lang) && label != null && label.length()>0){					
					Matcher map=p.matcher(label);
					boolean isMatched = map.matches();
				    if (isMatched)
				    {
				      result.add(label);
				    }				  	
				}
			}
		}		
	    return result;
	}
	
	/**
	 * obtenemos el número de acrónimos
	 */
	
     private int getacronimos(List<Resource> concepts, Property prop, String lang){		
		
		Pattern p=Pattern.compile("[A-Z]{2}.+");
		//List<String> result = new ArrayList<String>();
		int result=0;
		for(Resource res :concepts){	
			for (Statement st:res.listProperties(prop).toList()){
				
				//separamos las etiquetas de todos los conceptos por idioma				 
				String label = st.getString();
				if(st.getLanguage().equals(lang) && label.length()>0){
				    Matcher map=p.matcher(label);
				    boolean isMatched = map.matches();
				    if (isMatched) {
				    	result ++;
				    }				  				
				}
			}
		}		
	    return result;
	   }
	
     /**
 	 * obtenemos el número de carácteres no alfabéticos.
 	 */
 	
     private List<String> getcaracteresalfa(List<Resource> concepts, Property prop, String lang){
 		 		
 		Pattern p=Pattern.compile("[a-zA-ZáéíóúäëïöüàèìòùÁÉÍÓÚÀÈÌÒÙÄËÏÖÜ\\(\\) ]+");
 		List<String> result = new ArrayList<String>();
 		for(Resource res :concepts){	
 			for (Statement st:res.listProperties(prop).toList()){	
 				
 				//separamos las etiquetas de todos los conceptos por idimoa
 				 
 				String label = st.getString();
 				if(st.getLanguage().equals(lang) && label.length()>0){
 				    Matcher map=p.matcher(label);
 				    boolean isMatched = map.matches();
 				    if (!isMatched){
 				    	result.add(label);
 				    } 				  				
 				}
 			}
 		} 		
 	      return result;
 	  }
     
     private int[] cantRelation(List<Resource> concepts){
 		int result []= new int [2];
 		
 		for(Resource res:concepts){
 			for(Statement st:res.listProperties(skosBroaderProp).toList()){
 				Resource related = st.getResource();
 				for (Statement sr: res.getModel().listStatements(res, null, related).toList()){
 					Property prop = sr.getPredicate();
 					if (!prop.equals(skosNarrowerProp) && !prop.equals(skosBroaderProp) && !prop.equals(skosRelatedProp)  ){
 						result[0]++;
 						String drel = getIdPartOfUri(prop.getURI());
 						
 						
 						if(drel.equals("hasSubclass")||drel.equals("exact-location")||
 						   drel.equals("exact-location-of")||drel.equals("participant")||
 						   drel.equals("p-spatial-location")||drel.equals("p-spatial-location-of")||
 						   drel.equals("spatial-location")||drel.equals("e-temporal-location-of")||
 						   drel.equals("e-temporal-location")||	drel.equals("temporal-relation")||
 						   drel.equals("q-location")){
 							
 							result[1] ++;
 						}
 					}		
 		   	   }
 			}
 	     }	
 		return result;
 	 }
 	  
 	/**************************************************************************/
 	/**
 	 * obtenemos la parte identificativa de una uri
 	 */
 	private String getIdPartOfUri(String uri){
 		if(uri==null) return null;
 		String[] res = uri.split("/|#");
 		return res[res.length-1];
 	}
     
        		
		private void simpleQualityMeasures(List<Resource> concepts, String fileName,Resource esquema){
		try{
			
		    PrintStream informe = new PrintStream(new File(resultDir+File.separator+fileName+"_SimpleQualityMeasures"+".txt"));
						
			POSTaggerME englishPOSTag =  new POSTaggerME(new POSModel(new FileInputStream(englishPosModel)));
			POSTaggerME spanishPOSTag =  new POSTaggerME(new POSModel(new FileInputStream(spanishPosModel)));
			POSTaggerME frenchPOSTag =  new POSTaggerME(new POSModel(new FileInputStream(frenchPosModel)));
												
			//obtenemos todas las definiciones
			System.out.println("getLabels");
			List<String>langDEn =getLabels(concepts,skosDefinitionProp,"en");
			List<String>langDEs =getLabels(concepts,skosDefinitionProp,"es");
			List<String>langDFr =getLabels(concepts,skosDefinitionProp,"fr");
			
			//mostramos el número de definiciones por idioma
			informe.println("-----------------------------------------");
			informe.println("Número de definiciones:");
			informe.println("en:"+(langDEn.size()));
			informe.println("es:"+(langDEs.size()));
			informe.println("fr:"+(langDFr.size()));
			
			//mostramos el número de conceptos
			informe.println("-----------------------------------------");
			informe.println("Número total de conceptos:"+concepts.size());
						
			//obtenemos todas las etiquetas preferida
			List<String>langPEn =getLabels(concepts,skosPrefLabelProp,"en");
			List<String>langPEs =getLabels(concepts,skosPrefLabelProp,"es");
			List<String>langPFr =getLabels(concepts,skosPrefLabelProp,"fr");
			
			//mostramos el número de etiquetas preferidas por idioma
			informe.println("-----------------------------------------");
			informe.println("Número de etiquetas preferidas:");
			informe.println("en:"+(langPEn.size()));
			informe.println("es:"+(langPEs.size()));
			informe.println("fr:"+(langPFr.size()));
		
			//obtenemos todas las etiquetas alternativas
			List<String>langAEn =getLabels(concepts,skosAltLabelProp,"en");
			List<String>langAEs =getLabels(concepts,skosAltLabelProp,"es");
			List<String>langAFr =getLabels(concepts,skosAltLabelProp,"fr");
			
			//mostramos el número de etiquetas alternativas por idioma
			informe.println("-----------------------------------------");
			informe.println("Número de etiquetas alternativas:");
			informe.println("en:"+(langAEn.size()));
			informe.println("es:"+(langAEs.size()));
			informe.println("fr:"+(langAFr.size()));
			
			//obtenemos número de carácteres no alfabéticos.
			System.out.println("getcaracteresalfa");
			List<String>caractPEn =getcaracteresalfa(concepts,skosPrefLabelProp,"en");
			List<String>caractPEs =getcaracteresalfa(concepts,skosPrefLabelProp,"es");
			List<String>caractPFr =getcaracteresalfa(concepts,skosPrefLabelProp,"fr");
			
			//mostramos el número de etiquetas caracteres no alfabéticos.
		    informe.println("-----------------------------------------");
			informe.println("Número de etiquetas con caracteres no alfabéticos:");
			informe.println("en:"+ (caractPEn.size()));
			informe.println("es:"+ (caractPEs.size()));
			informe.println("fr:"+ (caractPFr.size()));
			
			//union de las etiquetas en cada idioma
			List<String>langEn = new ArrayList<String>(langPEn); langEn.addAll(langAEn);
			List<String>langEs = new ArrayList<String>(langPEs); langEs.addAll(langAEs);
			List<String>langFr = new ArrayList<String>(langPFr); langFr.addAll(langAFr);
			
			//mostramos el número de etiquetas preferidas duplicadas
			informe.println("-----------------------------------------");
			informe.println("Número de etiquetas preferidas duplicadas");
			informe.println("en:"+(langPEn.size()-(new HashSet<String>(langPEn)).size()));
			informe.println("es:"+(langPEs.size()-(new HashSet<String>(langPEs)).size()));
			informe.println("fr:"+(langPFr.size()-(new HashSet<String>(langPFr)).size()));	
			
            //obtenemos etiquetas preferidas que comienzan con mayúsculas
			System.out.println("getmayusculas");
			List<String>mayPEn =getmayusculas(concepts,skosPrefLabelProp,"en");
			List<String>mayPEs =getmayusculas(concepts,skosPrefLabelProp,"es");
			List<String>mayPFr =getmayusculas(concepts,skosPrefLabelProp,"fr");
			
            //obtenemos etiquetas preferidas que comienzan con mayúsculas
			
			List<String>mayAEn =getmayusculas(concepts,skosAltLabelProp,"en");
			List<String>mayAEs =getmayusculas(concepts,skosAltLabelProp,"es");
			List<String>mayAFr =getmayusculas(concepts,skosAltLabelProp,"fr");
			
			//mostramos el número de etiqutas preferidas q comienzan con mayúsculas por idioma.
			informe.println("-----------------------------------------");
			informe.println("Número de etiquetas preferidas que comienzan con mayúscula:");
			informe.println("en:"+(mayPEn.size()));
			informe.println("es:"+(mayPEs.size()));
			informe.println("fr:"+(mayPFr.size()));
			
			//mostramos el número de etiqutas alternativas q comienzan con mayúsculas por idioma.
			informe.println("-----------------------------------------");
			informe.println("Número de etiquetas alternativas que comienzan con mayúscula:");
			informe.println("en:"+(mayAEn.size()));
			informe.println("es:"+(mayAEs.size()));
			informe.println("fr:"+(mayAFr.size()));
			
            //obtenemos número de acrónimos 
			System.out.println("getacronimos");
			int acronPEn =getacronimos(concepts,skosPrefLabelProp,"en");
			int acronPEs =getacronimos(concepts,skosPrefLabelProp,"es");
			int acronPFr =getacronimos(concepts,skosPrefLabelProp,"fr");
			
			//mostramos número de acrónimos
			informe.println("-----------------------------------------");
			informe.println("Número de acrónimos en etiquetas preferidas:");
			informe.println("en:"+acronPEn);
			informe.println("es:"+acronPEs);
			informe.println("fr:"+acronPFr);
			
				
			//obtenemos las etiquetas en singular de cada idioma
			System.out.println("getSingularLabels");
			List<String> singEn = getSingularLabels(langEn,"en",englishPOSTag,spanishPOSTag,frenchPOSTag);
			List<String> singEs = getSingularLabels(langEs,"es",englishPOSTag,spanishPOSTag,frenchPOSTag);
			List<String> singFr = getSingularLabels(langFr,"fr",englishPOSTag,spanishPOSTag,frenchPOSTag);
			
			//obtenemos las plurales
			List<String> plurEn = new ArrayList<String>(langEn); plurEn.removeAll(singEn);
			List<String> plurEs = new ArrayList<String>(langEs); plurEs.removeAll(singEs);
			List<String> plurFr = new ArrayList<String>(langFr); plurFr.removeAll(singFr);
			
			//mezclamos plurales y singulares de todos los idiomas
			List<String> singulars = new ArrayList<String>(singEn); singulars.addAll(singEs); singulars.addAll(singFr); 
			List<String> plurals = new ArrayList<String>(plurEn); plurals.addAll(plurEs); plurals.addAll(plurFr);
			
			//mostramos el numero de etiquetas en plural y singular
			informe.println("-----------------------------------------");
			informe.println("Número de etiquetas en plural y singular");
			informe.println("Plural: "+plurals.size());
			informe.println("Singular: "+singulars.size());
			for(String lab:plurals){informe.println("P- "+lab);}
			for(String lab:singulars){informe.println("S- "+lab);}
		
			//obtenemos las frases proposicionales (en ingles)
			System.out.println("getPropPhrase");
			List<String> propPhraseEn = getPropPhrase(langEn,englishPOSTag,englishPosPreposName);
			
			//mostramos las frases proposicionales
			informe.println("-----------------------------------------");
			informe.println("Número de etiquetas proposicionales:"+propPhraseEn.size());
			for(String lab:propPhraseEn){informe.println("Prop- "+lab);}
			
			//identificamos las etiquetas complejas para cada idioma
			List<String> complexEn = getComplexPhrase(langEn,englishPOSTag);
			List<String> complexEs = getComplexPhrase(langEs,spanishPOSTag);
			List<String> complexFr = getComplexPhrase(langFr,frenchPOSTag);
			List<String> complex = new ArrayList<String>(complexEn); complex.addAll(complexEs); complex.addAll(complexFr);
			
			//mostramos las frases complejas
			informe.println("-----------------------------------------");
			informe.println("Número de etiquetas complejas:"+complex.size());
			for(String lab:complex){informe.println("Compl- "+lab);}
			
			//identificamos las frases con adverbios 
			System.out.println("getAdvPhrase");
			List<String> advEn = getAdvPhrase(langEn,englishPOSTag);
			List<String> advEs = getAdvPhrase(langEs,spanishPOSTag);
			List<String> advFr = getAdvPhrase(langFr,frenchPOSTag);
			List<String> adv = new ArrayList<String>(advEn); complex.addAll(advEs); complex.addAll(advFr);
			
			//mostramos las frases con adverbios
			informe.println("-----------------------------------------");
			informe.println("Número de etiquetas con adverbios:"+adv.size());
			for(String lab:adv){informe.println("Adv- "+lab);}
			
            //Obtenemos el número de RT		
			System.out.println("getRelatedRelations");
			Model modelo= JenaModelManager.loadJenaModel(fileToAnalize);
			int numRT= getRelatedRelations(modelo);
			
			//mostramos número de relaciones RT
			informe.println("-----------------------------------------");
			informe.println("Número total de relaciones RT: " +numRT);
			
			//Obtenemos el número de conceptos huérfanos
			System.out.println("getOrphanConcepts");  			 
			int numOC= getOrphanConcepts(esquema,concepts);
							
			//mostramos el número total de conceptos huérfanos es:
			informe.println("-----------------------------------------");
			informe.println("Número total de conceptos huérfanos: " +numOC);
			
			//Obtenemos el número de raíces
			System.out.println("gethasTopConc");
			int numhasTop= gethasTopConc(modelo);
			
			//mostramos el número total de raíces
			informe.println("-----------------------------------------");
			informe.println("Número total de raíces: " +numhasTop);
			
            //Obtener el número de relaciones RT incorrectas	
			System.out.println("detectIncorrectRTs");
			int relatedsN= detectIncorrectRTs(concepts);
			
			//mostramos número de relaciones RT incorrectas
			informe.println("-----------------------------------------");
			informe.println("Número de relaciones RT incorrectas: " +relatedsN);
						  			
			//obtenemos el número de ciclos
			System.out.println("detectIncorrectHierarchy");
			int numCiclo= detectCicles(concepts);
			
			//mostramos el número de ciclos
			informe.println("-----------------------------------------");
			informe.println("Número de ciclos: " +numCiclo);
			
			//obtenemos el número de BT/NT incorrectas
			System.out.println("cantRelation");
			int [] resultado= cantRelation(concepts);
			
			//Mostrar Cantidad BT/NT incorrecta 
			
			informe.println("-----------------------------------------");
			informe.println("Número de BT/NT Correctness: " +resultado[0]);
			informe.println("Número de BT/NT Correctness incorrectas: " +resultado[1]);
						      					
			informe.close();
			
		}catch(Exception er){
			er.printStackTrace();
		}
	}
	
	private String removePropositionalText(String text, String lang, POSTaggerME englishPOSTag, POSTaggerME spanishPOSTag,POSTaggerME frenchPOSTag ){
		try {
			String[] tokens = SimpleTokenizer.INSTANCE.tokenize(text);
			String[] POStokens;
			if(lang.equals("en")){
				POStokens = englishPOSTag.tag(tokens);
			}else if(lang.equals("es")){
				POStokens = spanishPOSTag.tag(tokens);
			}else {
				POStokens = frenchPOSTag.tag(tokens);
			}
			int i=0;
			for(;i < POStokens.length;i++){
				if(POStokens[i].matches("IN|SP|P|P+D")){
					break;
				}
			}
			if(i<POStokens.length){
				String res ="";
				for(int k=0;k<i;k++){
					res+=" "+tokens[k];
				}
				return res;
			}
			
			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return text;
	}
	
	
	private String preprocessLabels(String original) {
		String res = original;
		//si no hay cadena de texto
		if(original==null){return null;}
		//si la etiqueta son siglas la borramos
		if(StringUtils.isAllUpperCase(res)){return null;}
		//borra parentesis
		if (res.contains("(")){res = res.substring(0, res.indexOf("("));}
		//borrar igual
		if (res.contains("=")){res = res.substring(0, res.indexOf("="));}
		//borra otros simbolos
		res = res.replace("%", "");
		res = res.replace("-", " ");	
		return res.trim().toLowerCase();
	}
	
	
	
	/************************************************************************/
	/**
	 * contamos el numero de ocurrencias de cierto tipo de alineamiento
	 */
	public int countAlignments(Collection<List<List<String>>> alineamientos, int numAlign, String type){
		int count =0;
		for(List<List<String>> synset : alineamientos){
			for(List<String> ls: synset){
				if((ls.size()== numAlign+1 || numAlign==-1)&& (type == null || type.equals(ls.get(0)))){
					count++;
				}
			}
		}
		return count;
	}
	
	/*************************************************************************/
	/**
	 * contamos el numero total de synsets entre todos los cocneptos del idioma
	 * es una medida del grado de polisemia
	 */
	public int countSynsets(Collection<List<List<String>>> alineamientos){
		int count =0;
		for(List<List<String>> synset : alineamientos){
			for(List<String> ls: synset){
					count+=ls.size()-1;
			}
		}
		return count;
	}
	
	
	/*******************************************************************************/
	/*******************************************************************************/
	/**
	 * guardamos todos los emparejamientos directos de cada idioma ordenados por tipo
	 */
	public void storeAlignments(String lang, List<Resource> concepts, boolean direct, String fileName){
		try {
			String fname =null;
			if (direct){
				fname=resultDir+File.separator+fileName+"_singleWnetAlignments_"+lang+".txt";
			}else{
				fname=resultDir+File.separator+fileName+"_multipleWnetAlignments_"+lang+".txt";
			}
			PrintStream informe = new PrintStream(new File(fname));
		
			//obtenemos todos los conceptos que están alineados con wordnet gracias al idioma indicado
			//y sus synsets	
			HashMap<Resource,List<List<String>>> alineamientos = new HashMap<Resource,List<List<String>>>();
			for(Resource res :concepts){
				for (Statement st:res.listProperties(inner_conceptSynset).toList()){
					if(st.getLanguage().equals(lang)){
						String[] syn = st.getString().split("\\|");
						if(alineamientos.get(res)==null){
							alineamientos.put(res,new ArrayList<List<String>>());							
						}
						alineamientos.get(res).add(Arrays.asList(syn));						
					}
				}
			}
			
			//escribimos en el fichero toda la información del synset 
			saveSynsetsInfo(informe,alineamientos,direct);
			
			
			informe.close();
		} catch (FileNotFoundException e) {e.printStackTrace();}
	
	}
	
	/******************************************************************************/
	/**
	 * guarda aquellos conceptos que no han sido desambiguados
	 * @param modelo
	 * @param fileName
	 */
	private void storeUndisambiguatedAligns(Model modelo, String fileName){
		try {
			PrintStream informe = new PrintStream(resultDir+File.separator+fileName+"_undisambiguatedAlignments.txt");
			List<Statement> undisamb = modelo.listStatements(null,inner_possibleLabel_Synset,(RDFNode)null).toList();
			for(Statement sti:undisamb){
				Resource concept = sti.getSubject();
				//escribimos las labels del concepto en cada idioma
				for(Statement st: concept.listProperties(inner_conceptNoun).toList()){
					informe.println("Lang: "+st.getLanguage()+" Label: "+st.getString());
				}
				informe.println();
				
				//escribimos los synsets que no se sabe cual de los sentidos
				informe.println("UndesambiguatedSynsets:");
				String synse = sti.getString();
				List<String> syns = new ArrayList<String>(Arrays.asList(synse.split("\\|")));
				syns.add(0,"-");
				showSynsetDefinition(syns,informe);
				informe.println();
				
				//escribimos los synsets originales del concepto en cada idioma
				for(Statement st: concept.listProperties(inner_conceptSynset).toList()){
					String synsets = st.getString();
					informe.println("Lang: "+st.getLanguage()+" LabMatchPosSyns: "+synsets);
					showSynsetDefinition(Arrays.asList(synsets.split("\\|")),informe);
					informe.println();
				}
				informe.println("----------------------------------------------------------");						
			}
		
		} catch (Exception e) {e.printStackTrace();}
		
	}
	
	
	/******************************************************************************/
	/**
	 * guarda en un fichero los conceptos cuyos alineamientos son conflictivos
	 * o no hay alineamiento
	 */
	private void storeConflictAligns(Model modelo, String fileName){
		try {
			PrintStream informe = new PrintStream(resultDir+File.separator+fileName+"_conflictAlignments.txt");
			List<Statement> unalignable = modelo.listStatements(null,unalignable_wordnet,(RDFNode)null).toList();
			
			//obtenemos los tipos de no alineamiento diferentes
			Set<String>unalignType = new HashSet<String>();
			for (Statement s:unalignable){
				unalignType.add(s.getString());
			}
			
			//para cada tipo de problema hacemos un resumen
			for(String errT:unalignType){
				informe.println("----------------------------------------------------------");
				informe.println(errT);
				informe.println("----------------------------------------------------------");
				informe.println("----------------------------------------------------------");
				for (Statement s:unalignable){
					unalignType.add(s.getString());
					if(s.getString().equals(errT)){
						Resource concept = s.getSubject();
						//escribimos las labels del concepto en cada idioma
						for(Statement st: concept.listProperties(inner_conceptNoun).toList()){
							informe.println("Lang: "+st.getLanguage()+" Label: "+st.getString());
						}
						informe.println();
						//escribimos los synsets del concepto en cada idioma
						for(Statement st: concept.listProperties(inner_conceptSynset).toList()){
							String synsets = st.getString();
							informe.println("Lang: "+st.getLanguage()+" LabMatchPosSyns: "+synsets);
							showSynsetDefinition(Arrays.asList(synsets.split("\\|")),informe);
							informe.println();
						}
						informe.println("----------------------------------------------------------");						
			}	}	}
			informe.close();
		} catch (FileNotFoundException e) {e.printStackTrace();}
	}
	
	/*******************************************************************************/
	/**
	 * guardamos la información de los synsets extraidos de un tipo de etiquetas
	 */
	public void saveSynsetsInfo(PrintStream informe,HashMap<Resource,List<List<String>>> alineamientos, boolean direct){
		for (Resource res: alineamientos.keySet()){
			List<List<String>> lsynsets = alineamientos.get(res);
			boolean isOfType=false;
			if((direct && lsynsets.size()==1 && lsynsets.get(0).size()==2)
				|| (!direct && (lsynsets.size()>1 || lsynsets.get(0).size()>2))){	
				isOfType=true;
			}
			
			if(isOfType){
				//mostramos las etiquetas preferidas y alternativas
				for (Statement st2: res.listProperties(skosPrefLabelProp).toList()){
					informe.print(" | "+st2.getLanguage()+": "+st2.getString());							
				}
				informe.println();
				for (Statement st2: res.listProperties(skosAltLabelProp).toList()){
					informe.print(" | "+st2.getLanguage()+": "+st2.getString());							
				}
				informe.println();			
			
				for(List<String> synsets:lsynsets){
					showSynsetDefinition(synsets,informe);
				}
					
				informe.println("----------------------------------------------");				
			}
		}
	}
	
	/********************************************************************/
	/**
	 * guarda la definicion de los synsets en un printstream
	 */
	private void showSynsetDefinition(List<String> synsets, PrintStream informe){	
		informe.println(">>>> "+synsets.get(0));
		
		//mostramos las definiciones de los synsets
		for(int i=1; i<synsets.size();i++){
			Synset s= jwnl.getSynset(Integer.parseInt(synsets.get(i)));
			for (Word w: s.getWords()){
				informe.println(w.toString());
			}
			informe.println(s.getGloss());
		}
	}
	
	/*******************************************************/
	/**
	 * detectamos si hay ciclos en las relaciones nt
	 */
	/*private int detectIncorrectHierarchy(List<Resource> concepts){
		int numCiclo=0;
		Set<Resource> procesed = new HashSet<Resource>();
		for (Resource res: concepts){
			if(!procesed.contains(res)){
				procesed.add(res);
				Set<Resource> inHierarchy = new HashSet<Resource>();
				inHierarchy.add(res);
				boolean correct =true;
				for(Statement narrow: res.listProperties(skosNarrowerProp).toList()){
				    correct = correct && detectCorrectHierarchy (narrow.getResource(),new HashSet<Resource>(inHierarchy), procesed );					
				}				
				if(!correct){
					System.out.println("ciclo detectado:");
					printNarrows(res);
					numCiclo ++;
				}			
			}
		}
		
		return numCiclo;
	}*/
	/*private boolean detectCorrectHierarchy(Resource res, Set<Resource> inHierarchy, Set<Resource> procesed){
		if(inHierarchy.contains(res)){
			return false;
		}
		if(procesed.contains(res)){
			return true;
		}
		inHierarchy.add(res);
		boolean correct =true;
		for(Statement narrow: res.listProperties(skosNarrowerProp).toList()){
			correct = correct && detectCorrectHierarchy (narrow.getResource(),new HashSet<Resource>(inHierarchy), procesed);
		}
		return correct;
	}
	
	private void printNarrows (Resource res){
		System.out.println(res.getURI());
		for(Statement narrow: res.listProperties(skosNarrowerProp).toList()){
			printNarrows(narrow.getResource());
		}
	}*/
	
	
	/*******************************************************/
	/**
	 * detectamos ciclos
	 */
	Set<String> conceptsInCycle = new HashSet<String>();
	private int detectCicles(List<Resource> concepts){
		int cicles=0;
		for (Resource res: concepts){
			Set<String> uris = new HashSet<String>();
			uris.add(res.getURI());
			cicles+=detectCicles(res, uris, skosBroaderProp);
			cicles+=detectCicles(res, uris, skosNarrowerProp);
		}
		return cicles;
	}
	
	private int detectCicles(Resource res, Set<String> urisOr, Property prop){	
		int cicles=0;
		for(Statement bders: res.listProperties(prop).toList()){
			Set<String> uris = new HashSet<String>(urisOr);
			if(!uris.contains(bders.getResource().getURI())){				
				uris.add(bders.getResource().getURI());
				detectCicles(bders.getResource(), uris, prop);
			}else{				
				cicles++;					
				conceptsInCycle.addAll(uris);			
			}			
		}
		return cicles;
	}
	
	
	/*******************************************************/
	/**
	 * detectamos si las rts apuntan a padres, hijos o hermanos
	 */
	private int detectIncorrectRTs(List<Resource> concepts){
		int relatedsN=0;
		
		for (Resource res: concepts){
			Set<Resource> broaders = new HashSet<Resource>();
			Set<Resource> narrowers = new HashSet<Resource>();
			Set<Resource> brothers = new HashSet<Resource>();
			
			Queue<Resource>elements = new LinkedList<Resource>();
			elements.add(res);
			Set<String> uris = new HashSet<String>();
			uris.add(res.getURI());
			
			//System.out.println("broaders "+ res.getURI());
			while(elements.size()>0){
				Resource e = elements.poll();
				broaders.add(e);
				for(Statement bders: e.listProperties(skosBroaderProp).toList()){
					if(!uris.contains(bders.getResource().getURI())){
						elements.add(bders.getResource());
						uris.add(bders.getResource().getURI());
					}
				}	
			}
			
			//System.out.println("narrowerds");
			elements.add(res);
			uris = new HashSet<String>();
			uris.add(res.getURI());
			while(elements.size()>0){
				Resource e = elements.poll();
				narrowers.add(e);
				for(Statement bders: e.listProperties(skosNarrowerProp).toList()){
					if(!uris.contains(bders.getResource().getURI())){
						elements.add(bders.getResource());
						uris.add(bders.getResource().getURI());
					}
				}	
			}
			
			//System.out.println("brothers");
			for(Statement bders: res.listProperties(skosBroaderProp).toList()){
				elements.add(bders.getResource());
			}
			while(elements.size()>0){
				for(Statement bders: elements.poll().listProperties(skosNarrowerProp).toList()){
					brothers.add(bders.getResource());
				}
			}
			
			//System.out.println("analisis");
			for(Statement related: res.listProperties(skosRelatedProp).toList()){
				
		    	Resource rel = related.getResource();
		    	if(broaders.contains(rel)||narrowers.contains(rel)||brothers.contains(rel))
		    	{
		    		//System.out.println("incorrect related:"+getEnglishPrefLabel(res)+"--"+getEnglishPrefLabel(rel));
		    		   		
		    		relatedsN++;
		    	}
			}				
						
			
		}
		 //System.out.println("Total number of related"+relatedsN);
		 return relatedsN;
		 
		}
	
	
	
	
	
	/*private String getEnglishPrefLabel(Resource res){
		for(Statement related: res.listProperties(skosPrefLabelProp).toList()){
			if(related.getLanguage().equals("en")){
				return related.getString();
			}
		}
		return "";
	}*/
	
	/***********************************************************/
	/**
	 * obtiene los narrows transititvos de un recurso incluido el
	 */
	private Set<Resource> getNarrows(Resource urib, Model modelo){
		Set<Resource> resl = new HashSet<Resource>();
		if(resl.contains(urib)){
			System.err.println(urib);
		}
		resl.add(urib);
		for (Statement st: urib.listProperties(skosNarrowerProp).toList()){
			resl.addAll(getNarrows(st.getResource(),modelo));
		}
		return resl;	
	}
	

	/*******************************************************************************/
	/**
	 * Propiedades del tasklet
	 */
	public void setFileToAnalize(String fileToAnalize) {this.fileToAnalize = fileToAnalize;}
	public void setLangsToAnalyze(String[] langsToAnalyze) {this.langsToAnalyze = langsToAnalyze;}
	public void setResultDir(String resultDir) {this.resultDir = resultDir;}
}
