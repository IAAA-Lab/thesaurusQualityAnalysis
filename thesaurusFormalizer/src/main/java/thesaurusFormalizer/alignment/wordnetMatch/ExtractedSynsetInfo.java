package thesaurusFormalizer.alignment.wordnetMatch;

import java.util.List;

/**
 * registro para contener la información relevante
 */
public class ExtractedSynsetInfo {
	private String uri;
	private String labels;
	private String lang;
	private String type;
	private List<String> alignments;
	
	public ExtractedSynsetInfo(String uri, String labels, String lang) {
		this.uri = uri; this.labels = labels; this.lang = lang;
	}
	
	public String getUri() {return uri;}
	public String getLabels() {return labels;}
	public String getLang() {return lang;}
	public List<String> getAlignments() {return alignments;}
	public String getType() {return type;}
	
	public void setUri(String uri) {this.uri = uri;}
	public void setLabels(String labels) {this.labels = labels;}
	public void setLang(String lang) {this.lang = lang;}
	public void setAlignments(List<String> synsets){this.alignments = synsets;}
	public void setType(String type) {this.type = type;}
}
