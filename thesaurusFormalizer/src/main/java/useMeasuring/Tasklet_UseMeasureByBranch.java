package useMeasuring;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import rdfManager.JenaModelManager;
import rdfManager.RDFPropertyManager;

/**
 * Mide el uso de un tesauro agregado por ramas
 * mide porcentaje de conceptos usados y porcentaje de veces usado
 * tambien se puede probar a desglosar entre medida general y por colecciones
 *
 */

public class Tasklet_UseMeasureByBranch implements Tasklet{
	//fichero con el tesauro, los usos del tesauro en europeana y el fichero de resultados
	private String thesaurusFile, thesaurusUseFile, colectionSizeFile, outputFile;
	private int analisisLevel=1;
	
	public static final String usesBaseUri = "http://www.iaaa.es/uses#";
	//numero de conceptos en una rama, numero de conceptos usados en una rama, numero de conceptos usados por una coleccion en una rama
	public static final Property numberOfTotalConceptsInBranchProp = RDFPropertyManager.tempModel.createProperty(usesBaseUri+"numberOfTotalConceptsInBranch");
	public static final Property numberOfUsedConceptInBranchProp = RDFPropertyManager.tempModel.createProperty(usesBaseUri+"numberOfUsedConceptInBranch");
	public static final Property numberOfUsedConceptInBranchInACollectionProp = RDFPropertyManager.tempModel.createProperty(usesBaseUri+"numberOfUsedConceptInBranchInACollection");
	
	//numero de usos de un concepto, y numero de usos de los conceptos en la rama del concepto (el incluido)
	public static final Property usesOfTheConceptInTotalProp = RDFPropertyManager.tempModel.createProperty(usesBaseUri+"usesOfTheConceptInTotal");
	public static final Property usesOfTheConceptsInBranchInTotalProp = RDFPropertyManager.tempModel.createProperty(usesBaseUri+"usesOfTheConceptsInBranchInTotal");
	
	//numero de usos de un concepto en una coleccion, y numero de usos de los cocneptos en la rama del concepto en una coleccion (el incluido)
	public static final Property usesOfTheConceptInACollectionProp = RDFPropertyManager.tempModel.createProperty(usesBaseUri+"usesOfTheConceptInACollection");
	public static final Property usesOfTheconceptsInBranchInACollectionProp = RDFPropertyManager.tempModel.createProperty(usesBaseUri+"usesOfTheconceptsInBranchInACollection");
	
	
	/************************************************************************/
	/**
	 * realixa un informe del uso el tesauo en el repositorio del que se le pasan los datos
	 */
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		//cargamos el tesauro
		Model thes = JenaModelManager.loadJenaModel(thesaurusFile);
		
		//cargamos el tamaño de las colecciones
		HashMap<String, String> tamanoColecciones = new HashMap<String, String>();
		FileInputStream excelFile = new FileInputStream(new File(colectionSizeFile));
        Workbook workbook= new XSSFWorkbook(excelFile);
        Sheet datatypeSheet = workbook.getSheetAt(0);	
        Iterator<Row> iterator = datatypeSheet.iterator(); iterator.next(); //nos saltamos la cabecera
        while (iterator.hasNext()) {
        	Row currentRow = iterator.next();
        	String col = currentRow.getCell(0).getStringCellValue();
        	int size = (int)currentRow.getCell(1).getNumericCellValue();
        	tamanoColecciones.put(col, Integer.toString(size));
        }
        workbook.close();
        
		//leemos los usos de conceptos del tesauro
		HashMap<String, List<UseOfConceptInData>> conUse= new HashMap<String, List<UseOfConceptInData>>();
		excelFile = new FileInputStream(new File(thesaurusUseFile));
        workbook= new XSSFWorkbook(excelFile);
        datatypeSheet = workbook.getSheetAt(0);	
        iterator = datatypeSheet.iterator(); iterator.next(); //nos saltamos la cabecera
        while (iterator.hasNext()) {
        	Row currentRow = iterator.next();
        	String col = currentRow.getCell(0).getStringCellValue();
        	String concept = currentRow.getCell(1).getStringCellValue();
        	int uses = (int)currentRow.getCell(2).getNumericCellValue();
        	if(!conUse.containsKey(concept)){conUse.put(concept, new ArrayList<UseOfConceptInData>()); 		}
        	conUse.get(concept).add(new UseOfConceptInData(col,uses));
        }
        workbook.close();
        
            
        //añadimos al tesauro el uso medio de cada rama por colección y en total
        int usesOfConceptsInBranch=0, usedConeptsInBranch=0, totalConceptsInBranch=0;
        List<UseOfConceptInData> infoUsesNumber = new ArrayList<UseOfConceptInData>();
        List<UseOfConceptInData> infoUse = new ArrayList<UseOfConceptInData>();
        for(Resource r: getTopConcepts(thes)){
        	//añadimos la informacion de uso a cada concepto (es recursivo)
        	addUseInfoToConcept(r,conUse);  	
        	//aggregamos la información de todas las tamas para obtener la info resumida del tesauro en el esquema
        	usesOfConceptsInBranch+= Integer.parseInt(r.getProperty(usesOfTheConceptsInBranchInTotalProp).getString());
        	usedConeptsInBranch+=Integer.parseInt(r.getProperty(numberOfUsedConceptInBranchProp).getString());
			totalConceptsInBranch+=Integer.parseInt(r.getProperty(numberOfTotalConceptsInBranchProp).getString());
        	for(Statement stN: r.listProperties(usesOfTheconceptsInBranchInACollectionProp).toList()){
				String[] partes = stN.getString().split("\\|");
				boolean found =false;
				for(UseOfConceptInData i:infoUsesNumber){if(i.col.equals(partes[0])){found=true;i.uses+=Integer.parseInt(partes[1]);break;}}
				if(!found){infoUsesNumber.add(new UseOfConceptInData(partes[0],Integer.parseInt(partes[1])));}
			} 
        	
        	for(Statement stN: r.listProperties(numberOfUsedConceptInBranchInACollectionProp).toList()){
				String[] partes = stN.getString().split("\\|");
				boolean found =false;
				for(UseOfConceptInData i:infoUse){if(i.col.equals(partes[0])){found=true;i.uses+=Integer.parseInt(partes[1]);break;}}
				if(!found){infoUse.add(new UseOfConceptInData(partes[0],Integer.parseInt(partes[1])));}
			}
        	
        }
        
        //añadimos la informacion resumida al esquema
        Resource schema = thes.listSubjectsWithProperty(RDFPropertyManager.skosHasTopConcProp).next();
        schema.addProperty(usesOfTheConceptsInBranchInTotalProp, Integer.toString(usesOfConceptsInBranch));
        schema.addProperty(numberOfUsedConceptInBranchProp, Integer.toString(usedConeptsInBranch));
		schema.addProperty(numberOfTotalConceptsInBranchProp, Integer.toString(totalConceptsInBranch));
		for(UseOfConceptInData i:infoUsesNumber){schema.addProperty(usesOfTheconceptsInBranchInACollectionProp, i.col+"|"+i.uses);}
		for(UseOfConceptInData i:infoUse){schema.addProperty(numberOfUsedConceptInBranchInACollectionProp, i.col+"|"+i.uses);}
				
        //guardamos los resultados
		JenaModelManager.saveJenaModel(thes, outputFile);
		PrintStream salida = new PrintStream(new File(outputFile+".summary.txt"));
		generateReport(thes,thesaurusFile, salida,null, tamanoColecciones);
		for(Statement st:schema.listProperties(usesOfTheconceptsInBranchInACollectionProp).toList()){
			String[] partes = st.getString().split("\\|");
			generateReport(thes,thesaurusFile, salida,partes[0], tamanoColecciones);
		}
			
		return RepeatStatus.FINISHED;
	}
	
	/************************************************************************/
	/**
	 * genera el report de texto del tesauro procesado
	 */
	private void generateReport(Model thes, String thesaurusFile, PrintStream p, String collection, HashMap<String, String> tamanoColecciones){
		Resource schema = thes.listSubjectsWithProperty(RDFPropertyManager.skosHasTopConcProp).next();
		p.println("--------------------------------");
		p.println("Analizando tesauro "+thesaurusFile+ " collection: "+ collection);
		if(collection!=null){
			p.println("Numero de registros en la coleccion: "+tamanoColecciones.get(collection));
		}
		p.println("--------------------------------");
		double total=0, usados=0, usosInBranch=0;
		total = Double.parseDouble(schema.getProperty(numberOfTotalConceptsInBranchProp).getString());
		if(collection==null){
			usados = Double.parseDouble(schema.getProperty(numberOfUsedConceptInBranchProp).getString());
			usosInBranch = Double.parseDouble(schema.getProperty(usesOfTheConceptsInBranchInTotalProp).getString());
		}else{
			for(Statement st:schema.listProperties(numberOfUsedConceptInBranchInACollectionProp).toList()){
				String[] partes = st.getString().split("\\|");
				if(partes[0].equals(collection)){usados=Integer.parseInt(partes[1]);break;}
			}
			for(Statement st:schema.listProperties(usesOfTheconceptsInBranchInACollectionProp).toList()){
				String[] partes = st.getString().split("\\|");
				if(partes[0].equals(collection)){usosInBranch=Integer.parseInt(partes[1]);break;}
			}
		}
		p.printf("%.2f%% usado (%d/%d) con %d usos %n",(usados/total)*100, (int)usados, (int)total, (int) usosInBranch);
		
		List<Resource> tops = getTopConcepts(thes);
		for(Resource r: tops){
			showInfoConcept(r,1,analisisLevel, p,usosInBranch,collection);
		}	
	}
	private void showInfoConcept(Resource r, int level, int maxlevel, PrintStream p, double usosTotales, String collection){
		String sep ="";for(int i=0;i<level*3;i++,sep+=" ");
		if(level<=maxlevel){
			String label="";for(Statement st:r.listProperties(RDFPropertyManager.skosPrefLabelProp).toList()){if(st.getLanguage().equals("en")){label=st.getString();}};
			double total=0, usados=0, usosInBranch=0;
			total = Double.parseDouble(r.getProperty(numberOfTotalConceptsInBranchProp).getString());
			if(collection==null){
				usados = Double.parseDouble(r.getProperty(numberOfUsedConceptInBranchProp).getString());		
				usosInBranch = Double.parseDouble(r.getProperty(usesOfTheConceptsInBranchInTotalProp).getString());
			}else{
				for(Statement st:r.listProperties(numberOfUsedConceptInBranchInACollectionProp).toList()){
					String[] partes = st.getString().split("\\|");
					if(partes[0].equals(collection)){usados=Integer.parseInt(partes[1]);break;}
				}
				for(Statement st:r.listProperties(usesOfTheconceptsInBranchInACollectionProp).toList()){
					String[] partes = st.getString().split("\\|");
					if(partes[0].equals(collection)){usosInBranch=Integer.parseInt(partes[1]);break;}
				}
			}
			p.printf("%s %.2f%% (%d/%d) usado %.2f%% de usos (%d) %n",sep+label,(usados/total)*100, (int)usados,(int)total,(usosInBranch/usosTotales)*100,(int)usosInBranch);			
			for(Statement st:r.listProperties(RDFPropertyManager.skosNarrowerProp).toList()){
				showInfoConcept(st.getResource(),level+1,maxlevel,p,usosTotales, collection);
			}
		}
	}
		
	/************************************************************************/
	/**
	 * añade las medidas de uso locales y la suma de los hijos.
	 * La total y por colección
	 */
	private void addUseInfoToConcept(Resource r, HashMap<String, List<UseOfConceptInData>> conUse){
		List<UseOfConceptInData> infoUsesNumber = new ArrayList<UseOfConceptInData>();
		List<UseOfConceptInData> infoUse = new ArrayList<UseOfConceptInData>();
		int sumaUses = 0, usedConcept=0, totalConcepts=1;
		//guardamos las estadisticas propias del nodo
		if(conUse.containsKey(r.getURI())){
			infoUsesNumber.addAll(conUse.get(r.getURI()));
			for(UseOfConceptInData i:infoUsesNumber){infoUse.add(new UseOfConceptInData(i.col,1));}
			for(UseOfConceptInData i:infoUsesNumber){sumaUses+=i.uses;}
			r.addProperty(usesOfTheConceptInTotalProp, Integer.toString(sumaUses));							
			for(UseOfConceptInData i:infoUsesNumber){r.addProperty(usesOfTheConceptInACollectionProp, i.col+"|"+i.uses);}
			usedConcept++;
		}
		//a las estadisticas propias del nodo les añadimos las estadisticas agregadas de los hijos
		for(Statement st: r.listProperties(RDFPropertyManager.skosNarrowerProp).toList()){
			Resource narrow = (st.getResource());
			addUseInfoToConcept(narrow, conUse);
			sumaUses+= Integer.parseInt(narrow.getProperty(usesOfTheConceptsInBranchInTotalProp).getString());
			usedConcept+=Integer.parseInt(narrow.getProperty(numberOfUsedConceptInBranchProp).getString());
			totalConcepts+=Integer.parseInt(narrow.getProperty(numberOfTotalConceptsInBranchProp).getString());
			for(Statement stN: narrow.listProperties(usesOfTheconceptsInBranchInACollectionProp).toList()){
				String[] partes = stN.getString().split("\\|");
				boolean found =false;
				for(UseOfConceptInData i:infoUsesNumber){if(i.col.equals(partes[0])){found=true;i.uses+=Integer.parseInt(partes[1]);break;}}
				if(!found){infoUsesNumber.add(new UseOfConceptInData(partes[0],Integer.parseInt(partes[1])));}
			}
			for(Statement stN: narrow.listProperties(numberOfUsedConceptInBranchInACollectionProp).toList()){
				String[] partes = stN.getString().split("\\|");
				boolean found =false;
				for(UseOfConceptInData i:infoUse){if(i.col.equals(partes[0])){found=true;i.uses+=Integer.parseInt(partes[1]);break;}}
				if(!found){infoUse.add(new UseOfConceptInData(partes[0],Integer.parseInt(partes[1])));}
			}
			
			
		}
		r.addProperty(usesOfTheConceptsInBranchInTotalProp, Integer.toString(sumaUses));
		r.addProperty(numberOfUsedConceptInBranchProp, Integer.toString(usedConcept));
		r.addProperty(numberOfTotalConceptsInBranchProp, Integer.toString(totalConcepts));
		for(UseOfConceptInData i:infoUsesNumber){r.addProperty(usesOfTheconceptsInBranchInACollectionProp, i.col+"|"+i.uses);}	
		for(UseOfConceptInData i:infoUse){r.addProperty(numberOfUsedConceptInBranchInACollectionProp, i.col+"|"+i.uses);}	
		
	}
	
	/************************************************************************/
	/**
	 * devuelve los top concepts de un tesauro
	 */
	private List<Resource> getTopConcepts(Model m){
		List<Resource> top = new ArrayList<Resource>();
		Iterator<Resource> it = m.listSubjectsWithProperty(RDFPropertyManager.skosHasTopConcProp);
		if(it.hasNext()){
			Resource schema = m.listSubjectsWithProperty(RDFPropertyManager.skosHasTopConcProp).next();
			for(Statement st:schema.listProperties(RDFPropertyManager.skosHasTopConcProp).toList()){top.add(st.getResource());}
			return top;
		}
		
		List<Resource> concepts = m.listSubjects().toList();
		for(Resource c:concepts){
			if(!c.hasProperty(RDFPropertyManager.skosBroaderProp)){top.add(c);}
		}
		Resource sch = m.createResource();
		for(Resource c:top){
			sch.addProperty(RDFPropertyManager.skosHasTopConcProp, c);
		}
		
		return top;
	}
	
	/************************************************************************/
	/**
	 * estructura para almacenar el uso de cada concepto
	 */
	private class UseOfConceptInData{
		String col;
		int uses;
		public UseOfConceptInData(String _col,int _uses){
			col=_col;
			uses=_uses;
		}
	}
	
	/************************************************************************/
    /**
     * fija los parametros del tasklet
     */
	public void setThesaurusFile(String thesaurusFile) {this.thesaurusFile = thesaurusFile;}
	public void setThesaurusUseFile(String thesaurusUseFile) {this.thesaurusUseFile = thesaurusUseFile;}
	public void setOutputFile(String outputFile) {this.outputFile = outputFile;}
	public void setColectionSizeFile(String colectionSizeFile) {this.colectionSizeFile = colectionSizeFile;}
}
