//iubio/readseq/CompareSeqWriter.java
//split4javac// iubio/readseq/CompareSeqWriter.java date=07-Jun-2001

// iubio.readseq.CompareSeqWriter.java
// compare input seq w/ other - document, features and seq checksums, diffs

package iubio.readseq;

import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.Enumeration;

import flybase.OpenString;
import flybase.Debug;
import flybase.FastHashtable;
import flybase.SortedEnumeration;

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


/*
  update to compare two sequence library files:
   -- dont assume 1-1 ordering of sequences, read & save all (basic) record info
       before compare; compare by seqIds
   -- option compare (a) fasta-deflines/doc, (b) sequence (length, crc, bases); a+b
   -- pass1: read files a,b deflines ; calc/store defl, len, crc, in hash
      -- report non-comparable items, counts
   -- pass2: if (b.bases), show base diffs
   
   usage: see readseq.run
      	protected void comparePair(String inname, Readseq rd, Object second, int forceInformat, Writer outs )

		CompareSeqWriter cmpwriter= new CompareSeqWriter();
		cmpwriter.setOutput( outs);
		cmpwriter.setFlags( compareval);
		cmpwriter.setSourceNames( in2name, inname);
		cmpwriter.writeHeader(); 
		boolean more= true;					 
		for (int irec= 1; more; irec++) {
			SeqFileInfo rec1= rd.readAt(irec); 
			SeqFileInfo rec2= rd2.readAt(irec); 
			if (rec1 == null || rec2 == null) {
				more= false;
			 	if (rec2 != null) cmpwriter.writeln("End of file for "+inname);
				if (rec1 != null) cmpwriter.writeln("End of file for "+in2name);
				}
			else {
				cmpwriter.compareTo( new BioseqRecord(rec1) );
				if ( cmpwriter.setSeq( rec2)) cmpwriter.writeSeqRecord();
				}
			}
		cmpwriter.writeTrailer();	
		cmpwriter.close();  

  -- new loop
  
		CompareSeqWriter cmpwriter= new CompareSeqWriter();
		cmpwriter.setOutput( outs);
		cmpwriter.setFlags( compareval);
		cmpwriter.setSourceNames( in2name, inname);
	  boolean more= true;					 

    more= true;					 
    for (int irec= 1; more; irec++) {
			SeqFileInfo rec= rd.readAt(irec); 
			if (rec==null) more= false;
			else cmpwriter.addLib( inname, new BioseqRecord(rec));
			}
    more= true;					 
    for (int irec= 1; more; irec++) {
			SeqFileInfo rec= rd2.readAt(irec); 
			if (rec==null) more= false;
			else cmpwriter.addLib( in2name, new BioseqRecord(rec));
			}

		cmpwriter.compareLibs( in2name, inname);
		cmpwriter.close();  


*/

public
class CompareSeqWriter  extends BioseqWriter
{
	int seqkind, ndiff;
	long mycrc, osicrc;
	BioseqRecord mysi, osi; // BioseqRecord ? SeqFileInfo
	String myname="myname", oldname="oldname";
	String flags="";
	boolean haslibs;
	FastHashtable libs;
		
	  // revise to store all pair seqs, then compare after matching ids
	  // e.g. for (fasta) seq lib.
	  
	final boolean bClearRec = true; //? option? need seqdoc, not seq after set
  
  public void setFlags( String flags) { if (flags==null) flags="";  this.flags= flags; }
	public void setSourceNames(String myname, String oldname) {
		this.myname= myname; 
		this.oldname= oldname; 
		}

  final String namefmt(String name) { return Fmt.fmt( name, 8, Fmt.LJ); }

	public void writeHeader()  throws IOException			// per file
	{ 
		super.writeHeader();
    diffLibHeader();
    diffSummaryHeader();
	}

	public void diffLibHeader()   
  {
    writeln("# Compare sequences with "+Readseq.version);
    writeln("# Compare options: keyid,keycrc,keytitle,keyindex ; "
      +"nosame,nosummary,noid,notitle,nolength,nocrc,nodoc]" );
    writeln("# Compare optval : "+flags);
    writeln("# Compare sources: "+namefmt( myname)+" --- " + namefmt( oldname) );
  }
  
  void diffSummaryHeader() 
  {
 	  //writeln("# Compare sequence records ");
    //writeln("# Compare sources --- "+myname+" --- " + oldname );
   	if (flags.indexOf("nosum")<0) {
    writeString( Fmt.fmt( "  ", 8, Fmt.LJ) + " ");
    writeString( Fmt.fmt( "ID", 15, Fmt.LJ) + " ");
    writeString( Fmt.fmt( "Length", 15)  + " ");
    writeString( Fmt.fmt( "Checksum", 15) );
		writeln();
		}
  }
  
  
  protected FastHashtable getLib(String name)
  {
    if (libs==null) libs = new FastHashtable();
    haslibs= true;
    FastHashtable alib = (FastHashtable) libs.get(name);
    if (alib == null) {
      alib = new FastHashtable();
      libs.put(name, alib);
      }
    return alib;
  }
  
  public void addLib( String name,  BioseqRecord rec )
  {
    if (name==null || rec==null) return;
    String rid= rec.getID();
    long crc= rec.getChecksum(); 
    String title= rec.getTitle(); // calc & store
    FastHashtable lib= getLib(name);
   
    // keyid == rid
    boolean didcrc= false;
    if (flags.indexOf("keyid")>=0)  ;
    else if (flags.indexOf("keytit")>=0) rid= title;
    else if (flags.indexOf("keyindex")>=0) rid= String.valueOf(lib.size());
    else if (flags.indexOf("keycrc")>=0) { rid= String.valueOf(crc); didcrc=true; }
       //add regex pattern for title/id ?

        // really should use Map here to keep multi values/id
    if (lib.containsKey(rid)) rid += String.valueOf(lib.size());
    lib.put(rid, rec);
     // ! note this now saves all of BioseqRecord data ... may end up w/ lots; drop seq? doc?

      // add second hash by crc and check both? crc first
    if (!didcrc ) { // && flags.indexOf("keycrc")>=0 ? make default
      lib= getLib(name+".crc");
      rid= String.valueOf(crc);
        // really should use Map here to keep multi values/id
      if (lib.containsKey(rid)) rid += String.valueOf(lib.size());
      lib.put(rid, rec);
      }
      
    if ( bClearRec && (flags.indexOf("nodoc")>=0) ) 
      rec.clear();  // drop seq, doc; leave static fields
  }
  
  public void compareLibs(String aname, String bname)   throws IOException	
  {
    FastHashtable alib= getLib(aname);
    FastHashtable blib= getLib(bname);
    //FastHashtable alibcrc= getLib(aname+".crc");
    FastHashtable blibcrc= getLib(bname+".crc");
    setSourceNames(aname, bname);
    int libdiff= 0;
    
    super.writeHeader(); 
    diffLibHeader();
    ndiff= 0;
    if (alib.size() != blib.size()) {
		 	diff("Library size", String.valueOf(alib.size()), String.valueOf(blib.size()) );  
      }
	  libdiff += ndiff; ndiff= 0;
	  
	  //? change to collect diff types: same, no equivalent, diff records
	  
    diffSummaryHeader();
    
    StringWriter sw= new StringWriter();
    Writer saveouts= this.douts;
    this.douts= sw;
    
	  FastHashtable adid= new FastHashtable();
	  FastHashtable bdid= new FastHashtable();
	  Enumeration an= new SortedEnumeration(alib.keys()).elements();
	  while ( an.hasMoreElements() ) {
	    String aid= (String) an.nextElement();
	    BioseqRecord ar, br;
	    ar= (BioseqRecord) alib.get(aid);
	    br= (BioseqRecord) blib.get(aid);
		  //  -- some genes have several trans w/ same aa - need ID first
      // some aa-crc's are same, diff ids (from diff mrnas)
      if (br==null) {
        String acrc= String.valueOf(ar.getChecksum());
        if (!adid.containsKey(acrc)) br= (BioseqRecord) blibcrc.get(acrc);
	      }
	      
	    if (br!=null) {
        bdid.put(aid,aid);
        String acrc= String.valueOf(ar.getChecksum()); 
        adid.put(acrc,acrc); //?
        showDiff( ar, br); // sets ndiff=0
        showSeqDiff( ar, br);
        showDocDiff( ar, br);
        if (flags.indexOf("nosum")<0) writeln("# No. record differences: "+ndiff);
        if (ndiff>0 || flags.indexOf("nosame")<0) saveouts.write(sw.toString()); 
        if (ndiff>0) { libdiff++; ndiff= 0; }
        sw.getBuffer().setLength(0);
        }
	    }
	    
    an= new SortedEnumeration(alib.keys()).elements();
	  while ( an.hasMoreElements() ) {
	    String aid= (String) an.nextElement();
	    if (bdid.containsKey(aid)) continue;
	    BioseqRecord ar= (BioseqRecord) alib.get(aid);
		  showDiff( ar, null); // sets ndiff=0
  		if (flags.indexOf("nosum")<0) writeln("# No. record differences: "+ndiff);
	    if (ndiff>0 || flags.indexOf("nosame")<0) saveouts.write(sw.toString()); 
	    if (ndiff>0) { libdiff++; ndiff= 0; }
      sw.getBuffer().setLength(0);
	    }
      
    an= new SortedEnumeration(blib.keys()).elements();
	  while ( an.hasMoreElements() ) {
	    String aid= (String) an.nextElement();
	    if (bdid.containsKey(aid)) continue;
	    BioseqRecord br= (BioseqRecord) blib.get(aid);
		  showDiff( null, br); // sets ndiff=0
  		if (flags.indexOf("nosum")<0) writeln("# No. record differences: "+ndiff);
	    if (ndiff>0 || flags.indexOf("nosame")<0) saveouts.write(sw.toString()); 
	    if (ndiff>0) { libdiff++; ndiff= 0; }
      sw.getBuffer().setLength(0);
	    }
	    
		douts= saveouts;
		writeln();
		writeln("# No. library differences: "+libdiff);
  }
 		
	public void writeRecordStart()
	{
		super.writeRecordStart();
		showDiff( mysi, osi);
  }
  
	public boolean setSeq( SeqFileInfo si) {
		mysi= (si instanceof BioseqRecord) ? (BioseqRecord)si : new BioseqRecord(si);
		return (mysi!=null);
		//return setSeq( si.seq, si.offset, si.seqlen, si.seqid, si.seqdoc, si.atseq, Bioseq.baseOnly);
		}
 
  public void compareTo( BioseqRecord oldsi) { 
    osi= oldsi; 
    }

  protected void diff(String fld, String newval, String oldval)
  {
  	writeString( Fmt.fmt(fld, 15, Fmt.LJ) + ": "); 
  	writeString( Fmt.fmt(newval, 15));
  	writeString( " != ");
  	writeString( Fmt.fmt(oldval, 15));
  	writeln();
  	ndiff++;
  }
  

	public void showDiff(BioseqRecord mysi, BioseqRecord osi)
	{
 	  //if (mysi==null || osi==null) return;
 	  
    //seqkind= mysi.getseq().getSeqtype();
		mycrc = (mysi==null) ? 0 : mysi.getChecksum(); //CRC32checksum( mysi.getseq(), mysi.offset(), mysi.length());
		osicrc= (osi==null ) ? 0 : osi.getChecksum(); //CRC32checksum( osi.getseq(), osi.offset(), osi.length() );

   	if (flags.indexOf("nosum")<0) {
 		if (mysi==null) writeString("null");
  	else {
  	writeString( namefmt( myname) + " ");
    writeString( Fmt.fmt( mysi.getID(), 15, Fmt.LJ) + " ");
    writeString( Fmt.fmt( mysi.length(), 15) + " ");
    writeString( Fmt.fmt( Long.toHexString(mycrc).toUpperCase(), 15) );
		}
		writeln();

		if (osi==null) writeString("null");
   	else {
   	writeString( namefmt( oldname) + " ");
    writeString( Fmt.fmt( osi.getID(), 15, Fmt.LJ) + " ");
    writeString( Fmt.fmt( osi.length(), 15) + " ");
    writeString( Fmt.fmt( Long.toHexString(osicrc).toUpperCase(), 15) );
		}
		writeln();
		}
		else {
		if (mysi==null) writeString("null");
		else writeString( namefmt( myname) + ":" + Fmt.fmt( mysi.getID(), 15, Fmt.LJ));
		writeString(" -- ");
		if (osi==null) writeString("null");
		else writeString( namefmt( oldname) + ":" + Fmt.fmt( osi.getID(), 15, Fmt.LJ));
		writeln();
		}
		
    //? writeln("# Fields --- "+namefmt( myname)+" --- " + namefmt( oldname) );
		ndiff= 0;
 	  if (mysi == null && osi!=null) ndiff++;
 	  else if (mysi != null && osi==null) ndiff++;
	}
	
	public void writeRecordEnd() { 
		writeln("# No. record differences: "+ndiff);
		writeln();
		}
	
	public void writeSeq() // per sequence
	{
    showSeqDiff(mysi, osi);
  }
  
	protected void showSeqDiff(BioseqRecord mysi, BioseqRecord osi) // per sequence
	{
 	  if (mysi==null || osi==null) return;
		if (flags.indexOf("nolen")<0 && mysi.length() != osi.length() ) 
		 	diff("Sequence length", String.valueOf(mysi.length()), String.valueOf(osi.length()) );  
		else { // no need to check crc if len diff
			if (flags.indexOf("nocrc")<0 && mycrc != osicrc) 
				diff("Sequence checksum", Long.toHexString(mycrc).toUpperCase(), 
				  Long.toHexString(osicrc).toUpperCase());
			}
  }
  
	public void writeDoc()
	{
    showDocDiff(mysi, osi);
  }
  
	protected void showDocDiff(BioseqRecord mysi, BioseqRecord osi)
	{
 	  if (mysi==null || osi==null) return;
		//String cks= checksumString();
		if (flags.indexOf("noid")<0 && !mysi.getID().equals(osi.getID()) ) 	
		  diff("ID", mysi.getID(), osi.getID());  
    if (flags.indexOf("notitle")<0 && !mysi.getTitle().equals(osi.getTitle()) ) 	
      diff("Title", mysi.getTitle(), osi.getTitle());  

	  if (mysi.getdoc() instanceof BioseqDoc) {
			BasicBioseqDoc doc= new BasicBioseqDoc( mysi.getdoc()); 
			BasicBioseqDoc olddoc= new BasicBioseqDoc( osi.getdoc()); // null ok
//      if (flags.indexOf("notitle")<0 && !doc.getTitle().equals(olddoc.getTitle()) ) 	
//        diff("Title", doc.getTitle(), olddoc.getTitle());  
      if (flags.indexOf("nodoc")<0) 
        ndiff += doc.compareTo(douts, olddoc);
		  }
		
 	}
 	
};
//split4javac// iubio/readseq/CompareSeqWriter.java line=125

