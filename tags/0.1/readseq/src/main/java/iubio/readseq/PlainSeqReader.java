//iubio/readseq/PlainSeqReader.java
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

//split4javac// iubio/readseq/CommonSeqFormat.java line=145
public class PlainSeqReader  extends BioseqReader
{
	// very simple format -- blank line b/n sequences, no seqid
	protected File inputFile;
	
	public PlainSeqReader() {
		margin	=  0;
		addfirst= true;
		addend 	= true;  
		ungetend= false;
		//formatId= 13;
		}
	
	public boolean endOfSequence() {
	  return (nWaiting == 0 || getreadbuf(0) == '\n' || getreadbuf(0) == '\r');
		}

	public void setInputFile(File inf) { inputFile= inf; }
	
	protected void read() throws IOException 
	{ 
		if (inputFile!=null) {
			seqid= SeqFileInfo.cleanSeqID(inputFile.getName());  
			seqid= SeqFileInfo.getNextBlankID(seqid);
			}
		super.read();
	}
	
}

// add VeryRawSeqFormat? - only bases, no newlines, formatting, id, ...
// or change plain to expect no newlines for single seq, mulitple lines == multiple entries

