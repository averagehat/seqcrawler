package iubio.readseq;

import iubio.bioseq.SeqInfo;

import java.io.IOException;

import Acme.Fmt;


//public
class PaupSeqWriter extends InterleavedSeqWriter
{

	protected void interleaf(int leaf) {
		if (leaf==0) { writeln(); writeln("MATRIX"); }
		else writeln(); // required
		}

	protected void interleaveHeader()
	{
		String skind= SeqInfo.getKindLabel(bioseq.getSeqtype());
		writeln( "#NEXUS");
		//writeln( "[" + seqid + " -- data title]");
		writeln();
		writeln();
		writeln( "BEGIN DATA;");
		writeString( " DIMENSIONS NTAX=" + Integer.toString(getNseq()) );
		writeln( " NCHAR=" + seqLen() + ";" );
		writeString( " FORMAT DATATYPE=" + skind );
		writeString( " INTERLEAVE MISSING=" + opts.gapchar);
		if (opts.domatch) { writeString(" MATCHCHAR=" + opts.matchchar);  }
		writeln( ";");
	}
	
	public void writeTrailer() { 
		super.writeTrailer(); 
		writeln(";" );
		writeln( "END;"); 
		try { douts.flush(); } catch (IOException ex) {} //? missing END
		}
		
	public void writeRecordStart()
	{
		super.writeRecordStart();

		//testbase= new PaupOutBase();   testbaseKind= kUseTester;
		this.setOutputTranslation( new PaupOutBase( this.getOutputTranslation()));
		
  opts.nameleft = true;
  opts.nameflags= Fmt.LJ;
  opts.namewidth= 20;  // !? name width should be max of all name widths + some?
 	opts.seqwidth = 100; // was 100 ? shorter okay?
 	opts.spacer   = 20;	// was 21
  opts.tab = 0; 
	}

	public void writeDoc()
	{
		super.writeDoc();
 	writeString("[Name: " + Fmt.fmt(idword, 16, Fmt.LJ));
		writeString(" Len:" + Fmt.fmt(seqlen, 6));
		writeString("  Check:" + Fmt.fmt(checksum, 5));
		writeln("]");

		/*if (seqdoc instanceof BioseqDoc) {
			String title=  ((BioseqDoc)seqdoc).getTitle();
			if (title!=null) writeln( title );  
			}*/
	}

};
