package tools.wordnetManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/* permite encontrar los synsets en base a etiquetas en español*/
public class OpenMultilingualWordnetManager{

	private HashMap<String, List<Long>> wnModel = new HashMap<String, List<Long>> ();
	
	/*************************************************/
	/**
	 * al construir el objeto se carga el binario
	 */
	public OpenMultilingualWordnetManager(String hashLoc) {
		wnModel = loadWordnetSynsetsFromBinary(hashLoc);
	}
	
	/*************************************************/
	/**
	 * carga los syssets de binario
	 */
	
	@SuppressWarnings("unchecked")
	private static HashMap<String, List<Long>> loadWordnetSynsetsFromBinary(String hashLoc){
		
		HashMap<String, List<Long>> model = new HashMap<String, List<Long>> () ;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(hashLoc));
			model = (HashMap<String, List<Long>>) ois.readObject();
			ois.close();
		} catch (Exception e) {e.printStackTrace();}
		return model;
	}
	
	/*************************************************/
	/**
	 * devuelve los synsets de la etiqueta
	 */
	public List<Long> getSynsets(String label){
		return wnModel.get(label);
	}
	
	/*************************************************/
	/**
	 * carga los syssets de texto
	 */
	private static HashMap<String, List<Long>> loadWordnetSynsetsFromText(String source){
		HashMap<String, List<Long>> model = new HashMap<String, List<Long>> () ;
		try {
			Scanner input = new Scanner(new FileReader(new File(source)));
			input.nextLine();
			while(input.hasNext()){
				String synset = input.next();
				if(synset.endsWith("n")){
					String[] synp= synset.split("-");
					input.next();
					String label = input.nextLine().trim();
					if(!model.containsKey(label)){
						model.put(label, new ArrayList<Long>());
					};
					model.get(label).add(Long.valueOf(synp[0]));
				}else{
					input.nextLine();
				}
			}
			input.close();
		} catch (FileNotFoundException e) {e.printStackTrace();}
		return model;
	}
	
	/*************************************************/
	/**
	 * guarda los syssets en binario
	 */
	private static void saveWordnetSynsetsToBinary(String dest, HashMap<String, List<Long>> model){
		try {
			File d = new File(dest); d.getParentFile().mkdirs();
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(d));
			oos.writeObject(model); oos.close();
		} catch (Exception e) {e.printStackTrace();}
	}
	
	
	
	/**
	 * generamos la hastable que se usará para acceder a los synsets
	 */
	public static void generateWnetHashtable(String source, String dest){
		HashMap<String, List<Long>> model = loadWordnetSynsetsFromText(source);
		saveWordnetSynsetsToBinary(dest,model);
	}
	
}
