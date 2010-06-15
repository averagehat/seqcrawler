package iubio.readseq;

import java.io.IOException;


//public
class GenbankSeqReader  extends BioseqReader
{
	final static String kLocus  = "LOCUS ";
	final static String kOrigin = "ORIGIN";
	GenbankDoc doc;
	
	public GenbankSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//formatId= 2;
		}

	//public BioseqDoc getInfo() { return doc; }

	public boolean endOfSequence() {
		ungetend= (indexOfBuf(kLocus) == 0);
	  return (ungetend || indexOfBuf("//") >= 0);
		}

	
	protected void read() throws IOException
	{  
		doc= new GenbankDoc();
		if (sWaiting.indexOf(" aa ")>0) doc.setAmino(true);
		if (skipdocs) doc.setSkipDocs(skipdocs);
		seqdoc= doc;
	  while (!allDone) {
			boolean adddoc = ((atseq+1) == choice); //!  skip doc if wanted; readLoop() increments atseq
	    if (nWaiting > 12) seqid= sWaiting.substring(12).toString();
	  	if (adddoc) doc.addDocLine( sWaiting.toString());
	    while (!(endOfFile() || sWaiting.startsWith( kOrigin))) {
	  		getline(); if (adddoc) doc.addDocLine(sWaiting);
	   		}
	    readLoop();
			if (!allDone) {
	    	while (!(endOfFile() || (nWaiting > 0 && indexOfBuf(kLocus) == 0)))
	        getline();
	      }
	    if (endOfFile()) allDone = true;
	  }
	}

};
