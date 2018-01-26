/**
 * 
 */
/**
 * @author dayanydc
 *
 */
package prueba;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class prueba {

public static void main(String[] args) throws Exception {
	 
	try {
		
		// create HTTP Client
		HttpClient httpClient = HttpClientBuilder.create().build();
		
		// Create new getRequest with below mentioned URL
		HttpGet getRequest = new HttpGet("http://opendata.aragon.es/datos/api/3/action/package_list");	
		
		// Execute your request and catch response
		HttpResponse response = httpClient.execute(getRequest);
		
		// Check for HTTP response code: 200 = success
		 if (response.getStatusLine().getStatusCode() != 200) {
		 throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
		}
		// Get-Capture Complete application/xml body response
		BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));	
	    String output;	
	    String var= null;
	    
		// Simply iterate through JSON response and show on console.
		while ((output = br.readLine()) != null) {			
			 var= output;				
		}		
		//Decoding JSON		
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject)parser.parse(var);		
		JSONArray a = (JSONArray)(obj.get("result"));
	   
		String b;
		String output1;
		String var1= null;
		BufferedReader res= null;
		FileWriter fw;
	
							
		//Obteniendo id de los dataset.
		for (int i = 0; i <a.size(); i++) { 
			//System.out.println(a.get(i));
			b=(String) a.get(i);
			httpClient = HttpClientBuilder.create().build();
			getRequest= new HttpGet("http://opendata.aragon.es/datos/api/3/action/package_show?id="+b);
			response = httpClient.execute(getRequest);
			
			if (response.getStatusLine().getStatusCode() != 200) {
				 throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
				}
			 res = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
		     //Obteniendo el recurso.			
			 while ((output1 = res.readLine()) != null) {			
				 var1= output1;				
			}				 
			
		   	fw = new FileWriter("d:\\prueba\\european\\european"+i);
			fw.write(var1);
			fw.close(); 
			Thread.sleep(1000);
		
		}		
		 
	} catch (ClientProtocolException e) {
		e.printStackTrace();

	} catch (IOException e) {
		e.printStackTrace();
	}
}

}