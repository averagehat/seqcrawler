package iubio.readseq;

import java.io.IOException;



//public
class OlsenSeqReader  extends InterleavedSeqReader //InterleavedSeqreader
{
	public OlsenSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//formatLabel= "Olsen";
		//formatId= 10;
		}

	public boolean endOfSequence() {
		return false;
		}

	protected void read() throws IOException
	{
			// needs some work...
	}
	
};



