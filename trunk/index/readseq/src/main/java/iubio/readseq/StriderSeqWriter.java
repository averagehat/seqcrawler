package iubio.readseq;

//public
class StriderSeqWriter  extends BioseqWriter
{		
	public void writeRecordEnd() { writeln("//"); }

	public void writeDoc()
	{
		writeln( "; ### from DNA Strider ;-)");
		writeString( "; DNA sequence  ");
		writeString( seqid); 
		writeln( "  " + seqlen + " bases " + checksumString());
		writeln( ";");
		//linesout += 3;
	}
	
};
