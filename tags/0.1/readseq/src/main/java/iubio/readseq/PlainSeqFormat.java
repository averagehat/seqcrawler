//iubio/readseq/PlainSeqFormat.java
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

//split4javac// iubio/readseq/CommonSeqFormat.java line=41
public class PlainSeqFormat extends BioseqFormat
{
	public String formatName() { return "Plain|Raw"; }  
	public String formatSuffix() { return (reallyRaw) ? ".raw" : ".seq"; }  
	public String contentType() { return  (reallyRaw) ? "biosequence/raw" : "biosequence/plain"; } 
	//public BioseqReaderIface newReader() { return new PlainSeqReader(); }
	public BioseqWriterIface newWriter() { return new PlainSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }

	protected int nnewlines;
	protected boolean reallyRaw;
	protected SeqInfo seqkind;
	public void setSeqInfoTester(SeqInfo si) { seqkind= si; }

	public void setVariant(String varname) {
		if ("raw".equalsIgnoreCase(varname)) reallyRaw= true;
		// format tester cant distinguish multi-record raw and line-broken plain sequence
		}

	public void formatTestInit() { 
		super.formatTestInit(); 
		nnewlines= 0;
		reallyRaw= false;
		seqkind= null;
		}

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		nnewlines= atline;
		/*  // dang - caller eats newlines from line !
		if (nnewlines < 2 && line.length()>0) {
			char endc= line.charAt( line.length()-1 );
			if ( endc == '\n' || endc == '\r' ) nnewlines++; 
			else if (atline > 0 && nnewlines>0) nnewlines++; // partial 2nd line
			}
		*/
 		return false;
	}

	public int formatTestLikelihood() { 
		if (seqkind!=null) {
			if (seqkind.getKind() == SeqInfo.kOtherSeq) reallyRaw= false;
			else reallyRaw= (nnewlines < 2);  
			}
		return formatLikelihood; 
		}

	public BioseqReaderIface newReader() {
		if (reallyRaw) return new VeryRawSeqReader(); 
		else return new PlainSeqReader();
		}

}

//public
class PlainSeqWriter  extends BioseqWriter
{
	public void writeRecordEnd() { if (!isoneline) writeln(); }  // writeloop does at least one newline at end of seq

	public void setOpts(WriteseqOpts newopts) { 
		if (newopts!=null) {
			// pick only relevant opts .seqwidth
			opts.seqwidth= newopts.seqwidth;
			}
		}
		
	// simple oneline seq output ?
	protected final int kMaxlongbuf = 2048;
	protected boolean isoneline;
	
	protected void writeLoop() // per sequence
	{
		if (opts.seqwidth < 9999 && opts.seqwidth > 0) // so width=0 width=-1 width=9999+ will make 1 line
			super.writeLoop();
		else { 
			isoneline= true;
			int bioseqlen= Math.min(offset+seqlen, bioseq.length());
			char[]  bs= new char[kMaxlongbuf];
		  for (int i=0, bufl=0 ; i < seqlen; ) {
	 	    if (l1 < 0) l1 = 0;
		    l1++; 
	      char bc;
	      if (offset+i>=bioseqlen) bc= BaseKind.indelEdge;   
	      else bc= (char)bioseq.base(offset+i,fBasePart); 
	      i++; 
	      if (testbaseKind == kUseTester) bc= (char) testbase.outSeqChar( bc);
	      if (bc>0) bs[bufl++] = bc; 
	    	if (bufl >= kMaxlongbuf || l1 == opts.seqwidth  || i == seqlen) { //? no || l1 == opts.seqwidth 
	    	  int buflen= bufl;
	     	 	bufl = 0;  l1 = 0;
	      	writeByteArray( bs, 0, buflen);
		    	if (i == seqlen || l1 == opts.seqwidth) writeln(); //writeSeqEnd();   //  
		      }
		  	}
			}
	}
	
 		
};


// these two dont really need to be public, but dang javac splitter wont do unless told otherwise

