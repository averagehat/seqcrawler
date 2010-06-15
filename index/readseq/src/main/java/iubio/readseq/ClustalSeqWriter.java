package iubio.readseq;

import Acme.Fmt;


//public
class ClustalSeqWriter extends InterleavedSeqWriter //PhylipSeqWriter //?
{
	int lastlen;
	String lenerr;

	public void writeRecordStart() {
		super.writeRecordStart();
 	opts.spacer = 0;
 	opts.seqwidth= 60;
	opts.nameleft = true;
	opts.nameflags= Fmt.LJ;
	opts.namewidth= ClustalSeqReader.kNameWidth;  
 	opts.tab = 1; 
	//opts.tab = ClustalSeqReader.kNameWidth+1; 
		}

	public void writeDoc() {
		super.writeDoc();
		//? writeString( Fmt.fmt( idword, ClustalSeqReader.kNameWidth, Fmt.TR + Fmt.LJ) + "  ");
		}

	protected void interleaf(int leaf) {
		writeln( Fmt.fmt( " ", ClustalSeqReader.kNameWidth+1, Fmt.TR + Fmt.LJ));  // conserved line:           *:  :   :   *  .           :  .:   * :   *  :   . 
		writeln();  
		}

	protected void interleaveHeader() {
		writeln("CLUSTAL W (1.8) multiple sequence alignment"); 
		writeln(); 
		writeln(); 
	}

	public boolean setSeq( Object seqob, int offset, int length, String seqname,
						 Object seqdoc, int atseq, int basepart) 
	{
		if (lastlen > 0 && length != lastlen) {
			if (lenerr==null) lenerr= String.valueOf(lastlen) + " != ";
			lenerr += String.valueOf(length) + ", ";
			length= lastlen; // can we pad/trunc to first length?
			}
		else lastlen= length;
		return super.setSeq(seqob,offset,length,seqname,seqdoc,atseq, basepart);
	}

	public void writeTrailer()  { 
		if (lenerr!=null) {
			BioseqReader.message("Warning: this format requires equal sequence lengths.");
			BioseqReader.message("       : lengths are padded/truncated to "+lastlen);
			BioseqReader.message("       : " + lenerr);
			}
		super.writeTrailer();
	}
	
};