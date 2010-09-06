//iubio/readseq/FitchSeqFormat.java
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

//split4javac// iubio/readseq/CommonSeqFormat.java line=1148
public class FitchSeqFormat extends BioseqFormat
{
	public String formatName() { return "Fitch"; }  
	public String formatSuffix() { return ".fitch"; } 
	public String contentType() { return "biosequence/fitch"; } 
	public BioseqReaderIface newReader() { return new FitchSeqReader(); }
	//public BioseqWriterIface newWriter() { return new FitchSeqWriter(); }
	public boolean canread() { return false; }
	public boolean canwrite() { return false; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		int splen= line.length();
		boolean isfitch= true;
    for (int k=0; isfitch && (k < splen); k++) {
      if (k % 4 == 0) isfitch &= (line.charAt(k) == ' ');
      else isfitch &= (line.charAt(k) != ' ');
      }
    if (isfitch && (splen > 20)) formatLikelihood += 10;
    return false;
	}
	
}

//public
class FitchSeqReader  extends BioseqReader
{
	public FitchSeqReader() {
		margin	=  0;
		addfirst= true;
		addend 	= true;  
		ungetend= false;
		//formatId= 7;
		}
};


