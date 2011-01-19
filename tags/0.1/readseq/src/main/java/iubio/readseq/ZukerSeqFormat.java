//iubio/readseq/ZukerSeqFormat.java
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

//split4javac// iubio/readseq/CommonSeqFormat.java line=1185
public class ZukerSeqFormat extends BioseqFormat
{
	public String formatName() { return "Zuker"; }  
	public String formatSuffix() { return ".zuker"; } 
	public String contentType() { return "biosequence/zuker"; } 
	public BioseqReaderIface newReader() { return new ZukerSeqReader(); }
	//public BioseqWriterIface newWriter() { return new ZukerSeqWriter(); }
	public boolean canread() { return false; }
	public boolean canwrite() { return false; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.length()>0 && line.charAt(0) == '(') {
      formatLikelihood= 25;
      if (recordStartline==0) recordStartline= atline;
      return false; //!
      }
    else
    	return false;
	}
}

//public
class ZukerSeqReader  extends BioseqReader
{
	public ZukerSeqReader() {
		margin	=  0;
		addfirst= true;
		addend 	= true;  
		ungetend= false;
		//formatId= 9;
		}
};



