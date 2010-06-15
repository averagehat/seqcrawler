package iubio.readseq;

import iubio.bioseq.Bioseq;


//public
class NbrfSeqWriter  extends BioseqWriter
{

	public void writeRecordStart()
	{
		super.writeRecordStart();
  opts.spacer = 10;
	}
		
	public void writeSeqEnd() { writeString("*");  }
	
	public void writeDoc()
	{
  String tag;
  if (bioseq.getSeqtype() == Bioseq.kAmino)  tag= ">P1;"; else tag= ">DL;";
		writeln( tag + idword);
		writeln( seqid + "  " + seqlen + " bases  " + checksumString());
  //linesout += 3;
 }
 
};



