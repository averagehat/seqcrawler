//iubio/readseq/GenbankSeqFormat.java
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

//split4javac// iubio/readseq/CommonSeqFormat.java line=474
public class GenbankSeqFormat extends BioseqFormat
{
	public String formatName() { return "GenBank|gb"; }  
	public String formatSuffix() { return ".gb"; } 
	public String contentType() { return "biosequence/genbank"; } 
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }
	public boolean hasdoc() { return true; }
	public BioseqReaderIface newReader() { return new GenbankSeqReader(); }
	public BioseqWriterIface newWriter() { return new GenbankSeqWriter(); }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.startsWith(GenbankSeqReader.kLocus)) {
      formatLikelihood= 80;
      if (recordStartline==0) recordStartline= atline;
    	return false;
      }
    else if (line.startsWith(GenbankSeqReader.kOrigin)) {
      formatLikelihood += 70;
      return false;
      }
    else if (line.startsWith("//")) {
      formatLikelihood += 20;
      return false;
      }
    else
    	return false;
	}
}

//public
class GenbankSeqReader  extends BioseqReader
{
	final static String kLocus  = "LOCUS ";
	final static String kOrigin = "ORIGIN";
	GenbankDoc doc;
	
	public GenbankSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//formatId= 2;
		}

	//public BioseqDoc getInfo() { return doc; }

	public boolean endOfSequence() {
		ungetend= (indexOfBuf(kLocus) == 0);
	  return (ungetend || indexOfBuf("//") >= 0);
		}

	
	protected void read() throws IOException
	{  
		doc= new GenbankDoc();
		if (sWaiting.indexOf(" aa ")>0) doc.setAmino(true);
		if (skipdocs) doc.setSkipDocs(skipdocs);
		seqdoc= doc;
	  while (!allDone) {
			boolean adddoc = ((atseq+1) == choice); //!  skip doc if wanted; readLoop() increments atseq
	    if (nWaiting > 12) seqid= sWaiting.substring(12).toString();
	  	if (adddoc) doc.addDocLine( sWaiting.toString());
	    while (!(endOfFile() || sWaiting.startsWith( kOrigin))) {
	  		getline(); if (adddoc) doc.addDocLine(sWaiting);
	   		}
	    readLoop();
			if (!allDone) {
	    	while (!(endOfFile() || (nWaiting > 0 && indexOfBuf(kLocus) == 0)))
	        getline();
	      }
	    if (endOfFile()) allDone = true;
	  }
	}

};

//public
class GenbankSeqWriter  extends BioseqWriter
{
	final static int seqwidth= 60, ktab= 0, kspacer= 10, knumwidth= 9, knumflags= 0;  
	String datestr;
	int seqkind;
	
	public void writeRecordStart()
	{
		super.writeRecordStart();
   	//opts.tab = ktab;    
    opts.spacer = kspacer;  
   	opts.seqwidth = seqwidth;
    opts.numleft = true;
    opts.numwidth = knumwidth;  
    seqkind= bioseq.getSeqtype();
	}
	
	public void writeRecordEnd() { writeln("//"); }

//123456789
//ORIGIN      
//        1 cagcagccgc ggtaatacca gctccaatag cgtatattaa agttgttgtg gttaaaaagc
//       61 tcgtagttgg atctcagatc cggagctgcg gtccaccgcc cggtggttac tgtagcgacc
////

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
		    	if (rev) ib= origin-i; else ib= origin+i;
		    	writeString( Fmt.fmt( ib, knumwidth, knumflags));
		    	writeByte(' ');
					newline= false;
					}
				writeByte( ba[offset+i]); 
				if (i % seqwidth == seqwidth-1) { writeln(); newline= true; }
				else if (i % kspacer == kspacer-1) writeByte(' ');
				}
			}
		else {
			for (i= 0; i < seqlen; i++) {
				if (newline) {
		    	if (rev) ib= origin-nout; else ib= origin+nout;
		    	writeString( Fmt.fmt( ib, knumwidth, knumflags));
		    	writeByte(' ');
					newline= false;
					}

		   	char bc= bioseq.base(offset+i,fBasePart);
	      if (testbaseKind == kUseTester) bc= (char) testbase.outSeqChar( bc);
				if (bc>0) {
					writeByte( bc); nout++;
					if ( (nout-1) % seqwidth == seqwidth-1) { 
						writeln(); newline= true; 
						}
					else if ( (nout-1) % kspacer == kspacer-1) 
						writeByte(' ');
					}
				}
			}
			
		if (!newline) {
			writeln();
			}
  }
  
 
	protected void writeID() {
		//LOCUS       AF005656     1504 bp    DNA             UNA       02-JAN-1999
		writeString("LOCUS       ");
		writeString( Fmt.fmt( idword, 11, Fmt.LJ));
		writeString( Fmt.fmt( seqlen, 7, 0));
		if (seqkind==SeqInfo.kAmino)  writeString(" aa "); 
		else writeString(" bp ");
		String skind= SeqInfo.getKindLabel(seqkind);
		writeString( Fmt.fmt( skind, 6, 0));
		writeString("             UNA       ");
    if (datestr==null) {
    	SimpleDateFormat sdf= new SimpleDateFormat("dd-MMM-yyyy");
    	datestr= sdf.format(new Date());
    	}
		writeString( datestr);
		writeln();
		}
		
	protected final void writeTitle() { writeString("DEFINITION  "); writeln( seqid); }
			
	public void writeDoc()
	{
		String cks= checksumString();
		if (seqdoc instanceof BioseqDoc) {
			GenbankDoc doc= new GenbankDoc((BioseqDoc)seqdoc); 
			if (seqkind==SeqInfo.kAmino) doc.setAmino(true);
			boolean doid= true;
			String docid= doc.getID();
			if (docid==null || 
				(! idword.startsWith( SeqFileInfo.gBlankSeqid) && ! docid.equals(idword) )  )
					{ writeID(); doid= false; }
			if (!doid && doc.getTitle()==null) writeTitle(); // should have, but not before id
			doc.replaceDocField( doc.kSeqlen, String.valueOf(seqlen) );
			if (cks.length()>0 ) doc.replaceDocField(doc.kChecksum, cks); //! Urk this ends up AFTER BASE line !
			linesout += doc.writeTo(douts, doid);
			}
		else {
			writeID();
			writeTitle();
			if (cks.length()>0) { writeString("COMMENT     "); writeln(cks); }
			//writeString("BASE COUNT        0 a      0 c      0 g      0 t      0 others");
			}
		writeln( "ORIGIN      "); // always do here - never in doc
 	}
 	
};



