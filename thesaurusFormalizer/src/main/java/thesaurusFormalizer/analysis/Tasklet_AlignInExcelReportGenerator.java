package thesaurusFormalizer.analysis;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;
import rdfManager.JenaModelManager;
import thesaurusFormalizer.rdfManager.ThesFormalizerRDFPropertyManager;
import tools.wordnetManager.EnglishWordnetManager;

/**
 * genera un excel con la información de los alienamientos necesaria para poder
 * hacer un analisis de calidad
 */
public class Tasklet_AlignInExcelReportGenerator implements Tasklet{

	//propidades del tasklet
	private String fileToAnalize,dolceFile;
	private String resultFile;
	private List<String> branchsToDetail=new ArrayList<String>();
	
	//modelo acceso a wordnet
	private EnglishWordnetManager jwnl = new EnglishWordnetManager();
	
	//propiedades de rdf
	private static final Property skosPrefLabelProp = ThesFormalizerRDFPropertyManager.skosPrefLabelProp; 
	private static final Property exact_wordnetMatch = ThesFormalizerRDFPropertyManager.exact_wordnetMatch; 
	private static final Property isa_wordnetMatch = ThesFormalizerRDFPropertyManager.isa_wordnetMatch; 
	private static final Property rdfsSubclassOfProp = ThesFormalizerRDFPropertyManager.rdfsSubclassOfProp;
	private static final Property skosNarrowerProp = ThesFormalizerRDFPropertyManager.skosNarrowerProp;
	private static final Property skosBroaderProp = ThesFormalizerRDFPropertyManager.skosBroaderProp;
	private static final Property skosRelatedProp = ThesFormalizerRDFPropertyManager.skosRelatedProp;
	private static final Property inner_possibleLabel_Synset = ThesFormalizerRDFPropertyManager.inner_possibleLabel_Synset;
	private static final Property rdfsCommentProp = ThesFormalizerRDFPropertyManager.rdfsCommentProp;
		
	
	private Model dolceM ;
	
	/************************************************************************/
	/**
	 * analiza los alineamientos de un tesauro con wordnet
	 */
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		//cargamos el modelo de jena
		Model modelo = JenaModelManager.loadJenaModel(fileToAnalize);
		dolceM = JenaModelManager.loadJenaModel(dolceFile);
		
		//creamos el excel
		Workbook wb = new XSSFWorkbook();
	    
		
	    //lo rellenamos con la información de todos los conceptos
	    int i=0;
	    Sheet sheet = wb.createSheet("Aligns");
	    addHeader(sheet.createRow(i++));
	    for (Resource res: modelo.listSubjects().toList()){	    	
	    	addResourceInfoToExcel(res,sheet.createRow(i++));
	    }
	    	    
	    //creamos otra hoja con info de los conceptos de una rama solo
		int k =0;
	    for (String urib:branchsToDetail){
	    	i=0;
	    	sheet = wb.createSheet("branch_"+(++k));
	    	addHeader(sheet.createRow(i++));
	    	Set<Resource> narrows = getNarrows( modelo.getResource(urib),modelo);
	    	for(Resource res:narrows){		
		    	addResourceInfoToExcel(res,sheet.createRow(i++));
	    	}
	    }
	    
	   //creamos una hoja con los alineamientos de relaciones
	    i=0;
	    sheet = wb.createSheet("Relations");
	    addRelHeader(sheet.createRow(i++));
	    for (Resource res: modelo.listSubjects().toList()){	    	
	    	i=addRelationInfoToExcel(res,sheet,i);
	    }
	    
	  //creamos otra hoja con info de los conceptos de una rama solo
  		k =0;
  	    for (String urib:branchsToDetail){
  	    	i=0;
  	    	sheet = wb.createSheet("rbranch_"+(++k));
  	    	addRelHeader(sheet.createRow(i++));
  	    	Set<Resource> narrows = getNarrows( modelo.getResource(urib),modelo);
  	    	for(Resource res:narrows){		
  	    		i=addRelationInfoToExcel(res,sheet,i);
  	    	}
  	    }
	    
	    
	    //guardamos el excel
	    FileOutputStream fos = new FileOutputStream(resultFile);
	    wb.write(fos); wb.close(); fos.close();
	    
		return RepeatStatus.FINISHED;
	}
		
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
	
	/*********************************************************/
	/**
	 * añadimos el header a la hoja
	 */
	private void addHeader(Row row){
		row.createCell(0).setCellValue("Thes uri");
		row.createCell(1).setCellValue("Thes label");
		row.createCell(2).setCellValue("Thes label");
		row.createCell(3).setCellValue("Thes label");
		row.createCell(4).setCellValue("Type of rel with synset");
		row.createCell(5).setCellValue("Detailed type of rel with synset");
		row.createCell(6).setCellValue("Synset id");
		row.createCell(7).setCellValue("Synset text");
		row.createCell(8).setCellValue("Synset def");
		row.createCell(9).setCellValue("Dolce uri");
		row.createCell(10).setCellValue("Dolce noun");
	}
	
	/*********************************************************/
	/**
	 * añadimos el header a la hoja de relaciones
	 */
	private void addRelHeader(Row row){
		row.createCell(0).setCellValue("Source Concept");
		row.createCell(1).setCellValue("Thes label");
		row.createCell(2).setCellValue("Thes label");
		row.createCell(3).setCellValue("Thes label");
		row.createCell(4).setCellValue("Dolce SMap");
		row.createCell(5).setCellValue("Dolce SDef");
		row.createCell(6).setCellValue("Dest Concept");
		row.createCell(7).setCellValue("Thes label");
		row.createCell(8).setCellValue("Thes label");
		row.createCell(9).setCellValue("Thes label");
		row.createCell(10).setCellValue("Dolce DMap");
		row.createCell(11).setCellValue("Dolce DDef");
		row.createCell(12).setCellValue("BT/NT Rel");
		row.createCell(13).setCellValue("Dolce Rel");
		row.createCell(14).setCellValue("Dolce Rel Def");
		row.createCell(15).setCellValue("BT/NT Correctness");
	}
	
	
	/*********************************************************/
	/**
	 * Añade información de relaciones a la excel
	 */
	private int addRelationInfoToExcel(Resource res, Sheet sheet, int rowN){
		Set<String> procesados = new HashSet<String>();
		for(Statement st:res.listProperties(skosBroaderProp).toList()){
			Resource related = st.getResource();
			for (Statement sr: res.getModel().listStatements(res, null, related).toList()){
				Property prop = sr.getPredicate();
				if (!prop.equals(skosNarrowerProp) && !prop.equals(skosBroaderProp) && !prop.equals(skosRelatedProp)  ){
					procesados.add(res.getURI()+st.getResource().getURI());
					Row r = sheet.createRow(rowN++);
					r.createCell(0).setCellValue(getIdPartOfUri(res.getURI()));
					
					for(Statement sti:res.listProperties(skosPrefLabelProp).toList()){
						if(sti.getLanguage().equals("en")){
							r.createCell(1).setCellValue(sti.getString());
						}else if(sti.getLanguage().equals("es")){
							r.createCell(2).setCellValue(sti.getString());
						}else if(sti.getLanguage().equals("fr")){
							r.createCell(3).setCellValue(sti.getString());
						}
					}
					
					String uriDolce = getDolceSuperConcept(res);
					r.createCell(4).setCellValue(getIdPartOfUri(uriDolce));
					Statement sta = dolceM.getResource(uriDolce).getProperty(rdfsCommentProp);
					if(sta!=null){
						r.createCell(5).setCellValue(sta.getString());
					}
					r.createCell(6).setCellValue(getIdPartOfUri(sr.getResource().getURI()));
					 
					for(Statement sti:st.getResource().listProperties(skosPrefLabelProp).toList()){
						if(sti.getLanguage().equals("en")){
							r.createCell(7).setCellValue(sti.getString());
						}else if(sti.getLanguage().equals("es")){
							r.createCell(8).setCellValue(sti.getString());
						}else if(sti.getLanguage().equals("fr")){
							r.createCell(9).setCellValue(sti.getString());
						}
					}
					
					
					uriDolce = getDolceSuperConcept(sr.getResource());
					r.createCell(10).setCellValue(getIdPartOfUri(uriDolce));
					sta = dolceM.getResource(uriDolce).getProperty(rdfsCommentProp);
					if(sta!=null){
						r.createCell(11).setCellValue(sta.getString());
					}
					r.createCell(12).setCellValue(getIdPartOfUri(skosBroaderProp.getURI()));
					String drel = getIdPartOfUri(prop.getURI());
					r.createCell(13).setCellValue(drel);
					
					sta = dolceM.getResource(prop.getURI()).getProperty(rdfsCommentProp);
					if(sta!=null){
						r.createCell(14).setCellValue(sta.getString());
					}
					
					if(drel.equals("subClassOf")){
						r.createCell(15).setCellValue("is-a");
					}else if(drel.equals("participant-in")||
							drel.equals("q-location-of")){
						r.createCell(15).setCellValue("part-of");	
					}else if(drel.equals("approximate-location-of")||
							drel.equals("spatial-location-of")||
							drel.equals("material-place-of")||
							drel.equals("r-location-of")){	
						r.createCell(15).setCellValue("partOf/subClassOf");
					}else if(drel.equals("hasSubclass")||
							drel.equals("exact-location")||
							drel.equals("exact-location-of")||
							drel.equals("participant")||
							drel.equals("p-spatial-location")||
							drel.equals("p-spatial-location-of")||
							drel.equals("spatial-location")||
							drel.equals("e-temporal-location-of")||
							drel.equals("e-temporal-location")||
							drel.equals("temporal-relation")||
							drel.equals("q-location")){	
						r.createCell(15).setCellValue("incorrect");
					}else{
						r.createCell(15).setCellValue("unknown");
					}
					
					
				}
			}
		}
		
		for(Statement st:res.listProperties(skosBroaderProp).toList()){
			if(!procesados.contains(res.getURI()+st.getResource().getURI())){
				Row r = sheet.createRow(rowN++);
				r.createCell(0).setCellValue(getIdPartOfUri(res.getURI()));
				r.createCell(3).setCellValue(getIdPartOfUri(st.getResource().getURI()));
				
			}
			
		}
		
		
		for(Statement st:res.listProperties(skosRelatedProp).toList()){
			Resource related = st.getResource();
			for (Statement sr: res.getModel().listStatements(res, null, related).toList()){
				Property prop = sr.getPredicate();
				if (!prop.equals(skosNarrowerProp) && !prop.equals(skosBroaderProp) && !prop.equals(skosRelatedProp)  ){
					Row r = sheet.createRow(rowN++);
					r.createCell(0).setCellValue(getIdPartOfUri(res.getURI()));
					r.createCell(1).setCellValue(getDolceSuperConcept(res));
					r.createCell(2).setCellValue(getIdPartOfUri(sr.getResource().getURI()));
					r.createCell(3).setCellValue(getDolceSuperConcept(sr.getResource()));
					r.createCell(4).setCellValue(getIdPartOfUri(skosRelatedProp.getURI()));
					r.createCell(5).setCellValue(getIdPartOfUri(prop.getURI()));
				}
			}
		}
		return rowN;
	} 
	
	/***********************************************************************/
	/**
	 * returns the super Class of a concept (if is in Dolce), if there is not it return null
	 * 1 level, no transitive
	 */
	private String getDolceSuperConcept(Resource item){
		for (Statement st:item.listProperties(rdfsSubclassOfProp).toList()){
			if(st.getResource().isURIResource() && st.getResource().getURI().contains("www.loa-cnr.it")){
				return st.getResource().getURI();
			}
		}
		return null;
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
	
	
	/*********************************************************/
	/**
	 * añade la informaciónd e un recurso a la excel
	 */
	private void addResourceInfoToExcel(Resource res, Row row){
		//información del concepto
		String uri = res.getURI();
		String label = null; String label2 = null; String label3 = null;
		for(Statement st:res.listProperties(skosPrefLabelProp).toList()){
			if(st.getLanguage().equals("en")){
				label= st.getString();
			}else if(st.getLanguage().equals("es")){
				label2= st.getString();
			}else if(st.getLanguage().equals("fr")){
				label3= st.getString();
			}
		}
		
		//información del alineamiento con wordnet
		String tipoSynRel=null; String dettipoSynRel=null;
		String synId=null;
		if(res.hasProperty(isa_wordnetMatch)){
			tipoSynRel="isA";
			synId = res.getProperty(isa_wordnetMatch).getString();
			dettipoSynRel = res.getProperty(isa_wordnetMatch).getLiteral().getDatatypeURI();
		}else if(res.hasProperty(exact_wordnetMatch)){
			tipoSynRel="exact";
			synId = res.getProperty(exact_wordnetMatch).getString();
			dettipoSynRel = res.getProperty(exact_wordnetMatch).getLiteral().getDatatypeURI();
		}else if(res.hasProperty(inner_possibleLabel_Synset)){
			tipoSynRel="multiple";
		}else{
			tipoSynRel="no found";
		}
		String synWords ="", synDef=null;
		if(synId!=null){
			Synset syn = jwnl.getSynset(Long.parseLong(synId));			
			Word[] wd = syn.getWords();
			for(Word w:wd){
				synWords += w.getLemma()+" , ";
			}
			synDef = syn.getGloss();
		}
		
		//información del alineamiento con dolce
		String dolceUri =null;
		String dolceNoun=null;
		if(res.hasProperty(rdfsSubclassOfProp)){
			for(Statement st: res.listProperties(rdfsSubclassOfProp).toList()){
			    dolceUri =st.getResource().getURI();
				if (dolceUri.contains("DOLCE-Lite.owl")){
					dolceNoun = dolceUri.split("#")[1];
				}
			}			 
		}
		
		//añadimos toda la información a la excel
		row.createCell(0).setCellValue(uri);
		row.createCell(1).setCellValue(label);
		row.createCell(2).setCellValue(label2);
		row.createCell(3).setCellValue(label3);
		row.createCell(4).setCellValue(tipoSynRel);
		row.createCell(5).setCellValue(dettipoSynRel);
		row.createCell(6).setCellValue(synId);
		row.createCell(7).setCellValue(synWords);		
		row.createCell(8).setCellValue(synDef);
		row.createCell(9).setCellValue(dolceUri);
		row.createCell(10).setCellValue(dolceNoun);
		
	}

	/*******************************************************************************/
	/**
	 * Propiedades del tasklet
	 */
	public void setFileToAnalize(String fileToAnalize) {this.fileToAnalize = fileToAnalize;}
	public void setDolceFile(String dolceFile) {this.dolceFile = dolceFile;}
	
	public void setResultFile(String resultFile) {this.resultFile = resultFile;}
	public void setBranchsToDetail(List<String> branchsToDetail) {
		this.branchsToDetail = branchsToDetail;
	}
}
