package iubio.readseq;

import java.io.IOException;

import Acme.Fmt;




//public
class PirSeqWriter  extends BioseqWriter
{

	public void writeRecordStart()
	{
		super.writeRecordStart();
  opts.numwidth = 7;
  opts.seqwidth= 30;
  opts.spacer = kSpaceAll;
  opts.numleft = true;
	}
			
	public void writeHeader()  throws IOException { 
		super.writeHeader();
		writeln( "\\\\\\"); 
		}

	public void writeRecordEnd() { writeln("///"); }
	
	public void writeDoc()
	{
 	// somewhat like genbank...  
		writeString("ENTRY           ");
		writeString(idword);
		writeln(" ");

		writeString("TITLE           ");
		String title= seqid;
		if (seqdoc instanceof BioseqDoc) {
			String t= ((BioseqDoc)seqdoc).getTitle();
			if (t!=null) title= t;
			}
		writeString( title);
		writeString(" ");
 	writeString( String.valueOf(seqlen));
		writeString(" bases  ");
	writeln(checksumString());
		
		writeln( "SEQUENCE        ");
  //run a top number line for PIR 
  int j;
  for (j=0; j<opts.numwidth; j++) writeByte(' ');
  for (j=5; j<=opts.seqwidth; j += 5) writeString( Fmt.fmt( j, 10));
  writeln();  
  //linesout += 5;
	}
	
	
};