//iubio/readseq/AcedbSeqFormat.java
//split4javac// iubio/readseq/CommonSeqFormat.java date=04-Jun-2003

// iubio.readseq.CommonSeqFormat.java -- was seqread1.java
// low level readers & writers : sequential formats
// d.g.gilbert, 1990-1999

package iubio.readseq;
//package iubio.readseqF;

import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;

import flybase.OpenString;
import flybase.Debug;

import Acme.Fmt;
		 
//import iubio.readseq.*;
	// interfaces
import iubio.readseq.BioseqReaderIface;
import iubio.readseq.BioseqWriterIface;
import iubio.readseq.BioseqDoc;

import iubio.bioseq.BaseKind;
import iubio.bioseq.SeqInfo;
import iubio.bioseq.Bioseq;
import iubio.bioseq.BioseqFiled;

	// can we do w/o these?
import iubio.readseq.SeqFileInfo;
import iubio.readseq.GenbankDoc;
import iubio.readseq.EmblDoc;
import iubio.readseq.SwissDoc;
	
import iubio.readseq.BioseqFormat;
import iubio.readseq.BioseqWriter;

	
//========= sequential BioseqReader subclasses ==========

//split4javac// iubio/readseq/CommonSeqFormat.java line=1541
public class AcedbSeqFormat extends BioseqFormat
{
	public String formatName() { return "ACEDB"; }  
	public String formatSuffix() { return ".ace"; } 
	public String contentType() { return "biosequence/acedb"; } 
	public BioseqReaderIface newReader() { return new AcedbSeqReader(); }
	public BioseqWriterIface newWriter() { return new AcedbSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.indexOf(" : ")>0 && line.indexOf('"')>0) {
      formatLikelihood = 70;
      if (recordStartline==0) recordStartline= atline;
      if ( line.startsWith("DNA : ") 
      	|| line.startsWith("Peptide : ") 
      	|| line.startsWith("PROTEIN : ")) return true;
      else return false;  
      }
    else
    	return false;
	}

}

/*
acedb> find dna
acedb> show -c 2 -a

DNA : "2L52"
         cctaagcctaagcctaaaatagtgactctggcagttctctaaaataagtg
         actctggcagttcaccaaaaattgtgactctgaccgttcaccaaaaatag
         aaaagtacttctggatatctacagtgcgaagaaaatgccaaa

DNA : "3R5"
         ttttcaccgctcgagtgtcgtcttgctgttgcttgtaaattccaagatga

acedb> find peptide
acedb> show -c 2 -a

Peptide : "SW:2ABG_RABIT"
MGEDTDTRKINHSFLRDHSYVTEADIISTVEFNHTGELLATGDKGGRVVI
FQREPESKNAPHSQGEYDVYSTFQSHEPEFDYLKSLEIEEKINKIKWLPQ
SVIMTGAYNNFFRMFDRNTKRDVTLEASRESSKPRAVLKPRRVCVGGKRR
RDDISVDSLDFTKKILHTAWHPAENIIAIAATNNLYIFQDKVNSDVH

Peptide : "SW:2ACA_HUMAN"
MAATYRLVVSTVNHYSSVVIDRRFEQAIHYCTGTCHTFTHGIDCIVVHHS
VCADLLHIPVSQFKDADLNSMFLPHENGLSSAEGDYPQQAFTGIPRVKRG

*/

//public
class AcedbSeqReader  extends BioseqReader
{
	public AcedbSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		}

	public boolean endOfSequence() {
	  return (nWaiting == 0 || getreadbuf(0) == '\n' || getreadbuf(0) == '\r');
		}

	protected void read() throws IOException
	{
	  while (!allDone) {
	    int at= sWaiting.indexOf(":");
	    if (at>=0) seqid= sWaiting.substring(at+1).trim().toString();
	    else if (nWaiting > 0) seqid= sWaiting.trim().toString(); //?
			if (seqid!=null) {
				at= seqid.indexOf('"'); int e= seqid.lastIndexOf('"'); 
				if (at>=0 && e>at) seqid= seqid.substring(at+1,e); 
				}
				
	    readLoop();
	    if (!allDone) {
	    	while (! (endOfFile() || ( nWaiting > 0 && sWaiting.indexOf(":")>=0 ) ) )
	        getline();
	      }
	    if (endOfFile()) allDone = true;
	  }
	}
};


//public
class AcedbSeqWriter extends PearsonSeqWriter // BioseqWriter
{
	//final static int seqwidth= 60;  

	public void writeRecordEnd() { writeln(); }

	//public void writeSeq() { super.writeSeq(); } // same as fasta		
		
	public void writeDoc() {
    if (bioseq.getSeqtype() == Bioseq.kAmino)  writeString("Peptide : ");
    else writeString("DNA : ");
		writeln( '"' + idword + '"' );
		//writeln( seqid + "  " + seqlen + " bases  " + checksumString());
		}
		
};



		
 