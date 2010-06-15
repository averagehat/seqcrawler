// BioseqDoc.java
// sequence format information (document,features) handlers
// d.g.gilbert, 1997++


package iubio.readseq;


import java.io.*;
import java.util.*;

import iubio.bioseq.*;
import flybase.*;
import Acme.Fmt;


		
public interface BioseqDoc
	extends BioseqDocVals
{
	public String getID();
	public String getTitle();
	public String getFieldName(int kind); //? change to keys - or enumerate keys?
	public String getDocField(int kind);  
	public String getBiodockey(String field); 

	public void addBasicName(String line);
	public void addDocLine(String line); 	
	public void addDocLine(OpenString line); 	
	public void addDocField(String field, String value, int level, boolean append);
				// ^^ drop leve, append from interface ?
				
	public FastVector documents(); 	//? change to enumeration?
	public FastVector features(); 	//?  ""
	
			//?? add these methdods to iface from Impl
	// setWantedFeatures(exfeatures);
	// SeqRange featsr= bdi.getFeatureRanges(seqlen);
	// replaceDocItem( BioseqDocVals.kSeqlen, ...);

}

