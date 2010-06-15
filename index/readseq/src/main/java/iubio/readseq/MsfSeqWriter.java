package iubio.readseq;

import iubio.bioseq.Bioseq;

import java.text.SimpleDateFormat;
import java.util.Date;

import Acme.Fmt;



//public
class MsfSeqWriter extends InterleavedSeqWriter
{
	protected String datestr;

	protected long calculateChecksum()
	{
		return GCGchecksum( bioseq, offset, seqlen);
	}

	protected void interleaf(int leaf) {
			// do after writing seqq names ??!? == writeDoc, but need to interleave names
		if (leaf==0) { writeln( "//"); } 
		writeln(); 
		}

	protected void interleaveHeader()
	{
		int checktotal = 0;
  if (datestr==null) {
		SimpleDateFormat sdf= new SimpleDateFormat("MMM dd, yyyy  HH:mm"); //August 28, 1991  02:07
		datestr= sdf.format(new Date());
		}
		//foreach seq do
		//  int checksum= calculateChecksum(); // need checksum for all sequences -!?
		//checktotal= checksumTotal; // will have this after all are written!

  String stype;
  if (bioseq.getSeqtype() == Bioseq.kAmino)  { stype= "P"; writeString("!!AA"); }
  else { stype= "N"; writeString("!!NA"); }
		writeln("_MULTIPLE_ALIGNMENT"); // gcg9+ - leave out? - use AA_ for aminos ?
		writeln();
		writeString( " " + seqid + "  MSF: " + seqlen);
	  writeString( "  Type: "+stype+"  " + datestr );
	  writeln( "  Check: " +  checktotal + " ..");
	  writeln();
	}
	
	
	public void writeRecordStart()
	{
		super.writeRecordStart();
		setChecksum(true);
 	opts.spacer = 10;
  opts.nameleft = true;
  opts.namewidth= 15;  
 	opts.seqwidth= 50;
  opts.tab = 1; 
	}

	public void writeDoc()
	{
		super.writeDoc();
	writeString(" Name: " + Fmt.fmt(idword, 16, Fmt.LJ));
		writeString(" Len:" + Fmt.fmt(seqlen, 6));
		writeString("  Check:" + Fmt.fmt(checksum, 5));
		writeln("  Weight:  1.00");
	}

};