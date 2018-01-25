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

public class ConsultaEuropeanaProveedores {
		public static void main(String[] args) {
		try {
		    String sDirectorio = "src\\main\\resources\\Proveedores.txt";
			File archivo = new File(sDirectorio);
			FileReader fr = new FileReader(archivo);
		    BufferedReader br = new BufferedReader(fr);
			String model = "http://sparql.europeana.eu/";
			String linea;
			String a = null;						
	//		FileWriter fw = null;
			FileWriter fws = null;
	//		FileWriter fwc = null;
			
			
			fws = new FileWriter("src\\main\\resources\\metricaprovee\\metricaNew.txt", true);
			fws.write("Proveedor " + ";" + "Tesauro" + ";" + " Cant_Ocurrencias" + "\n");
			
		/*	fw = new FileWriter("src\\main\\resources\\metricaprovee\\providerDP.txt", true);
			fw.write("Provider " + ";" + "DataProvider" + "\n");
			
			fwc = new FileWriter("src\\main\\resources\\metricaprovee\\cantAllsubj.txt", true);
			fwc.write("Proveedor " + ";" + "Subject" + ";" + " Cant_Ocurrencias" + "\n");*/
			
			
			while ((linea = br.readLine()) != null) {
				a = linea;

				// Definición de consultas SPARQL
				
				//Número de registros de subject http utilizado proveedor.

			/*	String qs = "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+"PREFIX edm:<http://www.europeana.eu/schemas/edm/> "
						+"PREFIX oai:<http://www.openarchives.org/ore/terms/> "
						+"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+"SELECT (sample(SUBSTR(str(?k),1,20)) as ?t) (count(SUBSTR(str(?k),1,20)) as ?c)"
						+"WHERE{	?a1 edm:provider " + a + ". "
						+"?p1 oai:proxyIn ?a1. "
						+" ?a1 rdf:type oai:Aggregation. "
						+"?p1 dc:subject ?k. "
						+"  filter(regex(str(?k), 'http:' )). "
						+ " } group by  (SUBSTR(str(?k),1,20) as ?m)";*/
				
				String qs = "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+"PREFIX edm:<http://www.europeana.eu/schemas/edm/> "
						+"PREFIX oai:<http://www.openarchives.org/ore/terms/> "
						+"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+"SELECT ?ns  (count(?ns) as ?c) "
						+"WHERE{ ?a1 edm:provider " + a + ". "
						+"?p1 oai:proxyIn ?a1. "
						+" ?a1 rdf:type oai:Aggregation. "
						+"?p1 dc:subject ?k. "
						+ "BIND(REPLACE(str(?k), '(#|/)[^/]*$', '$1') AS ?ns). "
						+" filter( isURI(?k)). "
						+ " } group by ?ns";
				
				// Registro de dataProvider por Proveedores
				
		/*		String query= "PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						  +"PREFIX edm:<http://www.europeana.eu/schemas/edm/> "
						  +"PREFIX oai:<http://www.openarchives.org/ore/terms/> "
						  +"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						  +"SELECT ?dp "
						  +"WHERE{ ?a1 rdf:type oai:Aggregation. "
						  +"?a1 edm:provider " + a + ". "
						  +"?a1 edm:dataProvider ?dp."
						  +"?a1 edm:dataProvider ?dp."
						  +"?p1 oai:proxyIn ?a1."
						  +"?p1 dc:subject ?k."
						  +"filter( regex(str(?k), 'http:' ))."		    					          							                 						  
						  +" } group by ?dp ";
				
				//Cantidad de Subject por Proveedor
				
				String qsc="PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						  +"PREFIX edm:<http://www.europeana.eu/schemas/edm/> "
						  +"PREFIX oai:<http://www.openarchives.org/ore/terms/> "
						  +"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						  +"SELECT ?subj (count(?subj) as ?c)"
						  +"WHERE { ?agg edm:provider " + a + "."       
						  +"?proxy oai:proxyIn ?agg." 
						  +"?agg rdf:type oai:Aggregation."	 
						  +"?proxy dc:subject ?subj."								                            
						  +"} group by ?subj";*/
				 				
				// Declaración del objeto de la consulta
				System.out.println(qs);
				Query q = QueryFactory.create(qs);
			/*	System.out.println(query);
				Query qdp = QueryFactory.create(query);
				System.out.println(qsc);
				Query qesc = QueryFactory.create(qsc);*/
						

				// Inicializacion de QueryExecution factory

				QueryExecution qe = QueryExecutionFactory.sparqlService(model, q);
			/*	QueryExecution qdpe = QueryExecutionFactory.sparqlService(model, qdp);
				QueryExecution qdpc = QueryExecutionFactory.sparqlService(model, qesc);*/
				

				try {
					// Resultado de ejecución de la consulta.
					
					ResultSet re = qe.execSelect();
			/*		ResultSet redp = qdpe.execSelect();
					ResultSet rec = qdpc.execSelect();*/
                    
					while (re.hasNext()) {

						QuerySolution b = re.next();
						String cant = b.getLiteral("c").getLexicalForm();
				    	RDFNode tName = b.get("ns");						
						System.out.println("Se imprimio el proveedor" + a);
					    fws.write(a + ";" + tName + ";" + cant + ";" + "\n");
					    
					   
					}
					
										
		/*			while (redp.hasNext()) {

						QuerySolution b = redp.next();
						RDFNode dataP = b.get("dp");						
						System.out.println("Se imprimio el proveedor" + a);
					    fw.write(a + ";" + dataP + ";" + "\n");
					}
					
					while (rec.hasNext()) {

						QuerySolution b = rec.next();
						String cant = b.getLiteral("c").getLexicalForm();
						RDFNode subject = b.get("subj");						
						System.out.println("Se imprimio el proveedor" + a);
					    fwc.write(a + ";" + subject + ";" + cant + ";" + "\n");
					
						
					}*/
					
				
					
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					qe.close();
				/*	qdpe.close();
					qdpc.close();*/
					
				}
			}
		    fr.close();
			fws.close();
	/*		fw.close();
			fwc.close();*/
			br.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}