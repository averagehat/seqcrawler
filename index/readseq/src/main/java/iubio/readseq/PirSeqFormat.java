//iubio/readseq/PirSeqFormat.java
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

//split4javac// iubio/readseq/CommonSeqFormat.java line=673
public class PirSeqFormat extends BioseqFormat
{
	public String formatName() { return "PIR|CODATA"; }  
	public String formatSuffix() { return ".pir"; } 
	public String contentType() { return "biosequence/codata"; } 
	public BioseqReaderIface newReader() { return new PirSeqReader(); }
	public BioseqWriterIface newWriter() { return new PirSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }
	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.startsWith(PirSeqReader.kEntry)) {
      formatLikelihood += 80;
      if (recordStartline==0) recordStartline= atline;
      return false;
      }
    else if (line.startsWith(PirSeqReader.kSequence)) {
      formatLikelihood += 70;
      return false;
      }
    else if (line.startsWith("///")) {
      formatLikelihood += 20;
      return false;
      }
    else
    	return false;
	}
}

//public
class PirSeqReader  extends BioseqReader
{
	final static String kEntry = "ENTRY ";
	final static String kSequence = "SEQUENCE";
	
	public PirSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//formatId= 14;
		}


	public boolean endOfSequence() {
		ungetend= (indexOfBuf(kEntry) == 0);
	  return (ungetend || indexOfBuf("///") >= 0);
		}

	protected void read() throws IOException
	{  
	  while (!allDone) {
	    while (!(endOfFile() || sWaiting.startsWith(kSequence) 
	    	|| sWaiting.startsWith(kEntry)
	    	)) getline();
	    if (nWaiting > 16) seqid= sWaiting.substring(16).toString();
	    while (!(endOfFile() || sWaiting.startsWith(kSequence)))
	    	getline();
	    readLoop();
			if (!allDone) {
	    	while (!(endOfFile() || (nWaiting > 0 && indexOfBuf(kEntry) == 0)))
	        getline();
	      }
	    if (endOfFile()) allDone = true;
	  }
	}

	
};


//public
class PirSeqWriter  extends BioseqWriter
{

	public void writeRecordStart()
	{
		super.writeRecordStart();
    opts.numwidth = 7;
    opts.seqwidth= 30;
    opts.spacer = kSpaceAll;
    opts.numleft = true;
	}
			
	public void writeHeader()  throws IOException { 
		super.writeHeader();
		writeln( "\\\\\\"); 
		}

	public void writeRecordEnd() { writeln("///"); }
	
	public void writeDoc()
	{
   	// somewhat like genbank...  
		writeString("ENTRY           ");
		writeString(idword);
		writeln(" ");

		writeString("TITLE           ");
		String title= seqid;
		if (seqdoc instanceof BioseqDoc) {
			String t= ((BioseqDoc)seqdoc).getTitle();
			if (t!=null) title= t;
			}
		writeString( title);
		writeString(" ");
   	writeString( String.valueOf(seqlen));
		writeString(" bases  ");
  	writeln(checksumString());
		
		writeln( "SEQUENCE        ");
    //run a top number line for PIR 
    int j;
    for (j=0; j<opts.numwidth; j++) writeByte(' ');
    for (j=5; j<=opts.seqwidth; j += 5) writeString( Fmt.fmt( j, 10));
    writeln();  
    //linesout += 5;
	}
	
	
};



