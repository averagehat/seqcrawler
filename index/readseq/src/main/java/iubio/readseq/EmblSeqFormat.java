//iubio/readseq/EmblSeqFormat.java
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

//split4javac// iubio/readseq/CommonSeqFormat.java line=894
public class EmblSeqFormat extends BioseqFormat
{
	public String formatName() { return "EMBL|em"; }  
	public String formatSuffix() { return ".embl"; } 
	public String contentType() { return "biosequence/embl"; } 
	public BioseqReaderIface newReader() { return new EmblSeqReader(); }
	public BioseqWriterIface newWriter() { return new EmblSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }
	public boolean hasdoc() { return true; }
	
	protected boolean isAmino;
	
	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.startsWith(EmblSeqReader.kID)) {
      formatLikelihood += 80;
			if (line.indexOf(" AA.")>0) isAmino= true;
      if (recordStartline==0) recordStartline= atline;
      return false; //!?
      }
    else if (line.startsWith(EmblSeqReader.kAcc)) {
      formatLikelihood += 10;
      return false; //!
      }
    else if (line.startsWith(EmblSeqReader.kDesc)) {
      formatLikelihood += 10;
      return false; //!
      }
    else if (line.startsWith(EmblSeqReader.kSequence)) {
      formatLikelihood += 70;
      return false; //!?
      }
    else
    	return false;
	}

}

//public
class EmblSeqReader  extends BioseqReader
{
	final static String kID = "ID   ", kAcc= "AC   ", kDesc= "DE   ", kSequence = "SQ   ";
	EmblDoc doc;

	public EmblSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//formatId= 4;
		}
		
	//public BioseqDoc getInfo() { return doc; }

	public boolean endOfSequence() {
		ungetend= ( indexOfBuf(kID) == 0);
	  return (ungetend || indexOfBuf("//") >= 0);
		}

	protected void read() throws IOException
	{
		//sWaiting == PRT;   379 AA. << //? SwissDoc() if isamino
		if (sWaiting.indexOf(" AA.")>0) doc= new SwissDoc(); 
		else doc= new EmblDoc(); 
		if (skipdocs) doc.setSkipDocs(skipdocs);
		seqdoc= doc;
			
  	while (!allDone) {
			boolean adddoc = ((atseq+1) == choice); //!  skip doc if wanted; readLoop() increments atseq
			// readLoop() increments atseq !, sets addit for seq collecting
	  	if (adddoc) doc.addDocLine( sWaiting);
	    if (nWaiting > 5) seqid= sWaiting.substring(5).toString();

				// genbank reader does this way, other way leaves ? seqinfo in doc?
	  //  while (!(endOfFile() || sWaiting.startsWith( kSequence))) {
	  //		getline(); if (adddoc) doc.addDocLine(sWaiting);
	   //		}

	    do {
	  		getline(); if (adddoc) doc.addDocLine(sWaiting);
	    } while (!(endOfFile() || (sWaiting.startsWith(kSequence))));

	    readLoop();
	    
	    if (!allDone) {
	      while (!(endOfFile() || (nWaiting>0 && indexOfBuf(kID) == 0)))
	      	getline();
	    	}
	    if (endOfFile()) allDone = true;
	  }
	}

};


//public
class EmblSeqWriter  extends BioseqWriter
{
	final static int seqwidth= 60, ktab= 5, kspacer= 10, knumwidth= 8, knumflags= 0;  
	protected int seqkind;
	
	public void writeRecordStart()
	{
		super.writeRecordStart();
   	opts.tab = ktab;    
    opts.spacer = kspacer;  
   	opts.seqwidth = seqwidth;
    opts.numright = true;  
    opts.numwidth = knumwidth;  
    seqkind= bioseq.getSeqtype();
    if ( seqkind == SeqInfo.kAmino) {
			//BioseqWriter.gJavaChecksum= false;  // FIXME !
			//BioseqWriter.gShortChecksum= false;
    	setChecksum(true);
			}
	}
		
	public void writeRecordEnd() {  
		//BioseqWriter.gJavaChecksum= true;  // FIXME !
		writeln("//"); 
		}

	public void writeSeq() // per sequence
	{
		// writeLoop();
		int i, j, ib, nout= 0;
		int origin= opts.origin;
		boolean rev= opts.reversed;
		boolean newline= true;
		if (bioseq.isBytes() && testbaseKind != kUseTester) {
			byte[] ba= bioseq.toBytes();
			for (i= 0; i < seqlen; i++) {
				if (newline) {
					for (j=0; j<ktab; j++) writeByte(' '); 
					newline= false;
					}

				writeByte( ba[offset+i]); 
				if (i % seqwidth == seqwidth-1) { 
		    	if (rev) ib= origin-i; else ib= origin+i;
		    	writeString( "  " + Fmt.fmt( ib, knumwidth, knumflags));
					writeln(); newline= true; 
					}
				else if (i % kspacer == kspacer-1) writeByte(' ');
				}
			}
		else {
			for (i= 0; i < seqlen; i++) {
				if (newline) {
					for (j=0; j<ktab; j++) writeByte(' '); 
					newline= false;
					}

		   	char bc= bioseq.base(offset+i,fBasePart);
	      if (testbaseKind == kUseTester) bc= (char) testbase.outSeqChar( bc);
				if (bc>0) {
					writeByte( bc); 
					if ( (nout) % seqwidth == seqwidth-1) { 
		      	if (rev) ib= origin-nout; else ib= origin+nout;
			    	writeString( "  " + Fmt.fmt( ib, knumwidth, knumflags));
						writeln(); newline= true; 
						}
					else if ((nout) % kspacer == kspacer-1) 
						writeByte(' ');
					nout++;
					
					}
				}
			}
			
		if (!newline) {
			int tab= seqlen % seqwidth; 
			if (tab>0) {
				tab= seqwidth - tab;
				tab += (tab-1) / kspacer;
				}
			for (j=0; j<tab; j++) writeByte(' ');  // need to add missing spacers!
		  if (rev) ib= origin-seqlen-1; else ib= origin+seqlen-1;
			writeString( "  " + Fmt.fmt( ib, knumwidth, knumflags));
			writeln();
			}
  }
  
//ID   DMEST6A    standard; DNA; INV; 1754 BP.
//ID   EMBL       standard; DNA; UNC; 100 BP.
//ID   FASTA          STANDARD;      PRT;   100 AA.  << swissprot/amino
	protected void writeID() {
		writeString("ID   ");
		if (seqkind==SeqInfo.kAmino) {
			writeString( Fmt.fmt( idword, 15, Fmt.LJ));
			writeString(" STANDARD;      PRT; ");
	   	writeString( String.valueOf(seqlen));
			writeln(" AA.");
			}
		else {
			writeString( Fmt.fmt( idword, 11, Fmt.LJ));
			writeString(" standard; ");
			writeString( SeqInfo.getKindLabel(seqkind));  
			if (Debug.isOn) writeString("; debug");
			writeString("; UNC; ");
	   	writeString( String.valueOf(seqlen));
			writeln(" BP.");
			}
		}

	protected final void writeTitle() { writeString( "DE   "); writeln( seqid); }

//SQ   SEQUENCE   100 AA;  11907 MW;  5DB8D5B8 CRC32;    << swissprot/amino
//SQ   Sequence 400 BP; 104 A; 83 C; 130 G; 83 T; 0 other;

	protected void writeSeqStats(String cks) {	
		if (seqkind==SeqInfo.kAmino) {
			writeString( "SQ   SEQUENCE "); 
			writeString( Fmt.fmt( seqlen, 5, 0)); writeString(" AA;"); 
			writeString( Fmt.fmt( cks, 10, 0)); writeString(" CRC32;"); 
			writeln();
			}
		else {
			if (cks.length()>0) { writeString("CC   "); writeString(cks); writeln(" CRC32;"); }
			writeString( "SQ   Sequence "); writeString( String.valueOf(seqlen)); writeln(" BP;"); 
			}
		}

	public void writeDoc()
	{
		String cks= checksumString();
 		int at= cks.indexOf(" checksum"); if (at>0) cks= cks.substring(0,at);
		if (seqdoc instanceof BioseqDoc) {
			EmblDoc doc;
			if (seqkind==SeqInfo.kAmino) doc= new SwissDoc((BioseqDoc)seqdoc);
			else doc= new EmblDoc((BioseqDoc)seqdoc);
			boolean doid= true;
			String docid= doc.getID();
			if (docid==null || 
				(! idword.startsWith( SeqFileInfo.gBlankSeqid) && ! docid.equals(idword) )  )
					{ writeID(); doid= false; }
			if (!doid && doc.getTitle()==null) writeTitle(); // should have, but not before id
			doc.replaceDocField( doc.kSeqlen, String.valueOf(seqlen) );
			if (cks.length()>0 ) doc.replaceDocField(doc.kChecksum, cks); //! Urk this ends up AFTER SQ line !
			 
			linesout += doc.writeTo(douts, doid);
				//^^ EmblDoc() now writes "SQ" line with stats - usually
			if (doc.getDocField(BioseqDoc.kSeqstats)==null) {
				if (seqkind!=SeqInfo.kAmino && doc.getDocField(BioseqDoc.kChecksum)!=null) cks= "";
				writeSeqStats(cks); // must have
				}
			}
		else {
			writeID();
			writeTitle();
			writeSeqStats(cks);
			}
	}
   
};



