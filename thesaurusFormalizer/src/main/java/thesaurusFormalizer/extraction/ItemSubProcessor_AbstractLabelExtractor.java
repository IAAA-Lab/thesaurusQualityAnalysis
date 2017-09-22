package thesaurusFormalizer.extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;

import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

/**
 * copia la etiqueta completa al resultado
 * necesario si se queire intentar direct matching
 */
public abstract class ItemSubProcessor_AbstractLabelExtractor implements ItemProcessor<ExtractedLabelInfo, ExtractedLabelInfo>{
	
	//tagger a usar para part of speech
	protected POSTaggerME tagger; 
	
	/*******************************************************************************/
	/**
	 * copia la etiqueta completa al resultado
	 * necesario si se queire intentar direct matching
	 */
	public ExtractedLabelInfo process(ExtractedLabelInfo item) throws Exception {
		//preprocesamos la etiqueta eliminado elementos extra
		String label = preprocessLabels(item.getLabel());
		if(label==null || label.trim().length()==0){return item;}
		
		//partimos la frase en tokens y obtenemos su part of speech
		String[] tokens = SimpleTokenizer.INSTANCE.tokenize(label);
		List<String> tokensAsL = new ArrayList<String>(Arrays.asList(tokens));
		List<String> POStokensAsL = new ArrayList<String>(Arrays.asList(tagger.tag(tokens)));
		
		//generamos las diferentes variantes de la etiqueta a procesar
		List<String> extrNames = new ArrayList<String>();
		
		//añadimos la etiqueta original preprocesada
		extrNames.add("O\""+label);
		
		//si es una frase proposicional obtenemos su sujero
		int posprep = selectPropSubject(POStokensAsL,tokensAsL);
		if(posprep>=1){ // las frases no empiezan por preposición
			tokensAsL = tokensAsL.subList(0, posprep);
			POStokensAsL = POStokensAsL.subList(0, posprep);
			label=""; for(String s: tokensAsL){label += s+" ";} label = label.trim();
			extrNames.add("P\""+label);
		}
		
		//añadimos la etiqueta singular (la parte no proposicional)
		//se añade como otra original o preporcesada segun el caso
		String sing = stemPlural(label);
		if(!sing.equals(label) && posprep<1){
			extrNames.add("O\""+sing);
		}else if(!sing.equals(label) && posprep>=1){
			extrNames.add("P\""+sing);
		}
		
		//vamos eliminando adjetivos y de cada versión generamos su singular
		int pos; 
		while(POStokensAsL.size()>1 && (pos = posAdjective(POStokensAsL))>=0){
			tokensAsL.remove(pos);POStokensAsL.remove(pos);
			String noun=""; for(String s: tokensAsL){noun += s+" ";} noun = noun.trim();
			extrNames.add("N\""+noun);	
			String singu = stemPlural(noun);
			if(!singu.equals(noun)){extrNames.add("N\""+singu);}
		}
		
		item.setLabelVariants(extrNames);
		return item;
	}
	
	/**************************************************************/
	/**
	 * devolvemos la posicion de la proposicion o todo el tamaño si no esta
	 */
	private int selectPropSubject(List<String> POStokens, List<String> labels){
		for(int i=1;i< POStokens.size();i++){ //el primer token no puede ser preposicion
			if(isPreposition(POStokens.get(i),labels.get(i))){return i;}
		}	
		return -1;
	}
	
	/**************************************************************/
	/**
	 * selecciona el adjetivo menos significativo
	 */
	protected abstract int posAdjective(List<String> POStokensAsL);
	
	/**************************************************************/
	/**
	 * elegimos la parte principal de una frase proposicional o todo si no es proposicionasl
	 */
	protected abstract boolean isPreposition(String POStokens, String label);
	
	/***************************************************************/
	/**
	 * elimina el plural del texto indicado
	 */
	protected abstract String stemPlural(String texto);
	
	/***************************************************************/
	/**
	 * preprocesa las etiquetas para eliminar postoordinación, borrar parentesis y otros simpolos molestos
	 */
	private String preprocessLabels(String original) {
		String res = original;
		
		//si la etiqueta son siglas la borramos
		if(StringUtils.isAllUpperCase(res)){return null;}
		
		//borra parentesis
		if (res.contains("(")){res = res.substring(0, res.indexOf("("));}
		
		//borrar igual
		if (res.contains("=")){res = res.substring(0, res.indexOf("="));}
		
		//borra otros simbolos
		res = res.replaceAll("%", "");
		res = res.replaceAll("-", " ");
		res = res.replaceAll("\\.", " ");
			
		return res.trim().toLowerCase();
	}
}
