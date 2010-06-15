package iubio.readseq;

import java.io.IOException;


/*
acedb> find dna
acedb> show -c 2 -a

DNA : "2L52"
         cctaagcctaagcctaaaatagtgactctggcagttctctaaaataagtg
         actctggcagttcaccaaaaattgtgactctgaccgttcaccaaaaatag
         aaaagtacttctggatatctacagtgcgaagaaaatgccaaa

DNA : "3R5"
         ttttcaccgctcgagtgtcgtcttgctgttgcttgtaaattccaagatga

acedb> find peptide
acedb> show -c 2 -a

Peptide : "SW:2ABG_RABIT"
MGEDTDTRKINHSFLRDHSYVTEADIISTVEFNHTGELLATGDKGGRVVI
FQREPESKNAPHSQGEYDVYSTFQSHEPEFDYLKSLEIEEKINKIKWLPQ
SVIMTGAYNNFFRMFDRNTKRDVTLEASRESSKPRAVLKPRRVCVGGKRR
RDDISVDSLDFTKKILHTAWHPAENIIAIAATNNLYIFQDKVNSDVH

Peptide : "SW:2ACA_HUMAN"
MAATYRLVVSTVNHYSSVVIDRRFEQAIHYCTGTCHTFTHGIDCIVVHHS
VCADLLHIPVSQFKDADLNSMFLPHENGLSSAEGDYPQQAFTGIPRVKRG

*/

//public
class AcedbSeqReader  extends BioseqReader
{
	public AcedbSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		}

	public boolean endOfSequence() {
	  return (nWaiting == 0 || getreadbuf(0) == '\n' || getreadbuf(0) == '\r');
		}

	protected void read() throws IOException
	{
	  while (!allDone) {
	    int at= sWaiting.indexOf(":");
	    if (at>=0) seqid= sWaiting.substring(at+1).trim().toString();
	    else if (nWaiting > 0) seqid= sWaiting.trim().toString(); //?
			if (seqid!=null) {
				at= seqid.indexOf('"'); int e= seqid.lastIndexOf('"'); 
				if (at>=0 && e>at) seqid= seqid.substring(at+1,e); 
				}
				
	    readLoop();
	    if (!allDone) {
	    	while (! (endOfFile() || ( nWaiting > 0 && sWaiting.indexOf(":")>=0 ) ) )
	        getline();
	      }
	    if (endOfFile()) allDone = true;
	  }
	}
};
