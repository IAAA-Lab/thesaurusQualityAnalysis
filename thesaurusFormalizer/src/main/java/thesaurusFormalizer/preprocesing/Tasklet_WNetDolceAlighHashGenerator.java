package thesaurusFormalizer.preprocesing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import net.didion.jwnl.data.Synset;
import rdfManager.JenaModelManager;
import rdfManager.RDFPropertyManager;
import tools.wordnetManager.EnglishWordnetManager;

public class Tasklet_WNetDolceAlighHashGenerator implements Tasklet{

	//alineamiento de wonderWeb
	private String WNDolceAligment;
	
	//hash destino con el alineamiento preparado para su uso
	private String WNDolceHash;
	
	//propiedades de rdfs
	private static Property rdfsSubclassOfProp = RDFPropertyManager.rdfsSubclassOfProp;
	private static Property rdfsCommentProp = RDFPropertyManager.rdfsCommentProp;
	
	/************************************************************************/
	/**
	 * analiza los alineamientos de un tesauro con wordnet
	 */
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		//obtenemos todos los alineamientos definidos entre wordnet y dolce
		HashMap<String,String> wnDolce = new HashMap<String,String>();
		Model modelo=JenaModelManager.loadJenaModel(WNDolceAligment);
		for(Statement st:modelo.listStatements(null,rdfsSubclassOfProp,(RDFNode)null).toList()){
			String objectUri = st.getResource().getURI();
			String subjectUri = st.getSubject().getURI();
			if (objectUri.contains("DOLCE-Lite.owl") && !subjectUri.contains("DOLCE-Lite.owl")){
				wnDolce.put(subjectUri, objectUri);
			}
		}
			
		HashMap<String,String> wnDolceSynId = new HashMap<String,String>();
		//encontramos para cada uri de wordnet el synset al que hace referencia
		EnglishWordnetManager ewn = new EnglishWordnetManager();
		for(String wnuri :wnDolce.keySet()){					
			//generamos una definición de tamaño limitado apra facilitar el alineamiento
			//ya que han cambiado algo entre versiones
			String definition = modelo.getProperty(modelo.getResource(wnuri), rdfsCommentProp).getString();
			definition = definition.replaceAll("\"", "").replaceAll("\'", "").replaceAll(";", "").replaceAll(":", "");
			String inidef = definition.length()>20 ? definition.substring(0,20) : definition;
			String findef = definition.length()>20 ? definition.substring(definition.length()-20) : definition;
	
			//obtemenos la primera palabra de la uri
			String[] steps = wnuri.split("#")[1].split("__");
			String word = steps[0].toLowerCase();
			if (word.charAt(word.length()-1)>='0' && word.charAt(word.length()-1)<='9'){
				word = word.split("_")[0];
			}
			word=word.replace("_", " ");
			
			//buscamos la equivalencia entre wn 1 y wn 3 a traves de las definiciones del fichero de mapping
			List<Synset> ls = ewn.getSynsets(word);
			boolean found=false;
			for(Synset l:ls){
				String gloss = l.getGloss();
				gloss = gloss.replaceAll("\"", "").replaceAll("\'", "").replaceAll(";", "").replaceAll(":", "");
								
				if (gloss.contains(inidef)|| gloss.contains(findef)){
					wnDolceSynId.put(Long.toString(l.getOffset()), wnDolce.get(wnuri));
					found=true; break;
				}
			}
			
			//aquellos que no se han podido encontrar directamente emparejando las definiciones
			//los revisamos a mano y los asignamos manualmente
			
			if (wnuri.equals("http://www.loa-cnr.it/ontologies/OWN/OWN.owl#SATURATION_2")){
				wnDolceSynId.put("13925188", wnDolce.get(wnuri));found=true;	
			}else if (wnuri.equals("http://www.loa-cnr.it/ontologies/OWN/OWN.owl#SUBSTANCE")){
				wnDolceSynId.put("19613", wnDolce.get(wnuri));found=true;	
			}else if (wnuri.equals("http://www.loa-cnr.it/ontologies/OWN/OWN.owl#VACUUM__VACUITY_1")){
				wnDolceSynId.put("8653474", wnDolce.get(wnuri));found=true;	
			}else if (wnuri.equals("http://www.loa-cnr.it/ontologies/OWN/OWN.owl#PHYSICAL_PROPERTY")){
				wnDolceSynId.put("5009170", wnDolce.get(wnuri));found=true;	
			}else if (wnuri.equals("http://www.loa-cnr.it/ontologies/OWN/OWN.owl#TIME_PERIOD__PERIOD__PERIOD_OF_TIME__AMOUNT_OF_TIME")){
				wnDolceSynId.put("15113229", wnDolce.get(wnuri));found=true;	
			}else if (wnuri.equals("http://www.loa-cnr.it/ontologies/OWN/OWN.owl#MEASURE__MEASUREMENT")){
				wnDolceSynId.put("996969", wnDolce.get(wnuri));found=true;	
			}else if (wnuri.equals("http://www.loa-cnr.it/ontologies/OWN/OWN.owl#MEASURE__QUANTITY__AMOUNT__QUANTUM")){
				wnDolceSynId.put("33615", wnDolce.get(wnuri));found=true;	
			}else if (wnuri.equals("http://www.loa-cnr.it/ontologies/OWN/OWN.owl#MANNER__MODE__STYLE__WAY__FASHION")){
				wnDolceSynId.put("4928903", wnDolce.get(wnuri));found=true;	
			}else if (wnuri.equals("http://www.loa-cnr.it/ontologies/OWN/OWN.owl#SUBSTANCE__MATTER")){
				wnDolceSynId.put("20827", wnDolce.get(wnuri));found=true;	
			}
			
			//si no se encuentra alguno lo muestra para que podamos corregir el problema
			//debería ser cero, hay un problema a corregir si esto se ejecuta
			if(!found){ 
				System.out.println("---------------------------");
				System.out.println(wnuri);
				System.out.println(word);
				System.out.println(modelo.getProperty(modelo.getResource(wnuri), rdfsCommentProp).getString());
				for(Synset l:ls){
					System.out.println("--"+l.getOffset());
					System.out.println("--"+l.getGloss());
				}
			}
		}	
			
		//guardamos el hashtable
		try {
			File salidaF = new File(WNDolceHash); salidaF.getParentFile().mkdirs();
			ObjectOutputStream salida=new ObjectOutputStream(new FileOutputStream(salidaF));
			salida.writeObject(wnDolceSynId); salida.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return RepeatStatus.FINISHED;
	}
	
	/**************************************************************/
	/**
	 * propiedades del tasklet
	 */
	public void setWNDolceAligment(String wNDolceAligment) {
		WNDolceAligment = wNDolceAligment;
	}

	public void setWNDolceHash(String wNDolceHash) {
		WNDolceHash = wNDolceHash;
	}
	
}
