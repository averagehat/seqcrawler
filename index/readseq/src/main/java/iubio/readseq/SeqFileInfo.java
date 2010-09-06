//iubio/readseq/SeqFileInfo.java
//split4javac// iubio/readseq/SeqFileInfo.java date=06-Jun-2001

// SeqFileInfo.java
// d.g.gilbert 

package iubio.readseq;

import java.io.*;
import iubio.bioseq.*;
		 

/**
	* a lightweight class to pass seq and file info 
	* between reader/writer and others.  This lacks knowledge of
	* BioseqDoc (seqdoc).
	* Recommended you use BioseqRecord instead of this one.
	*/
	
//split4javac// iubio/readseq/SeqFileInfo.java line=17
public class SeqFileInfo
	implements Cloneable
{
	public static String gBlankSeqid = "nameless";
	public static String gMaskName = "#Mask";  // drop? obsolete?
	public static boolean gWriteMask= true; 	 // drop? obsolete?
	public static int gBlanknum= 0;

		// for file -- are these still needed here?
	public int format, skiplines, err;
	public int atseq, nseq;
	public long	modtime;

		// for current seq -- should be protected not public
	public int seqlen, offset;
	public boolean ismask= false, hasmask= false; // drop these? obsolete?
	public Object seq; 		// Bioseq -  ?don't want to be tied here to specific structure
	public Object seqdoc; // BioseqDoc mostly, always? 
	public String seqid = gBlankSeqid;
	public Object extradata; // TraceData, other?
	
	public SeqFileInfo() {}

	public SeqFileInfo( Object seq, int offset, int seqlen) { // Bioseq seq
		this.seq= seq;
		this.offset= offset;
		this.seqlen= seqlen;
		}

	public String toString() {
		StringBuffer sb= new StringBuffer( this.getClass().getName());
		sb.append(": id="); sb.append( seqid);
		sb.append(", length="); sb.append(seqlen);
		sb.append(", hasdoc="); sb.append(hasdoc());
		return sb.toString();
		}
		
		// accessors 
	public final int length() { return seqlen; }
	public void setlength(int len) { seqlen= len; }
	
	public final int offset() { return offset; }
	public void setoffset(int off) { offset= off; }

	public final Object getseqObject() { return seq; }
	public void setseq(Object seq) { this.seq= seq; }
	
	public final Object getdocObject() { return seqdoc; }
	public void setdoc(Object doc) { seqdoc= doc; }
	
	public final Object getextraObject() { return extradata; }
	public void setextra(Object doc) { extradata= doc; }
	
	public final boolean hasseq() { return (seq!=null && seqlen>0); }
	public final boolean hasdoc() { return (seqdoc!=null); }
	public final boolean hasid() { return (seqid!=null && seqid.length()>0 && (!seqid.startsWith(gBlankSeqid)) ); }
	public final boolean hasmask() { return (ismask || hasmask); }
	
	public final String getID() { return seqid; }
	public final void setSeqID(String seqid) { this.seqid= seqid; checkSeqID(); }
	
	public void clear() // drop big data
	{
	  seq= null;
	  seqdoc= null;
	  extradata= null;
	}
	
	public void checkSeqID() 
	{
 		this.ismask= false;
 		String sid= this.seqid;
 		if (sid!=null && sid.length() > 0) {
  		int at= sid.indexOf(gMaskName);
  		if (at>0) {
       	this.ismask= true;  
       	sid= sid.substring(0,at);
 				}
   		this.seqid= cleanSeqID(sid);
   		}
		}

	public static String getNextBlankID() { return gBlankSeqid+"_"+String.valueOf(++gBlanknum); }
	public static String getNextBlankID(String id) { return id+"_"+String.valueOf(++gBlanknum); }

	public static String cleanSeqID( String s) { 
		int i;
		s= s.trim();
	  if ((i= s.indexOf(' ')) > 0) s= s.substring(0,i);//? some folks like spaces in name?
		if ((i= s.indexOf(',')) > 0) s= s.substring(0,i);
		// replace all non-alphanumerics but '-' ?
		s= s.replace('\n','_').replace('\r','_').replace(' ','_').replace('.','_');
		return s;
  	}

	public void copyto(SeqFileInfo si) {
		si.format= format;
		si.skiplines= skiplines;
		si.err= err;
		si.atseq= atseq;
		si.nseq= nseq;
		si.modtime= modtime;
		si.seqlen= seqlen;
		si.offset= offset;
		si.ismask= ismask;
		si.hasmask= hasmask;
		si.seq= seq;
		si.seqdoc= seqdoc;
		si.seqid= seqid;
		}

	public Object clone() {
		try {
			SeqFileInfo si= (SeqFileInfo) super.clone();
			// reset the per-sequence fields
			si.seqid=  gBlankSeqid;
			si.ismask= si.hasmask= false;
			si.seqlen= 0;
			si.offset= 0;
			si.seq= null;
			si.seqdoc= null;
			//si.extradata= null; ??
			si.modtime= 0;
	    return si;
			}
		catch (CloneNotSupportedException ex) { throw new Error(ex.toString()); }
		} 
}



