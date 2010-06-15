package iubio.readseq;

import java.io.IOException;


//public
class PearsonSeqReader  extends BioseqReader
{
	public PearsonSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//formatId= 8;
		}

	public boolean endOfSequence() {
	  return (nWaiting > 0 && getreadbuf(0) == '>');
		}

	protected void read() throws IOException
	{
	  while (!allDone) {
	    if (nWaiting > 0) seqid= sWaiting.substring(1).toString();
	    readLoop();
	    if (!allDone) {
	    	while (!(endOfFile() || (nWaiting > 0 && getreadbuf(0) == '>') ))
	        getline();
	      }
	    if (endOfFile()) allDone = true;
	  }
	}
};
