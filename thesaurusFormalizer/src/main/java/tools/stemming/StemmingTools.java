package tools.stemming;

import java.io.StringReader;
import java.util.Arrays;

import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.AttributeFactory;

/**
 * metodos utiles de stemming
 */
public class StemmingTools {
	
	/*************************************************************************/
	/**
	 * realiza stemming de plurales en español, reduce los terminos a la raiz sin plurales
	 * reduce a forma comun singular y plural (quita a e y o)
	 */
	public static String stemSpanishPlurals(String text){
		String singular="";
		try{
			StandardTokenizer ts = new StandardTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY);
			ts.setReader(new StringReader(text));
			SpanishMinimalStemFilter pluralStemmer = new SpanishMinimalStemFilter(ts);
			pluralStemmer.reset();
			while (pluralStemmer.incrementToken()){
				CharTermAttribute attr = pluralStemmer.getAttribute(CharTermAttribute.class);	
				singular+=" "+ new String(Arrays.copyOfRange(attr.buffer(), 0, attr.length()));
			}
			pluralStemmer.close();
		}catch (Exception er){
			er.printStackTrace();
		}
		return singular.trim();
	}
	
	/*************************************************************************/
	/**
	 * realiza stemming de plurales en ingles, reduce los terminos a la raiz sin plurales
	 * quita los plurales perfectamente
	 */
	public static String stemEnglishPlurals(String text){
		String singular="";
		try{
			StandardTokenizer ts = new StandardTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY);
			ts.setReader(new StringReader(text));
			EnglishMinimalStemFilter pluralStemmer = new  EnglishMinimalStemFilter(ts);
			pluralStemmer.reset();
			while (pluralStemmer.incrementToken()){
				CharTermAttribute attr = pluralStemmer.getAttribute(CharTermAttribute.class);	
				singular+=" "+ new String(Arrays.copyOfRange(attr.buffer(), 0, attr.length()));
			}
			pluralStemmer.close();
		}catch (Exception er){
			er.printStackTrace();
		}
		return singular.trim();
	}
	
	/*************************************************************************/
	/**
	 * realiza stemming de plurales en frances, reduce los terminos a la raiz sin plurales
	 * reduce a forma comun singular y plural (quita a e y o)
	 */
	public static String stemFrenchPlurals(String text){
		String singular="";
		try{
			StandardTokenizer ts = new StandardTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY);
			ts.setReader(new StringReader(text));
			FrenchMinimalStemFilter pluralStemmer = new FrenchMinimalStemFilter(ts);
			pluralStemmer.reset();
			while (pluralStemmer.incrementToken()){
				CharTermAttribute attr = pluralStemmer.getAttribute(CharTermAttribute.class);	
				singular+=" "+ new String(Arrays.copyOfRange(attr.buffer(), 0, attr.length()));
			}
			pluralStemmer.close();
		}catch (Exception er){
			er.printStackTrace();
		}
		return singular.trim();
	}
	
}
