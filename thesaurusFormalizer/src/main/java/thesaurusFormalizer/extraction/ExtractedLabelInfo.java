package thesaurusFormalizer.extraction;

import java.util.List;

/**
 * registro para contener la información relevante
 */
public class ExtractedLabelInfo {
	private String uri;
	private String label;
	private String lang;
	private List<String> labelVariants;
	
	public ExtractedLabelInfo(String uri, String type, String lang) {
		this.uri = uri; this.label = type; this.lang = lang;
	}
	
	public String getUri() {return uri;}
	public String getLabel() {return label;}
	public String getLang() {return lang;}
	public List<String> getLabelVariants() {return labelVariants;}

	public void setUri(String uri) {this.uri = uri;}
	public void setLabel(String type) {this.label = type;}
	public void setLang(String lang) {this.lang = lang;}
	public void setLabelVariants(List<String> labelVariants){this.labelVariants = labelVariants;}
}
