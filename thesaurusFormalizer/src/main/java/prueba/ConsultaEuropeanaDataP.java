/**
 * 
 */
/**
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

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

public class ConsultaEuropeanaDataP {
		public static void main(String[] args) {
		try {
		    String sDirectorio = "src\\main\\resources\\DataProveedores.txt";
			File archivo = new File(sDirectorio);
			FileReader fr = new FileReader(archivo);
		    BufferedReader br = new BufferedReader(fr);
			String model = "http://sparql.europeana.eu/";
			String linea;
			String a = null;						
			FileWriter fw = null;
		//	FileWriter fwc = null;
			
			fw = new FileWriter("src\\main\\resources\\metricaprovee\\SubjHTTP.txt", true);
			fw.write("DataProveedor " + ";" + "Tesauro" + ";" + " Cant_Ocurrencias" + "\n");
			
		/*	fwc = new FileWriter("src\\main\\resources\\metricaprovee\\TotalSubjHTTP.txt", true);
			fwc.write("DataProveedor " + ";" + " Cant_Ocurrencias" + "\n");*/
			
			while ((linea = br.readLine()) != null) {
				a = linea;

				// Definición de consultas SPARQL
				
				//Número de registros de un tesauros utilizado en la colección.

			/*	String qs = "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+"PREFIX edm:<http://www.europeana.eu/schemas/edm/> "
						+"PREFIX oai:<http://www.openarchives.org/ore/terms/> "
						+"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+"SELECT (sample(SUBSTR(str(?k),1,20)) as ?t) (count(SUBSTR(str(?k),1,20)) as ?c)"
						+"WHERE{	?a1 edm:dataProvider " + a + ". "
						+"?p1 oai:proxyIn ?a1. "
						+" ?a1 rdf:type oai:Aggregation. "
						+"?p1 dc:subject ?k. "
						+"  filter(regex(str(?k), 'http:' )). "
						+ " } group by  (SUBSTR(str(?k),1,20) as ?m)";*/
				
				
				//Consulta para determinar los espacios de nombre de los subj http sin enriquecimiento
				
			/*	String qs = "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+"PREFIX edm:<http://www.europeana.eu/schemas/edm/> "
						+"PREFIX oai:<http://www.openarchives.org/ore/terms/> "
						+"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+"SELECT ?ns  (count(?ns) as ?c) "
						+"WHERE{ ?a1 edm:dataProvider " + a + ". "
						+"?p1 oai:proxyIn ?a1. "
						+" ?a1 rdf:type oai:Aggregation. "
						+"?p1 dc:subject ?k. "
						+ "BIND(REPLACE(str(?k), '(#|/)[^/]*$', '$1') AS ?ns). "
						+" filter( isURI(?k)). "
						+ " } group by ?ns";*/
				
			//Consulta para devolver el número de subj HTTP por dataProvider.
				
				String qs = "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+"PREFIX edm:<http://www.europeana.eu/schemas/edm/> "
						+"PREFIX oai:<http://www.openarchives.org/ore/terms/> "
						+"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+"SELECT ?ns  (count(?ns) as ?c) "
						+"WHERE{ ?a1 edm:dataProvider " + a + ". "
						+"?p1 oai:proxyIn ?a1. "
						+" ?a1 rdf:type oai:Aggregation. "
						+"?p1 dc:subject ?ns. "	
						+"filter( regex(str(?ns), '^http')). "
						+ " } group by ?ns";
				
				//Consulta para devolver el número de ocurrencias total por dataproviders.
				
			/*	String qsc="PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						  +"PREFIX edm:<http://www.europeana.eu/schemas/edm/> "
						  +"PREFIX oai:<http://www.openarchives.org/ore/terms/> "
						  +"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						  +"SELECT ?subj (count(?subj) as ?c)"
						  +"WHERE { ?agg edm:dataProvider " + a + "."       
						  +"?proxy oai:proxyIn ?agg." 
						  +"?agg rdf:type oai:Aggregation."	 
						  +"?proxy dc:subject ?subj."								                            
						  +"} group by ?subj";*/
								 				
				// Declaración del objeto de la consulta
				System.out.println(qs);
				Query q = QueryFactory.create(qs);
		/*		System.out.println(qsc);
				Query qc = QueryFactory.create(qsc);*/
						

				// Inicializacion de QueryExecution factory

				QueryExecution qe = QueryExecutionFactory.sparqlService(model, q);
			//	QueryExecution qce = QueryExecutionFactory.sparqlService(model, qc);

				try {
					// Resultado de ejecución de la consulta.
					
					ResultSet re = qe.execSelect();
				//	int count=0;						
					while (re.hasNext()) {

						QuerySolution b = re.next();
						String cant = b.getLiteral("c").getLexicalForm();
						RDFNode tName = b.get("ns");						
						System.out.println("Se imprimio el proveedor" + a);
					   fw.write(a + ";" + tName + ";" + cant + ";" + "\n");
					//	count+= Integer.parseInt(cant);
					}
					
				//	fwc.write(a + ";" + count + ";" + "\n");
					
			/*		ResultSet rce = qce.execSelect();
									
					while (rce.hasNext()) {
						QuerySolution b = rce.next();
						String cant = b.getLiteral("c").getLexicalForm();
						RDFNode subject = b.get("subj");						
						System.out.println("Se imprimio el proveedor" + a);
				        fwc.write(a + ";" + subject + ";" + cant + ";" + "\n");
												
					}*/
					
					
					
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					qe.close();
					//qce.close();
					
				}
			}
		    fr.close();
			//fwc.close();
		    fw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}