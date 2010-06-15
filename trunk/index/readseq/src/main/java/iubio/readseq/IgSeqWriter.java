package iubio.readseq;


//public
class IgSeqWriter  extends BioseqWriter
{
	public void writeSeqEnd() { writeString("1"); }
	
	public void writeDoc()
	{
		//writeln( ";" + seqid + "  " + seqlen + " bases  " + checksumString());
		writeString( ";");
		writeString( seqid);
		writeString("  ");
		writeString( String.valueOf( seqlen));
		writeString(  " bases  ");
		writeln( checksumString());

		if (seqdoc instanceof BioseqDoc) {
			String title= ((BioseqDoc)seqdoc).getTitle();
			if (title!=null) { writeString(";"); writeln( title );  }
			}

		writeln( idword);
	//linesout += 2;
	}
 
};

