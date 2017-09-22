package tools.stemming;

public class FrenchMinimalStemmer {

	 public int stem(char s[], int len) {
	    if (len < 4)
	      return len;
	    
	    if (s[len-1] == 'x') {
	      return len - 1;
	    }
	    
	    if (s[len-1] == 's') return len - 1;
	    return len;
	}
}
