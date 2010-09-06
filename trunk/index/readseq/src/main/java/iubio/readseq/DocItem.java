//iubio/readseq/DocItem.java
//split4javac// iubio/readseq/BioseqDocItems.java date=07-Jun-2001

// BioseqDocItems.java
// sequence format information (document,features) handlers
// d.g.gilbert, 1997++


package iubio.readseq;


import java.io.*;
import java.util.*;

import flybase.*;
import iubio.bioseq.*;

//split4javac// iubio/readseq/BioseqDocItems.java line=20
public class DocItem 
	implements Cloneable
///*java1.2*/, Comparable
{	
	//public static boolean bPreserveNewlines = true; // may01 test
	public static String sAppendBreak = "\n"; // may01 test - was " "
	
	public String name; 	// field name or key
	public String value;	// field value
	public int kind;			// field kind; a readseq constant where name changes per databank format
	public int level;			// level in document heirarchy: kField, kSubfield, kFeatField, kFeatCont of BioseqDocVals
		
	public DocItem() { this("", "", 0, 0); }
	public DocItem(DocItem p) { this(p.getName(), p.getValue(), p.getKind(), p.getLevel()); }
	public DocItem(String name, String value, int kind, int level) {
		this.kind= kind;
		this.level = level;
		this.name= name; 		//? ever null
		setValue(value); 		//this.value= value;	//? ever null
    }
    
	public final int getLevel() { return level; }
	public final void setLevel(int level) { this.level= level; }

	public final String getName() { return name; }
	public final int getKind() { return kind; }

	public final boolean hasValue() { return (value!=null && value.length()>0); }
	public String getValue() { return value; }
	public void setValue(String other) { this.value= (other==null ? "" : other); }

	public void appendValue(String appendval) { // can be called a lot w/ long docs
		appendValue( sAppendBreak, appendval);
		//if (appendval!=null) setValue( value + " " + appendval); //? space
		}
		
	static protected StringBuffer apbuf= new StringBuffer(); 
		// for efficiency, skip so many s + s -- static ? need to be thread-safe

	public void appendValue(String joinval, String appendval) { 
		if (appendval!=null) { 
			//if (apbuf==null) apbuf= new StringBuffer(value); else 
			{ apbuf.setLength(0); apbuf.append(value); }
			apbuf.append(joinval); 
			apbuf.append(appendval);
			setValue( apbuf.toString() );  
			}
		}

	public final boolean sameLevel(DocItem other) { return level == other.getLevel(); }
	public final boolean sameName(String other)  { return name.equals(other); }
	public final boolean sameName(DocItem other) { return name.equals(other.name); }
	public final boolean sameKind(int otherkind) { return kind == otherkind; }
	public final boolean sameKind(DocItem other) { return kind == other.kind; }
	public final boolean sameValue(String other)  { return value.equals(other); }

	public String toString() {
		StringBuffer sb= new StringBuffer();
		if (Debug.val()>1) { sb.append( this.getClass().getName()); sb.append(": "); }
		sb.append( name); sb.append("="); sb.append( value);
		if (Debug.val()>1) {
			sb.append(" [k="); sb.append( kind);
			sb.append(", l="); sb.append( level); sb.append("]");
			}
		return sb.toString();
		}
		
	public boolean equals(Object other) {
		if (other instanceof DocItem) {
			DocItem odoc= (DocItem) other;
			if (!sameKind(odoc.kind)) return false;
			if (!sameName(odoc.name)) return false;
			if (!sameValue(odoc.value)) return false;
			//return ( sameKind((DocItem)other) && super.equals((DocItem) other) );
			return true;
			}
		else return false;
		}

	public boolean sameNameOrKind(Object other) {
		if (other instanceof DocItem) 
			return ( sameKind((DocItem)other) && sameName((DocItem) other) );
		else if (other instanceof Integer) return sameKind(((Integer)other).intValue() );
		else if (other instanceof String) return sameName((String) other);
		else return false;
		}
	
		// java.lang.Comparable for sorting
	public int compareTo(Object ob) {
		int c= 0;
		if (name!=null) c= name.compareTo( ((DocItem)ob).name);
		if (c==0 && value!=null) c= value.compareTo( ((DocItem)ob).value);
		return c;
		}

	public Object clone() {
		try {
			DocItem c= (DocItem) super.clone();
	    return c;
			}
		catch(CloneNotSupportedException ex) { throw new Error(ex.toString()); }
		}
};


	/** trivial subclass to identify feature notes */
