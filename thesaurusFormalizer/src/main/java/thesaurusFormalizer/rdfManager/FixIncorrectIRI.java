package thesaurusFormalizer.rdfManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

/** 
 * corrige las iris de un rdf con espacios quitando los espacios, lo hace con el texto a pelo, es un apaño
 * 
 * */
public class FixIncorrectIRI {

	public static void main(String[] args) {
		try {
			BufferedReader br = new BufferedReader(new FileReader("data/input/thesauri/culturaItalia.skos.xml"));
			BufferedWriter bw = new BufferedWriter( new FileWriter("data/input/thesauri/culturaItaliac.skos.xml"));
			
			String line = br.readLine();
			while(line!=null) {
				if(line.contains("skos:Concept")||line.contains("skos:related")||line.contains("skos:narrower")||line.contains("skos:broader")) {
					int index =line.indexOf("\"");
					if (index>=0) {
						String l2 = line.substring(index);
						line= line.substring(0, index);
						l2=l2.replaceAll(" ", "");
						line+=l2;
					}
				}
				bw.write(line);bw.newLine();
				line = br.readLine();
			}
			bw.close();
			br.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
