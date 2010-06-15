// iubio.readseq.CompareSeqWriter.java
// compare input seq w/ other - document, features and seq checksums, diffs

package iubio.readseq;

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
import iubio.readseq.BasicBioseqDoc; //GenbankDoc;
 
import iubio.readseq.BioseqFormat;
import iubio.readseq.BioseqWriter;



public
class CompareSeqWriter  extends BioseqWriter
{
	int seqkind, ndiff;
	long mycrc, osicrc;
	BioseqRecord osi; // BioseqRecord ? SeqFileInfo
	String myname, oldname;
	
  public void compareTo( BioseqRecord oldsi) { osi= oldsi; }
	public void setSourceNames(String myname, String oldname) {
		this.myname= myname; this.oldname= oldname; }

	public void writeHeader()  throws IOException			// per file
	{ 
		super.writeHeader();
    writeln("# Compare sources --- "+myname+" --- " + oldname );
	}
		
	public void writeRecordStart()
	{
		super.writeRecordStart();
 	  writeln("# Compare sequence records ");
 	  
    seqkind= bioseq.getSeqtype();
		mycrc = CRC32checksum( bioseq, offset, seqlen);
		osicrc= CRC32checksum( osi.getseq(), osi.offset(), osi.length() );
		
    writeString( Fmt.fmt( "  ", 8, Fmt.LJ) + " ");
    writeString( Fmt.fmt( "ID", 15, Fmt.LJ) + " ");
    writeString( Fmt.fmt( "Length", 15)  + " ");
    writeString( Fmt.fmt( "Checksum", 15) );
		writeln();

   	writeString( Fmt.fmt( "NEW", 8, Fmt.LJ) + " ");
    writeString( Fmt.fmt( seqid, 15, Fmt.LJ) + " ");
    writeString( Fmt.fmt( seqlen, 15) + " ");
    writeString( Fmt.fmt( Long.toHexString(mycrc), 15) );
		writeln();

   	writeString( Fmt.fmt( "OLD", 8, Fmt.LJ) + " ");
    writeString( Fmt.fmt( osi.getID(), 15, Fmt.LJ) + " ");
    writeString( Fmt.fmt( osi.length(), 15) + " ");
    writeString( Fmt.fmt( Long.toHexString(osicrc), 15) );
		writeln();
		
    writeln("# Differences ----- NEW ------- OLD --------");
		ndiff= 0;
	}
	
	public void writeRecordEnd() { 
		writeln("# N. differences : "+ndiff+" ----------------------------------");
		writeln();
		}
	
	public void writeSeq() // per sequence
	{
		if ( seqlen != osi.length() ) 
		 	diff("Sequence length: ", String.valueOf(seqlen), String.valueOf(osi.seqlen) );  
		else { // no need to check crc if len diff
			if (mycrc != osicrc) 
				diff("Sequence checksum :", Long.toHexString(mycrc), Long.toHexString(osicrc));
			}
  }
  
  protected void diff(String fld, String newval, String oldval)
  {
  	writeString( Fmt.fmt(fld, 15, Fmt.LJ) + " "); 
  	writeString( Fmt.fmt(newval, 15));
  	writeString( " != ");
  	writeString( Fmt.fmt(oldval, 15));
  	writeln();
  	ndiff++;
  }
  
	public void writeDoc()
	{
		//String cks= checksumString();
		if (! seqid.equals(osi.getID()) ) 	diff("ID: ", seqid, osi.seqid);  
		
		if (seqdoc instanceof BioseqDoc) {
			BasicBioseqDoc doc= new BasicBioseqDoc((BioseqDoc)seqdoc); 
			BasicBioseqDoc olddoc= new BasicBioseqDoc(osi.getdoc()); 
			ndiff += doc.compareTo(douts, olddoc);
			}
		else if (osi.getdoc()!=null) {
			diff("Document values: ", "(missing)",  String.valueOf( osi.getdoc().documents().size()) );  
			}
 	}
 	
};