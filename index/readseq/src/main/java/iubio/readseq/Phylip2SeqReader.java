package iubio.readseq;

import flybase.Debug;

import java.io.IOException;

//public
class Phylip2SeqReader  extends PhylipSeqReader
{
	public Phylip2SeqReader() {
		super();
		interleaved= false;
		}
		
	public boolean endOfSequence() {
		return endSequential();
		}
	protected void read() throws IOException {
		readSequential(); 
		}

	/*public Object clone() {
		Phylip2SeqReader c= (Phylip2SeqReader) super.clone();
		//? doesn't clone() copy vals of method data ?
    return c;
 		}*/

	protected boolean endSequential()
	{
		ungetend= false;
		countseq(getreadchars(), getreadcharofs()+margin, nWaiting); //countseq(sWaiting);
		boolean done= ( seqlencount >= sequenceLength());
	 	addend= !done;
		return done;
	}

	protected void readSequential() throws IOException
	{
		if (sequenceLength()==0 || speciesCount() == 0) {
	    NumSppBases nsppb= PhylipSeqFormat.readSpeciesLength(sWaiting); //!? must have already read these?
	    nospp= nsppb.nospp;
	    baselen= nsppb.baselen;
			Debug.println("format: phylip-sequential, nspp="+speciesCount()+", nbase="+sequenceLength());
			getline();
			}
		setNseq(speciesCount());
	  while (!allDone) {
	    seqlencount= 0;
	    seqid= sWaiting.substring(0,10).toString();
	    sWaiting= sWaiting.substring(10);
	    nWaiting= sWaiting.length();

	    margin= 0;
	    addfirst= true;
	    readLoop();
	    if (endOfFile()) allDone = true;
	  	}
	}

}
