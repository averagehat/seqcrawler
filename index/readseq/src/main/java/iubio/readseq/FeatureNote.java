package iubio.readseq;

/** trivial subclass to identify feature notes */
public class FeatureNote extends DocItem
{
	public FeatureNote() { super(); }
	public FeatureNote(DocItem p) { 
		super(p); 
		this.kind= BioseqDocVals.kFeatureNote; 
		this.level= BioseqDocVals.kFeatCont; 
		}
	public FeatureNote(String name, String value) {
		this(name,value,BioseqDocVals.kFeatureNote,BioseqDocVals.kFeatCont);
    }
	public FeatureNote(String name, String value, int kind, int level) {
		//? force name to start with '/' ???
		super(name,value,kind,level);
    }

	/*
	public String toString() {
		StringBuffer sb= new StringBuffer();
		if (Debug.isOn) { sb.append( this.getClass().getName()); sb.append(": "); }
		sb.append( name); sb.append("="); sb.append( value);
		return sb.toString();
		}
	*/
}