/**
 * 
 */
/**
 * @author dayanydc
 *
 */
package prueba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

public class ConsultaEuropeana {

	public static void main(String[] args) {
		try {
			String sDirectorio = "D:\\coleccionesC.txt";
			File archivo = new File(sDirectorio);
			FileReader fr = new FileReader(archivo);
			BufferedReader br = new BufferedReader(fr);
			String model = "http://sparql.europeana.eu/";
			String linea;
			String a = null;						
			FileWriter fw = null;
			FileWriter fwc = null;
			FileWriter fwo = null;
			FileWriter fwoc = null;
			FileWriter fwsol= null;
			FileWriter fwct = null;	
			FileWriter fwctt = null;
			fw = new FileWriter("d:\\metricas\\metrica.txt", true);
			fw.write("Colección " + ";" + "Tesauro" + ";" + " Cant_Ocurrencias" + "\n");
			fwc = new FileWriter("d:\\metricas\\metricaConcepto.txt", true);
			fwc.write("Colección " + ";" + "Concepto" + ";" + " Cant_Ocurrencias" + "\n");
			fwo = new FileWriter("d:\\metricas\\metricaProveeConcep.txt", true);
			fwo.write("Colección " + ";" + "Concepto" + ";" + "Proveedor" + ";" + " Cant_Ocurrencias" + "\n");
			fwoc = new FileWriter("d:\\metricas\\metricaProveedorCant.txt", true);
			fwoc.write("Colección " + ";" + "Proveedor" + ";" + " Cant_Ocurrencias" + "\n");
			fwsol= new FileWriter("d:\\metricas\\metricaSubjCant.txt",true);
			fwsol.write("Colección " + ";" + "Subject" + ";" + " Cant_Ocurrencias" + "\n");			
			fwct = new FileWriter("d:\\metricas\\metricaSubjCant_HTTP.txt",true);
			fwct.write("Colección " + ";" + "Subject" + ";" + " Cant_Ocurrencias" + "\n");
			fwctt = new FileWriter("d:\\metricas\\metricaConcepto_HTTP.txt", true);
			fwctt.write("Colección " + ";" + "Concepto" + ";" + " Cant_Ocurrencias" + "\n");

			while ((linea = br.readLine()) != null) {
				a = linea;

				// Definición de consultas SPARQL
				
				//Número de registros de un tesauros utilizado en la colección.

				String qs = "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+ "SELECT (sample(SUBSTR(str(?y),1,19)) as ?r) (count(SUBSTR(str(?y),1,19)) as ?c)"
						+ "WHERE{	?x dc:subject ?y. " + "?x <http://www.openarchives.org/ore/terms/proxyIn> ?p. "
						+ "?p <http://www.europeana.eu/schemas/edm/collectionName> \"" + a + "\". "
						+ " } group by  (SUBSTR(str(?y),1,19) as ?m)";
				
				//Número de conceptos de un tesauro utilizados en la colección.
				
				String qsc= "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
				        + "SELECT ?y   (count(?y) as ?c)" 
						+ "WHERE{ ?x dc:subject ?y. " 
				        + "?x <http://www.openarchives.org/ore/terms/proxyIn> ?p. " 
				        + "?p <http://www.europeana.eu/schemas/edm/collectionName> \"" + a + "\". " 
				        + " } group by ?y";			
							  				 				
				//Nombre de las Organizaciones, conceptos y número de veces utilizadas en la colección.

					String qso = "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+ "SELECT  ?provee ?subj (count(?subj) as ?s)"
						+ " WHERE { ?euAgg <http://www.europeana.eu/schemas/edm/collectionName> \"" + a + "\". "
						+ "?proxy <http://www.openarchives.org/ore/terms/proxyIn> ?euAgg. "
						+ "?proxy dc:subject ?subj. "
						+ "?proxy <http://www.openarchives.org/ore/terms/proxyFor> ?proCHO. "
						+ "?agg <http://www.europeana.eu/schemas/edm/aggregatedCHO> ?proCHO. "
						+ "?agg <http://www.europeana.eu/schemas/edm/dataProvider> ?provee. "
						+ "} group by ?subj ?provee";
				
				// Nombre de las Organizaciones utilizadas en la colección y número de veces utilizadas.
				
				 	String qsoc= "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
					    + "SELECT   ?provee (count(?provee) as ?proveedor)"
						+ " WHERE { ?euAgg <http://www.europeana.eu/schemas/edm/collectionName> \"" + a + "\". "
						+ "?proxy <http://www.openarchives.org/ore/terms/proxyIn> ?euAgg. " 
						+ "?proxy <http://www.openarchives.org/ore/terms/proxyFor> ?proCHO. "
						+ "?agg <http://www.europeana.eu/schemas/edm/aggregatedCHO> ?proCHO. "
						+ "?agg <http://www.europeana.eu/schemas/edm/dataProvider> ?provee. "
						+ "} group by ?provee";
				
	            String query= "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
                       + "SELECT ?subj   (count(?subj) as ?cant) "
                       + " WHERE { ?euAgg <http://www.europeana.eu/schemas/edm/collectionName>  \"" + a + "\". "
                       + "?proxy <http://www.openarchives.org/ore/terms/proxyIn> ?euAgg. " 
                       + "?proxy <http://www.openarchives.org/ore/terms/proxyFor> ?proCHO. "
                       + "?agg <http://www.europeana.eu/schemas/edm/aggregatedCHO> ?proCHO. "
                       + "?agg <http://www.europeana.eu/schemas/edm/dataProvider> ?provee. "
                       + "?proprov <http://www.openarchives.org/ore/terms/proxyIn> ?agg. "
                       + "?proprov dc:subject ?subj. "
                       + "} group by ?subj ORDER BY DESC (?cant)";
				
				 String qst = "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
	                       + "SELECT (sample(SUBSTR(str(?subj),1,19)) as ?r )  (count(SUBSTR(str(?subj),1,19)) as ?cant) "
	                       + " WHERE { ?euAgg <http://www.europeana.eu/schemas/edm/collectionName>  \"" + a + "\". "
	                       + "?proxy <http://www.openarchives.org/ore/terms/proxyIn> ?euAgg. " 
	                       + "?proxy <http://www.openarchives.org/ore/terms/proxyFor> ?proCHO. "
	                       + "?agg <http://www.europeana.eu/schemas/edm/aggregatedCHO> ?proCHO. "
	                       + "?agg <http://www.europeana.eu/schemas/edm/dataProvider> ?provee. "
	                       + "?proprov <http://www.openarchives.org/ore/terms/proxyIn> ?agg. "
	                       + "?proprov dc:subject ?subj. "
	                       + "filter( regex(str(?subj), '^http')) "
	                       + "} group by  (SUBSTR(str(?subj),1,19) as ?m) ";
				
                //Número de conceptos de un tesauro utilizados en la colección HTTP.
				
				String qsct= "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
				           + "SELECT ?subj   (count(?subj) as ?c)" 
				           + " WHERE { ?euAgg <http://www.europeana.eu/schemas/edm/collectionName>  \"" + a + "\". "
	                       + "?proxy <http://www.openarchives.org/ore/terms/proxyIn> ?euAgg. " 
	                       + "?proxy <http://www.openarchives.org/ore/terms/proxyFor> ?proCHO. "
	                       + "?agg <http://www.europeana.eu/schemas/edm/aggregatedCHO> ?proCHO. "
	                       + "?agg <http://www.europeana.eu/schemas/edm/dataProvider> ?provee. "
	                       + "?proprov <http://www.openarchives.org/ore/terms/proxyIn> ?agg. "
	                       + "?proprov dc:subject ?subj. "
	                       + "filter( regex(str(?subj), '^http')) " 
				           + " } group by ?subj";	
				 				
				// Declaración del objeto de la consulta

				Query q = QueryFactory.create(qs);
				Query qc = QueryFactory.create(qsc);
				Query qo = QueryFactory.create(qso);
				Query qoc = QueryFactory.create(qsoc);
				Query queryS = QueryFactory.create(query);
				Query querySt = QueryFactory.create(qst);
				Query qct = QueryFactory.create(qsct);				

				// Inicializacion de QueryExecution factory

				QueryExecution qe = QueryExecutionFactory.sparqlService(model, q);
				QueryExecution qec = QueryExecutionFactory.sparqlService(model, qc);
				QueryExecution qeo = QueryExecutionFactory.sparqlService(model, qo);
				QueryExecution qeoc = QueryExecutionFactory.sparqlService(model, qoc);
				QueryExecution qexc = QueryExecutionFactory.sparqlService(model, queryS);
				QueryExecution qexct = QueryExecutionFactory.sparqlService(model, querySt);
				QueryExecution qect = QueryExecutionFactory.sparqlService(model, qct);

				try {
					// Resultado de ejecución de la consulta.
					
					ResultSet re = qe.execSelect();
					ResultSet rec = qec.execSelect();
					ResultSet reo = qeo.execSelect();
					ResultSet reoc = qeoc.execSelect();
					ResultSet rsul = qexc.execSelect();
					ResultSet rsult = qexct.execSelect();
					ResultSet rect = qect.execSelect();
					
					System.out.println("Se esta imprimiendo la colección" + a);

					while (re.hasNext()) {

						QuerySolution b = re.next();
						String cName = b.getLiteral("c").getLexicalForm();
						RDFNode rName = b.get("r");
					//	String rName = b.getLiteral("r").getLexicalForm();
						fw.write(a + ";" + rName + ";" + cName + ";" + "\n");
					}
					
					while (rec.hasNext()) {

						QuerySolution bc = rec.next();
						RDFNode stringConcep = bc.get("y");
						String cantidad = bc.getLiteral("c").getLexicalForm();
					//	Resource concepto = (Resource) bc.get("y");
					//	String stringConcep = concepto.getURI();						
					    fwc.write(a + ";" + stringConcep + ";" + cantidad + ";" + "\n");
					}

						while (reo.hasNext()) {

						QuerySolution bo = reo.next();
					//	String provee = bo.getLiteral("provee").getLexicalForm();
						RDFNode provee = bo.get("provee");
						RDFNode stringSubj = bo.get("subj");
						String cantidad = bo.getLiteral("s").getLexicalForm();
					//	Resource subj = (Resource) bo.get("subj");
					//	String stringSubj = subj.getURI();						
						fwo.write(a + ";" + stringSubj + ";" + provee + ";" + cantidad + ";" + "\n");											
					}
					
						while (reoc.hasNext()) {

						QuerySolution boc = reoc.next();
					//	String provee = boc.getLiteral("provee").getLexicalForm();
						RDFNode provee=boc.get("provee");
						String cantidad = boc.getLiteral("proveedor").getLexicalForm();					
						fwoc.write(a + ";"+ provee + ";" + cantidad + ";" + "\n");
					}
					
					while (rsul.hasNext()) {

						QuerySolution sol = rsul.next();
						String cant = sol.getLiteral("cant").getLexicalForm();
						RDFNode subj = sol.get("r");							
						fwsol.write(a + ";"+ subj + ";" + cant+ ";" + "\n");
					}
					
					while (rsult.hasNext()) {

						QuerySolution sol = rsult.next();
						String cant = sol.getLiteral("cant").getLexicalForm();
						RDFNode subj = sol.get("r");							
						fwct.write(a + ";"+ subj + ";" + cant+ ";" + "\n");
					}
					
					while (rect.hasNext()) {

						QuerySolution sol = rect.next();
						String cant = sol.getLiteral("c").getLexicalForm();
						RDFNode subj = sol.get("subj");							
						fwctt.write(a + ";"+ subj + ";" + cant+ ";" + "\n");
					}

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					qe.close();
					qec.close();
					qeo.close();
					qeoc.close();
					qexc.close();
					qexct.close();
					qect.close();

				}
			}
			fr.close();
			fw.close();
			fwc.close();
			fwo.close();
			fwoc.close();
			fwsol.close();
			fwct.close();
			fwctt.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}