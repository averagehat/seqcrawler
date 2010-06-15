package iubio.readseq;

import java.io.IOException;



//public
class IgSeqReader  extends BioseqReader
{
	public IgSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= true;  
		ungetend= false;
		//formatId= 1;
		}

	public boolean endOfSequence() {
	  return ( indexOfBuf('1')>=0 || indexOfBuf('2')>=0);
		}

	protected void read() throws IOException
	{
	  while (!allDone) {
	    do {
	      getline();
	      sWaiting= sWaiting.trim(); 
	      nWaiting= sWaiting.length();
	    } while (!(endOfFile() || (nWaiting>0 && sWaiting.charAt(0) != ';' ) ));
	    
	    if (!endOfFile()) {
	      seqid= sWaiting.toString();
	      readLoop();
	      }
	    if (endOfFile()) allDone = true;
	  }
	}
};

