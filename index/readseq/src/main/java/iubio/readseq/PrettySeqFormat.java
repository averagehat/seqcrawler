//iubio/readseq/PrettySeqFormat.java
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

 

//split4javac// iubio/readseq/InterleavedSeqReader.java line=1445
public class PrettySeqFormat extends BioseqFormat
{		
	public String formatName() { return "Pretty";  }  
	public String formatSuffix() { return ".pretty"; }  
	public String contentType() { return "biosequence/pretty"; }  

	public boolean canread() { return false; }   //? not readable? - fix
	public boolean canwrite() { return true; }  
	public boolean interleaved() { return true; }  

	public BioseqWriterIface newWriter() { return new PrettySeqWriter(); }		
	//public BioseqReaderIface newReader() { return new PrettySeqReader(); }

}


//public
class PrettySeqReader  extends InterleavedSeqReader //InterleavedSeqreader
{
	public PrettySeqReader() {
		margin	=  0;
		addfirst= true;
		addend 	= true;  
		ungetend= false;
		//formatId= 18;
		}
};


//public
class PrettySeqWriter extends InterleavedSeqWriter
{
	boolean firstseq;
	int interline;
	
	public void writeRecordStart() 			// per seqq
	{
		super.writeRecordStart(); // super does opts.plainInit()
		//if (!opts.userchoice) opts.prettyInit();
		if (firstseq) { opts.numwidth= Fmt.fmt(seqlen).length() + 1; firstseq= false; }
	}
		
	public void writeHeader()  throws IOException			// per file
	{ 
		super.writeHeader();
		if (!opts.userchoice) opts.prettyInit();
		interline= opts.interline;
		firstseq= true;
		if (opts.numtop) {
			// seqlen must be set to min/max seqlen
			opts.numline = 1;
			if (interleaved()) fileIndex.indexit(); 
			writeSeq(); // write number line (numline==1)
			opts.numline = 2;
			if (interleaved()) fileIndex.indexit(); 
			writeSeq(); // write tic line (numline==2)
			opts.numline = 0;
			}
	}

	protected void interleaf(int leaf) {
		for (int i= interline; i>0; i--) writeln(); // is newline legal for phylip here?
		}
	
	public void writeTrailer()  // per file 
	{ 
		if (opts.numbot) {
			opts.numline = 2;
			if (interleaved()) fileIndex.indexit();  
			writeSeq(); // write tic line (numline==2)
			opts.numline = 1;
			if (interleaved()) fileIndex.indexit(); 
			writeSeq(); // write number line (numline==1)
			opts.numline = 0;
			}
		super.writeTrailer();
	}
		
};



