package iubio.readseq;

import java.io.IOException;



//public
class GcgSeqReader  extends BioseqReader
{
	public GcgSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		testbase= new TestGcgBase();
		testbaseKind= kUseTester;
		//formatId= 5;
		}

	public boolean endOfSequence() {
		return false;
		}

	protected void read() throws IOException
	{
  boolean gotuw= false;
  do {
    getlineBuf();
	    if (sWaiting.startsWith("!!")) continue; // GCG version 9+ comment line
   	gotuw = ( indexOfBuf("..")>=0);
  } while (!(gotuw || endOfFile()));
  if (gotuw) readUWGCG(); 
	}

	protected void readUWGCG()  throws IOException
	{
		atseq++;
		addit = (choice > 0);
	  if (addit) seqlen = 0;
		seqid= sWaiting.toString();
		int i;
	  if ((i = seqid.indexOf(" Length: "))>0) seqid= seqid.substring(0,i).trim();
	  else if ((i = seqid.indexOf(".."))>0) seqid= seqid.substring(0,i).trim();
		boolean done;
	  do {
	 		done = endOfFile();
	    getlineBuf(); // bad of endOfFile ??
	    if (sWaiting.startsWith("!!")) continue; // GCG version 9+ comment line
	    if (!done) addseq(getreadchars(), getreadcharofs()+margin, nWaiting - margin);
	  } while (!done);
	  if (choice == kListSequences) addinfo(seqid);
		allDone = true;
	}
	
	
};