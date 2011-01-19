package iubio.readseq;

//import java.util.Hashtable;
import java.io.*;

import iubio.bioseq.BaseKind;
//import iubio.bioseq.SeqRange;

import flybase.FastProperties;
import flybase.AppResources;
import flybase.Debug;

public class ToAminoBase extends OutBiobase
{
	public static String codontable="codon";
	
	static FastProperties codons= null;
	int i= 0;
	char acodon[] = new char[] {'X','X','X'};
	
	public ToAminoBase( OutBiobaseIntf nextout) { 
		super(nextout); 
	  if (codons==null) readCodons();  
		}
		
	public int outSeqChar(int c) { 
		if (outtest!=null) c= outtest.outSeqChar(c);   
		//? if (c < 'A' || c > 'z') return c;
		//? if (c <= ' ' || c > 'z') return c;
		if (c < 0) { i=0; return 0; } // need end of seq flag to reset counter
		int b= i % 3; i++;
		acodon[b] = Character.toUpperCase( (char)c); 
	  if (b != 2) return 0; 
	  String aminos= codons.getProperty(new String(acodon));
    if (aminos==null) return 'x'; // fixme
		return BaseKind.Amino321(aminos); 
  }

	void readCodons()
	{
		if (codons==null) 
		try {
				// want these names sorted ! - are sorted in file...so read file not as properties...
			codons= new FastProperties();
	  	String pname= System.getProperty( codontable, codontable);
	  	pname= AppResources.global.findPath(pname + ".properties");
			Debug.println("codontable: " + pname);
			InputStream ins= AppResources.global.getStream( pname);
			if (ins!=null) { codons.load(ins); ins.close(); }
			}
		catch (Exception e) {
		  e.printStackTrace();
		  }
    }

}
  