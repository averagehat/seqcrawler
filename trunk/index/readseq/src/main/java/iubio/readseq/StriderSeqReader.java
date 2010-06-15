package iubio.readseq;

import java.io.IOException;



//public
class StriderSeqReader  extends BioseqReader
{
	public StriderSeqReader() {
		margin	=  0;
		addfirst= true;
		addend 	= false;  
		ungetend= false;
		//formatId= 6;
		}

	public boolean endOfSequence() {
	  return ( indexOfBuf("//")>=0);
		}

	protected void read() throws IOException
	{
	  while (!allDone) {
    getline();
    sWaiting= sWaiting.trim(); 
    nWaiting= sWaiting.length();
	    if (sWaiting.indexOf("; DNA sequence  ")==0)
  		seqid= sWaiting.substring(16).toString();
	    else if (nWaiting > 0)
  		seqid= sWaiting.substring(1).toString();
			while (!(endOfFile() || (nWaiting>0 && sWaiting.charAt(0) != ';' ) )) {
	      getline();
	      sWaiting= sWaiting.trim(); 
	      nWaiting= sWaiting.length();
	    	}
	    
	    if (!endOfFile()) {
	      readLoop();
	      }
	    if (endOfFile()) allDone = true;
	  }
	}
};
