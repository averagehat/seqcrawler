//iubio/readseq/GcgSeqFormat.java
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

//split4javac// iubio/readseq/CommonSeqFormat.java line=1355
public class  GcgSeqFormat extends BioseqFormat
{
	public String formatName() { return "GCG"; }  
	public String formatSuffix() { return ".gcg"; } 
	public String contentType() { return "biosequence/gcg"; } 
	public BioseqReaderIface newReader() { return new GcgSeqReader(); }
	public BioseqWriterIface newWriter() { return new GcgSeqWriter(); }
	public BioseqWriterIface newWriter(int nseqs) { 
		// can't write more than one seq in this format !
		if (nseqs>1)
			return BioseqFormats.newWriter(
				BioseqFormats.formatFromContentType("biosequence/msf"),nseqs);
		else 
			return new GcgSeqWriter();
		}
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
    if (line.startsWith("!!")) {
      // GCG version 9+ comment line
      //if (recordStartline==0) return true; //? only if 1st line?
      // seen - !!NA_SEQUENCE  !!AA_SEQUENCE !!AAPROFILE  !!NAPROFILE  !!AA_MULTIPLE_ALIGNMENT !!NA_MUL...
      // !!RICH_SEQUENCE
      if (line.indexOf("MULTIPLE_ALIGNMENT") >= 0) {
      	// not Gcg -> MSF
      	formatLikelihood = 0; //??
      	return false;
      	}
      else if ( line.startsWith("!!NA") 
      	|| line.startsWith("!!AA")) {
      	  formatLikelihood += 92; //??
          return true; // are no others possible?
          }
     	else if ( line.startsWith("!!RICH_SEQUENCE")) { // RSF - need parser ??
      	  formatLikelihood += 92; // fix me - parse it or need other handler !
          return true;  
          }
      formatLikelihood += 50; //??
      return false; //!?
      }
		else if (line.indexOf("..")>0 && line.indexOf("Check:")>0) {
      formatLikelihood += 80; //92; //??
      //if (recordStartline==0) recordStartline= atline;
      return false; //!?
      }
    else
    	return false;
	}
}

//public
class GcgSeqReader  extends BioseqReader
{
	public GcgSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		testbase= new TestGcgBase();
		testbaseKind= kUseTester;
		//formatId= 5;
		}

	public boolean endOfSequence() {
		return false;
		}

	protected void read() throws IOException
	{
    boolean gotuw= false;
    do {
      getlineBuf();
	    if (sWaiting.startsWith("!!")) continue; // GCG version 9+ comment line
     	gotuw = ( indexOfBuf("..")>=0);
    } while (!(gotuw || endOfFile()));
    if (gotuw) readUWGCG(); 
	}

	protected void readUWGCG()  throws IOException
	{
		atseq++;
		addit = (choice > 0);
	  if (addit) seqlen = 0;
		seqid= sWaiting.toString();
		int i;
	  if ((i = seqid.indexOf(" Length: "))>0) seqid= seqid.substring(0,i).trim();
	  else if ((i = seqid.indexOf(".."))>0) seqid= seqid.substring(0,i).trim();
		boolean done;
	  do {
	 		done = endOfFile();
	    getlineBuf(); // bad of endOfFile ??
	    if (sWaiting.startsWith("!!")) continue; // GCG version 9+ comment line
	    if (!done) addseq(getreadchars(), getreadcharofs()+margin, nWaiting - margin);
	  } while (!done);
	  if (choice == kListSequences) addinfo(seqid);
		allDone = true;
	}
	
 	
};



//public
class GcgSeqWriter  extends BioseqWriter
{
	protected String datestr;

	protected long calculateChecksum()
	{
		return GCGchecksum( bioseq, offset, seqlen);
	}

	public void writeRecordStart()
	{
		super.writeRecordStart();
		setChecksum(true);
		//testbase= new TestGcgBase(); testbaseKind= kUseTester;
		this.setOutputTranslation( new GcgOutBase( this.getOutputTranslation()));

		/*
		//dupSeqForOutput= true; // due to bioseq.replace()
		if (dupSeqForOutput) {
			Biobase[] bb= ((Bioseq) seqob).dup();
			this.bioseq= new Bioseq( bb);
			}
    if (BaseKind.indelHard != '.' && bioseq.indexOf( BaseKind.indelHard)>=0) {
    	bioseq= bioseq.clone();
    	String oldb= new String( { indelHard, indelSoft, indelEdge } );
    	String newb= new String( "..." );
    	bioseq.replace(seqlen, oldb, newb);  
    	//bioseq.replace(seqlen, BaseKind.indelHard, '.');   
			} */
   	opts.spacer = 10;
    opts.numleft = true;
	}
		
	//protected void writeRecordEnd() { writeln(); }
	
	
	public void writeDoc()
	{
		writeln( seqid );
		if (seqdoc instanceof BioseqDoc) {
			String title=  ((BioseqDoc)seqdoc).getTitle();
			if (title!=null) writeln( title );  
			}
    if (datestr==null) {
  		SimpleDateFormat sdf= new SimpleDateFormat("MMM dd, yyyy  HH:mm"); //August 28, 1991  02:07
  		datestr= sdf.format(new Date());
  		}
		writeString( "    " + idword + "  Length: " + seqlen );
		writeln( "  " + datestr + "  Check: " + checksum + "  ..");
	}
		
};

class TestGcgBase extends TestBiobase
{

	public int isSeqChar(int c) {
		//if (Character.isSpace((char)c) || Character.isDigit((char)c)) return 0;
		if (c<=' ' || (c >= '0' && c <= '9')) return 0;
		else {
	    if (c == '.') return '-';  //do the indel translate 
	    else return c;
	    }
		}		
}

class GcgOutBase extends OutBiobase
{
	public GcgOutBase( OutBiobaseIntf nextout) { super(nextout); }
	public int outSeqChar(int c) {
		if (outtest!=null) c= outtest.outSeqChar(c);   
		if (c==BaseKind.indelHard) return '.';
		else if (c==BaseKind.indelSoft) return '.';
		else if (c==BaseKind.indelEdge) return '.'; // for GCG-9/RSF, this should be ~
	 	else return c;
		}
}



