package iubio.readseq;

import java.io.IOException;

//public
class Asn1SeqWriter  extends BioseqWriter
{
	public void writeHeader()  throws IOException { 
		super.writeHeader();
		writeln("Bioseq-set ::= {\nseq-set {"); 
		}
	public void writeTrailer() { 
		writeln("} }"); 
		super.writeTrailer();
		}
};