//iubio/readseq/FeatureItem.java
//split4javac// iubio/readseq/BioseqDocItems.java date=07-Jun-2001

// BioseqDocItems.java
// sequence format information (document,features) handlers
// d.g.gilbert, 1997++


package iubio.readseq;


import java.io.*;
import java.util.*;

import flybase.*;
import iubio.bioseq.*;

//split4javac// iubio/readseq/BioseqDocItems.java line=143
public class FeatureItem  extends DocItem
{
	//public static Font defont= new Font("SansSerif", 0, 9);
	protected SeqRange location;  
	protected FastVector notes; // of FeatureNote, "/name=value"
	protected DocItem curnote;
	    
	public FeatureItem(String name, SeqRange value ) {
		super(name, value.toString(), BioseqDoc.kFeatureItem,  BioseqDoc.kFeatField);
		location= value;
    }
	public FeatureItem(String name, SeqRange value, int level) {
		super(name, value.toString(), BioseqDoc.kFeatureItem, level);
		location= value;
    }
    
	public FeatureItem(String name, String value, int level) {
		super(name, value, BioseqDoc.kFeatureItem, level);
		setValue(value);  
    }

	public FeatureItem( ) {
		super("", "", BioseqDoc.kFeatureItem, BioseqDoc.kFeatField);
  	}
  	
	public void set(String name, SeqRange value) {
		this.name= name;
		setValue(value.toString()); //? ever null
    }

	public Object clone() { 
 		FeatureItem fi= (FeatureItem) super.clone();  
 		if (notes!=null) fi.notes= (FastVector) notes.clone(); // doesn't clone contents !
 		//fi.notes.cloneItems();
 		if (location!=null) fi.location= (SeqRange) location.clone();
 		fi.curnote= null; //?
 		//? featkind?
 		return fi;
		}


	public boolean equals(Object ob) {
		if (ob instanceof FeatureItem) {
			FeatureItem fi= (FeatureItem) ob;
			return (getName().equals(fi.getName()) && location.equals(fi.getLocation()));
			}
		return false;
		}
		
			// override
	public String getValue() { return getLocationString(); } 

	public void setValue(String newval) {
		super.setValue(newval);
		try { location= SeqRange.parse(value); }
		catch (SeqRangeException sre) { System.err.println(sre.getMessage()); } // (sre)
		}

	public void appendValue(String appendval) {
		super.appendValue( "", appendval);
		//if (appendval!=null) setValue( this.value + appendval); //? no space
		}
		
	public SeqRange getLocation() { return location; }
	public String getLocationString() {  return location.toString(); }
	
	public void updateRange(int changeflags, int start, int length, byte[] changes) {
 		//public final static int kDelete = 1, kInsert = 2, kReorder = 4, kChange = 8;
 		location.updateRange(changeflags, start, length, changes);
		}

		// java.lang.Comparable for sorting
	public int compareTo(Object ob) {
		int c= 0;
		if (location!=null) c= location.compareTo(((FeatureItem)ob).location);
		if (c == 0) c= super.compareTo(ob);
		return c;
	}

	public FastVector getNotes() { return notes; }
		
	public String getNotesText() { 
		StringBuffer sb= new StringBuffer();
		//if (Debug.isOn) { sb.append( this.getClass().getName()); sb.append(": "); }
		//sb.append( getName()); sb.append("="); sb.append( getLocationString());
		sb.append( getName()); sb.append(" loc="); sb.append( getLocationString());
		sb.append('\n'); //?
		if (notes!=null) for (int i=0; i<notes.size(); i++) {
			DocItem di= (DocItem) notes.elementAt(i);
			sb.append(di.getName());
			sb.append('=');
			String v= di.getValue();
			if (v.indexOf(' ')>=0) {
			  String q="'"; if (v.indexOf("'")>=0) q="\""; 
			  v= q+v+q;
			  }
			sb.append(v);
			sb.append('\n');
			}
		return sb.toString();
		}
		
		//? need getNotesText opposite setNotesText()
	public void setNotesText(String notetext, boolean append) 
	{
		if (!append && notes!=null) notes.removeAllElements();
		StringTokenizer st= new StringTokenizer(notetext,"\n\r");
		while (st.hasMoreTokens()) { 
			String kv= st.nextToken();
			int eq= kv.indexOf('=');
			if (eq>0) {
				String key= kv.substring(0,eq).trim();
				String val= kv.substring(eq+1).trim();
				if ("name".equals(key)) {
					name= val; //? allow name change?
					}
				else if ("location".equals(key)) {
					setValue(val);
					}
				else {
					if (!key.startsWith("/")) key= "/" + key;
					putNote( new FeatureNote( key, val)); 
					}
				}
			}
	}
	 
		
	public void putNote(DocItem note) { 
		if (notes==null) notes= new FastVector();
		notes.addElement(note); 
		curnote= note;
		}
		
	public void appendNote(String value) { 
		if (curnote!=null && value!=null) {
			if ("/translation".equals(curnote.getName())) 
				curnote.appendValue( "", value.trim()); // special hack - keep spaces out of this
			else
				curnote.appendValue( curnote.sAppendBreak, value); 
		//	String curval= curnote.getValue();
		//	if (!"/translation".equals(curnote.getName())) curval += " "; // special hack - keep spaces out of this
		//	curnote.setValue( curval + value); //? space
			}
		}
		
  public DocItem getNote(String name) {
  	if (notes==null) return null;
		String n2= (name.startsWith("/")) ? null : "/" + name;
  	for (int i=0; i<notes.size(); i++) {
  		DocItem nv= (DocItem) notes.elementAt(i);
   		if (nv.sameName(name) ) return nv;
 			else if (n2!=null && nv.sameName(n2) ) return nv;
  		}
  	return null;
  	}
  	
  public String getNoteValue(String name) {
  	DocItem di= getNote(name);
  	if (di!=null) return di.getValue(); else return null;
  	}
  	
	public String toString() {
		StringBuffer sb= new StringBuffer();
		if (Debug.val()>1) { sb.append( this.getClass().getName()); sb.append(": "); }
		sb.append( name); sb.append("="); sb.append( value);
		if (notes!=null) for (int i=0; i<notes.size(); i++) {
			sb.append('\t'); sb.append( notes.elementAt(i));
			}
		return sb.toString();
		}


};




