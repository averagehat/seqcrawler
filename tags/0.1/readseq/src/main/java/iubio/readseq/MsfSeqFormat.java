//iubio/readseq/MsfSeqFormat.java
//split4javac// iubio/readseq/InterleavedSeqReader.java date=13-Jun-2003

// InterleavedSeqReader.java -- was seqread2.java
// low level readers & writers : interleaved formats
// d.g.gilbert, 1990-1999


package iubio.readseq;


import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;

import Acme.Fmt;
import flybase.Debug;
import flybase.OpenString;
//import flybase.Native;

import iubio.bioseq.BaseKind;
import iubio.bioseq.SeqInfo;
import iubio.bioseq.Bioseq;

// PaupSeqFormat reader needs work/test
// PrettySeqFormat not readable
// OlsenSeqFormat not readable yet

 

//split4javac// iubio/readseq/InterleavedSeqReader.java line=890
public class MsfSeqFormat extends BioseqFormat
{		
	public String formatName() { return "MSF";  }  
	public String formatSuffix() { return ".msf"; }  
	public String contentType() { return "biosequence/msf"; }  
	public boolean canread() { return true; }   
	public boolean canwrite() { return true; }  
	public boolean interleaved() { return true; }  

	public BioseqWriterIface newWriter() { return new MsfSeqWriter(); }		
	public BioseqReaderIface newReader() { return new MsfSeqReader(); }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		int m, t;
		
    if (line.startsWith("!!")) {
      // GCG version 9+ comment line
      // seen - !!NA_SEQUENCE  !!AA_SEQUENCE !!AAPROFILE  !!NAPROFILE  !!AA_MULTIPLE_ALIGNMENT !!NA_MUL...
      // !!RICH_SEQUENCE
   		if ( ( line.startsWith("!!NA") || line.startsWith("!!AA")) 
      	  && line.indexOf("MULTIPLE_ALIGNMENT") >= 0) {
        formatLikelihood += 92;  
      	return true;
      	}
      return false;  
      }
		else if ( (m= line.indexOf("MSF:")) >= 0 
		  && (t= line.indexOf("Type:", m)) > m 
		  && line.indexOf("Check:", t) > t ) {
	      formatLikelihood += 95;
        if (recordStartline==0) recordStartline= atline;
	      return true;
	      }
    else
    	return false;
	}
}

	// 28sep99 -failing ?
//public
class MsfSeqReader  extends InterleavedSeqReader //InterleavedSeqreader
{
	public MsfSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		testbase= new TestGcgBase();
		testbaseKind= kUseTester;
		//formatId= 15;
		}

	public boolean endOfSequence() {
		return false;
		}	

	//protected OpenString sid;

	protected void read() throws IOException
	{
  	int  at, iline= 0;
		OpenString  sid= new OpenString("");
	  boolean  done, indata= false;
	 	addit = (choice > 0);
	  if (addit) seqlen = 0;
	  seqlencount= 0;
	  do {
	    getline();
	    done = endOfFile();
	    if (sWaiting.startsWith("!!")) continue; // GCG version 9+ comment line
   		OpenString si= sWaiting;
   		int offset= 0;
   		
	    if (done && nWaiting==0) break;
	    
	   	else if (indata) {
	   		//?
	   		offset= 0;
    		while (offset<nWaiting && si.charAt(offset) <= ' ') offset++;
	      if ( offset<nWaiting ) {
        	int seqat;
        	for (seqat= offset; si.charAt(seqat) > ' '; seqat++) ;
	      	OpenString id= si.substring(offset,seqat).trim();
	       // Debug.println("msf at id=<"+id+"> match="+id.equals(sid));
	        if (id.equals(sid)) //sid.equals(id)) 
	        	addseq( getreadchars(), getreadcharofs()+seqat, nWaiting-seqat);   
	        iline++;
	        }	   	
	   		}
	   	
	    else if ( (at= si.indexOf("Name: ")) >= 0) {  
	    	// seqq header line 
	      // Name: somename      Len:   100  Check: 7009  Weight:  1.00 
	      //nseq++; 
	      atseq++; 
	      at += 6;
	      if (choice == kListSequences) 
	      	addinfo( si.substring(at).trim().toString());
	      else if (atseq == choice) {
	        seqid= si.substring(at).trim().toString(); 
	        int e;
	        for (e= 0; seqid.charAt(e)> ' '; e++) ;
	        sid= new OpenString( seqid.substring(0,e).trim());
	        //Debug.println("msf sid=["+sid+"]");
	        }
	      }

	    else if ( si.indexOf("//")>=0 ) {  
	      indata = true;
	      iline= 0;
				//if (nseq==0) {  }
				setNseq(atseq);
	      if (choice == kListSequences) done = true;
	      }
	      
	  } while (!done);
		allDone = true;
	}

};


//public
class MsfSeqWriter extends InterleavedSeqWriter
{
	protected String datestr;

	protected long calculateChecksum()
	{
		return GCGchecksum( bioseq, offset, seqlen);
	}

	protected void interleaf(int leaf) {
			// do after writing seqq names ??!? == writeDoc, but need to interleave names
		if (leaf==0) { writeln( "//"); } 
		writeln(); 
		}

	protected void interleaveHeader()
	{
		int checktotal = 0;
    if (datestr==null) {
  		SimpleDateFormat sdf= new SimpleDateFormat("MMM dd, yyyy  HH:mm"); //August 28, 1991  02:07
  		datestr= sdf.format(new Date());
  		}
		//foreach seq do
		//  int checksum= calculateChecksum(); // need checksum for all sequences -!?
		//checktotal= checksumTotal; // will have this after all are written!

    String stype;
    if (bioseq.getSeqtype() == Bioseq.kAmino)  { stype= "P"; writeString("!!AA"); }
    else { stype= "N"; writeString("!!NA"); }
		writeln("_MULTIPLE_ALIGNMENT"); // gcg9+ - leave out? - use AA_ for aminos ?
		writeln();
		writeString( " " + seqid + "  MSF: " + seqlen);
	  writeString( "  Type: "+stype+"  " + datestr );
	  writeln( "  Check: " +  checktotal + " ..");
	  writeln();
	}
	
	
	public void writeRecordStart()
	{
		super.writeRecordStart();
		setChecksum(true);
   	opts.spacer = 10;
    opts.nameleft = true;
    opts.namewidth= 15;  
   	opts.seqwidth= 50;
    opts.tab = 1; 
	}

	public void writeDoc()
	{
		super.writeDoc();
  	writeString(" Name: " + Fmt.fmt(idword, 16, Fmt.LJ));
		writeString(" Len:" + Fmt.fmt(seqlen, 6));
		writeString("  Check:" + Fmt.fmt(checksum, 5));
		writeln("  Weight:  1.00");
	}

};







