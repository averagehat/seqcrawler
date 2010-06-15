package iubio.readseq;

import java.io.IOException;



//public
class EmblSeqReader  extends BioseqReader
{
	final static String kID = "ID   ", kAcc= "AC   ", kDesc= "DE   ", kSequence = "SQ   ";
	EmblDoc doc;

	public EmblSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//formatId= 4;
		}
		
	//public BioseqDoc getInfo() { return doc; }

	public boolean endOfSequence() {
		ungetend= ( indexOfBuf(kID) == 0);
	  return (ungetend || indexOfBuf("//") >= 0);
		}

	protected void read() throws IOException
	{
		//sWaiting == PRT;   379 AA. << //? SwissDoc() if isamino
		if (sWaiting.indexOf(" AA.")>0) doc= new SwissDoc(); 
		else doc= new EmblDoc(); 
		if (skipdocs) doc.setSkipDocs(skipdocs);
		seqdoc= doc;
			
	while (!allDone) {
			boolean adddoc = ((atseq+1) == choice); //!  skip doc if wanted; readLoop() increments atseq
			// readLoop() increments atseq !, sets addit for seq collecting
	  	if (adddoc) doc.addDocLine( sWaiting);
	    if (nWaiting > 5) seqid= sWaiting.substring(5).toString();
	    do {
	  		getline(); if (adddoc) doc.addDocLine(sWaiting);
	    } while (!(endOfFile() || (sWaiting.startsWith(kSequence))));

	    readLoop();
	    
	    if (!allDone) {
	      while (!(endOfFile() || (nWaiting>0 && indexOfBuf(kID) == 0)))
	      	getline();
	    	}
	    if (endOfFile()) allDone = true;
	  }
	}

};