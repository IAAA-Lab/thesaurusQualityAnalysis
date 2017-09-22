package prueba;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.client.ClientProtocolException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class OAICrawler {

	private String _oaiServiceUrl;
	private String _set;
	private int _maxRequests;
	private String _outputFolder;
	private int _waitingTime; 
	public static final String DC = "oai_dc";	
	public static final String MARC = "marcxml";		
	private static final String _RESUMPTIONTOKEN= "resumptionToken";
	

	public OAICrawler(String oaiServiceUrl, String set,int maxRequests, String outputFolder) {
		this(oaiServiceUrl,set,maxRequests,outputFolder,1);
	}

	public OAICrawler(String oaiServiceUrl, String set,int maxRequests, String outputFolder, int waitingTime) {
		_oaiServiceUrl = oaiServiceUrl;
		_set = set;	
		_maxRequests=maxRequests;
		_outputFolder = outputFolder;
		_waitingTime = waitingTime;
	}

	public void crawlEDM(){
		crawl("edm");
	}
		
	private static void wait(int seconds) {
        try{ 
        	Thread.sleep(seconds*1000);
        } catch(Exception ex) {}
	}
	
	public void crawl(String metadataStandard){
		String request = _oaiServiceUrl+"?verb=ListRecords&metadataPrefix="+metadataStandard+"&set="+_set;
		int i=1;
	    String fileName = _outputFolder+"/"+_set+metadataStandard+i+".xml";
	    
		executeGet(request,fileName,_waitingTime);

		String resumptionToken=getResumptionToken(fileName);
		System.out.println(fileName + " " + resumptionToken);
		
		while ((i<_maxRequests)&&(resumptionToken!=null)){
			i++;
			request = _oaiServiceUrl+"?verb=ListRecords&resumptionToken="+resumptionToken;
			fileName = _outputFolder+"/"+_set+metadataStandard+i+".xml";
			executeGet(request,fileName,_waitingTime);
			resumptionToken=getResumptionToken(fileName);
			System.out.println(fileName + " " + resumptionToken);
		}
	}		
	
	private static String getResumptionToken(String fileName) {
		String result = null;
		try{
		  DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		  DocumentBuilder builder = domFactory.newDocumentBuilder();
		  org.w3c.dom.Document xmlDoc = builder.parse(new File(fileName));
		  
		  NodeList list = xmlDoc.getElementsByTagName(_RESUMPTIONTOKEN);
		  
		  if ((list!=null)&& (list.getLength()>0))
			  result = list.item(0).getTextContent();
		  
		  } catch( Exception ex ) {
			  System.err.println(ex.getMessage());
		  }
		return result;
		
	}
	
/*	private static String getListsetSpec(String fileName) {
		String resultlist = null;
		try{
		  DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		  DocumentBuilder builder = domFactory.newDocumentBuilder();
		  org.w3c.dom.Document xmlDoc = builder.parse(new File(fileName));
		  
		  NodeList list1 = xmlDoc.getElementsByTagName(_setSpec);
		  
		  if ((list1!=null)&& (list1.getLength()>0))
			  resultlist = list1.item(0).getTextContent();
		      
		  } catch( Exception ex ) {
			  System.err.println(ex.getMessage());
		  }
		return resultlist;
		
	}*/
		
	public static void main(String[] args) {
	 try {
		String sDirectorio ="D:\\colecciones.txt";		
		File archivo = new File (sDirectorio);
		FileReader fr = null;
		fr= new FileReader (archivo);
		BufferedReader br = new BufferedReader(fr);
		String linea;
		String set= null;
		while ((linea = br.readLine()) != null){
		 set=linea;
		 OAICrawler tazCrawler = new OAICrawler("http://oai.europeana.eu/oaicat/OAIHandler",set,5,"taz");
		 tazCrawler.crawlEDM();
		}	
		  fr.close();
	 } catch (Exception e){
         e.printStackTrace();
	   }
	}

    private static void executeGet(String targetURL, String fileName, int waitingTime) {  
    	
    	  wait(waitingTime);    	  
	      URL url;  
	      HttpURLConnection connection = null;
	      
	      try {  
	          //Crea la conexion  
	          url = new URL(targetURL);  
	          connection = (HttpURLConnection) url.openConnection();  
	          connection.setRequestMethod("GET");              
	          connection.setUseCaches(false);  
	          connection.setDoOutput(true);  
	  
	          //process response  
	          connection.getErrorStream();  
	          int codigo= connection.getResponseCode();//200, 500, 404, etc.  
	  
	          System.out.println(targetURL + " " +codigo);
	          //Status 200 is Ok
	          if (codigo == 200){ // Si devuelve codigo 200 OK
	        	  
		          //transformar a string el response 
		          InputStream is = connection.getInputStream();  

		          System.out.println("escribiendo "+ fileName);
		          
		          FileOutputStream fos = new FileOutputStream (fileName);
		          
		          int leido = is.read();
		          while (leido!=-1) {
		        	  fos.write(leido);
		        	  leido=is.read();
		          }
		          is.close();
		          fos.close();
		        
	          }
	        } catch (Exception e) {
	            System.err.println(e.getMessage());
	            e.printStackTrace();
	        } finally {  
	          if (connection != null) {  
	              connection.disconnect();  
	          }
	          
	      }  
	  }     

}
