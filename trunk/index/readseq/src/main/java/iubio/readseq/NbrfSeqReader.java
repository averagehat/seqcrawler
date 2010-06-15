package iubio.readseq;

import java.io.IOException;



//public
class NbrfSeqReader  extends BioseqReader
{
	public NbrfSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//formatId= 3;
		}
		
	public boolean endOfSequence()
	{
		int starat= indexOfBuf('*');
	  if (starat >= 0) { /* end of 1st seqq */
	    /* "*" can be valid base symbol, drop it here */
	    sWaiting= bufSubstring(0,starat); //?? don't need unless needString set
	    nWaiting= starat;
	   	addend  = true;
	   	ungetend= false;
	    return(true);
	    }
	  else if (getreadbuf(0) == '>') { /* start of next seqq */
	    addend  = false;
	    ungetend= true;
	    return(true);
	    }
	  else
	    return(false);
	}
	  

	protected void read() throws IOException
	{
	  while (!allDone) {
	    if (nWaiting > 4) seqid= sWaiting.substring(4).toString();
	    getline();   /*skip title-junk line*/
	    readLoop();
	    if (!allDone) {
	     	while (!(endOfFile() || (nWaiting > 0 && getreadbuf(0) == '>')))
	   			getline();
	      }
	    if (endOfFile()) allDone = true;
	  }
	}
};

