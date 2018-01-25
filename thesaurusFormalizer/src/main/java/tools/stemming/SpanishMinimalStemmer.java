package tools.stemming;

public class SpanishMinimalStemmer {

	 public int stem(char so[], int len) {
		 char s[] = so.clone();
		    if (len < 5)
		      return len;
		    
		    for (int i = 0; i < len; i++)
		      switch(s[i]) {
		        case '�': 
		        case '�': s[i] = 'a'; break;		     
		        case '�':
		        case '�': s[i] = 'o'; break;
		        case '�':
		        case '�': s[i] = 'e'; break;
		        case '�':
		        case '�': s[i] = 'u'; break;
		        case '�':
		        case '�': s[i] = 'i'; break;
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
