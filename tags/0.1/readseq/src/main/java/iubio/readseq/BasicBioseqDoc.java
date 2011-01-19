//iubio/readseq/BasicBioseqDoc.java
//split4javac// iubio/readseq/BasicBioseqDoc.java date=26-May-2001

// iubio.readseq.BasicBioseqDoc.java
// sequence format information (document,features) handlers
// d.g.gilbert, 1997++


package iubio.readseq;


import java.io.*;
import java.util.*;

import iubio.bioseq.*;
import flybase.*;


	// convenience class for programmers
//split4javac// iubio/readseq/BasicBioseqDoc.java line=17
public class BasicBioseqDoc extends BioseqDocImpl
{
	public static String xprop= "XmlDoc"; 
	private static FastHashtable elabel2keys = new FastHashtable();  // format label => biodockey
	private static FastProperties	keys2elabel= new FastProperties();  // biodockey => format label
	
	static { 
  	String pname= System.getProperty( xprop, xprop);
  	getDocProperties(pname,keys2elabel,elabel2keys);
		}

	public BasicBioseqDoc() { }
	
	public BasicBioseqDoc(BioseqDoc source) {
		super(source);  
		fFromForeignFormat = !(source instanceof BasicBioseqDoc);
		}
		
	public BasicBioseqDoc(String idname) { 
		super(); 
		addBasicName( idname);
		}

	public BasicBioseqDoc(Object source) {
    super();
		if (source instanceof BasicBioseqDoc) {
		  setSourceDoc((BasicBioseqDoc)source);
		  fFromForeignFormat = !(source instanceof BasicBioseqDoc);
      }
		else if (source instanceof String) 
		  addBasicName((String)source);
		}

	public String getBiodockey(String field) { return (String) elabel2keys.get(field); }
	public String getFieldName(int kind) 
	{ 
		String lab= null;  
		String biodockey= getBiodockey(kind);
		if (biodockey!=null) lab= (String) keys2elabel.get( biodockey);
		return lab;
		} 
		
	public  void addDocLine(String line) { //abstract
		throw new Error("Cant add doc line --  use GenbankDoc/EmblDoc instead"); 
		}


}
