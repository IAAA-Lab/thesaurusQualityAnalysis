package prueba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Indice {
	// Se crea el Mapa
	public static Map<String, List<Objeto>> index = new TreeMap<>(); 

	public static void writeMap() {
		try{
		FileWriter fw = null;					
		fw = new FileWriter("D:\\metricas\\usoConcepto.txt",true);
		fw.write("Concepto" + ";" + "NoColecciones" + ";" + " Cant_Ocurrencias" + "\n");
		for (Map.Entry<String, List<Objeto>> entry : index.entrySet()) {
			
			String concepto = entry.getKey();
			List<Objeto> listObj = entry.getValue();						
			
			
			int sum= 0;
			for (int i = 0; i < listObj.size(); i++) {
				Objeto obj = listObj.get(i);
				sum += Integer.valueOf(obj.getWeight()).intValue();
								
			}
				
			fw.write(concepto + ";" + listObj.size() + ";" + sum + ";" + "\n");
		}
		
		    fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			List<Objeto> list = null;

			String path = "D:\\metricas_todo\\metricaConceptoHTTP.txt"; 																																				
			BufferedReader br = new BufferedReader(new FileReader(new File(path)));
			String line = br.readLine();
			line = br.readLine();

			String[] array = line.split(";");
			String collection = array[0];
			String concept = array[1];
			String frequency = array[2];		

			list = new LinkedList<>();
          // Se lee el fichero,y se adiciona en la lista.
			while (line != null) {
					if (index.containsKey(concept)) {
						list = index.get(concept);
						list.add(new Objeto(collection, frequency));
					} else {
						list.add(new Objeto(collection, frequency));
					}
					index.put(concept, list);
			
				line = br.readLine();
				if (line != null) {
					list = new LinkedList<>();
					array = line.split(";");
					collection = array[0];
					concept = array[1];
					frequency = array[2];
				} 
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		 writeMap();
	}
}
