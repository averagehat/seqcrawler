package iubio.readseq;

import iubio.bioseq.Bioseq;

//public
class AcedbSeqWriter extends PearsonSeqWriter // BioseqWriter
{
	//final static int seqwidth= 60;  

	public void writeRecordEnd() { writeln(); }

	//public void writeSeq() { super.writeSeq(); } // same as fasta		
		
	public void writeDoc() {
    if (bioseq.getSeqtype() == Bioseq.kAmino)  writeString("Peptide : ");
    else writeString("DNA : ");
		writeln( '"' + idword + '"' );
		//writeln( seqid + "  " + seqlen + " bases  " + checksumString());
		}
		
};
