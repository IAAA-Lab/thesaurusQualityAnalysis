package tools.stemming;

public class SpanishMinimalStemmer {

	 public int stem(char so[], int len) {
		 char s[] = so.clone();
		    if (len < 5)
		      return len;
		    
		    for (int i = 0; i < len; i++)
		      switch(s[i]) {
		        case 'Á': 
		        case 'á': s[i] = 'a'; break;		     
		        case 'Ó':
		        case 'ó': s[i] = 'o'; break;
		        case 'É':
		        case 'é': s[i] = 'e'; break;
		        case 'Ú':
		        case 'ú': s[i] = 'u'; break;
		        case 'Í':
		        case 'í': s[i] = 'i'; break;
		      }
		    
		    switch(s[len-1]) {
		      case 'o':
		      case 'a':
		      case 'e': return len;
		      case 's':
		        if (s[len-2] == 'e' && s[len-3] == 's' && s[len-4] == 'e')
		          return len-2;
		        if (s[len-2] == 'e' && s[len-3] == 'n' && s[len-4] == 'o')
			          return len-2;
		        if (s[len-2] == 'e' && s[len-3] == 'l')
			          return len-2;
		        if (s[len-2] == 'e' && s[len-3] == 'c') {
		          s[len-3] = 'z';
		          return len - 2;
		        }
		        if (s[len-2] == 'o' || s[len-2] == 'a' || s[len-2] == 'e')
		          return len - 1;
		    }
		    
		    return len;
		  }
	
}
