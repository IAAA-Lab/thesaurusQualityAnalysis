package tools.dataStructure;

import net.didion.jwnl.data.Synset;

/**
 * estructura para almacenar un sysntet y el sentido y nivel en el que se ha encontrado
 * respect de otro indicado
 */
public class SynsetPlusLevel implements Comparable<SynsetPlusLevel>{
	private Synset _synset; //synset encontrado
	private int _level; //nivel de antecesores desde el origen
	private int _branch;//sentido del original que tiene como antecesor el sinset incidado

    /**************************************************************/
	/**
	 * constructor de la clase
	 */
	public SynsetPlusLevel(Synset _synset, int _level, int _branch) {
		this._synset = _synset;
		this._level = _level;
		this._branch = _branch;
	}

    /**************************************************************/
    //metodos get y set
	public Synset get_synset() {
		return _synset;
	}
	
	public void set_synset(Synset _synset) {
		this._synset = _synset;
	}
	
	public int get_level() {
		return _level;
	}
	
	public void set_level(int _level) {
		this._level = _level;
	}
	
	public int get_branch() {
		return _branch;
	}

	public void set_branch(int _branch) {
		this._branch = _branch;
	}
	
    /**************************************************************/
	/**
	 * metodo de comparacion para poder ordenar una lista de esto
	 * compara por branch y si son del mismo branch por nivel en el branch
	 */
	public int compareTo(SynsetPlusLevel spl){
		Integer b1 = new Integer(_branch);
		Integer b2 = new Integer(spl.get_branch());
		if(b1!=b2){
			return b1.compareTo(b2);
		}
		return (new Integer(_level)).compareTo(new Integer(spl.get_level()));
	}
}
