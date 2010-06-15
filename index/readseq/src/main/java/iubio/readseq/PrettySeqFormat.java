package iubio.readseq;



public class PrettySeqFormat extends BioseqFormat
{		
	public String formatName() { return "Pretty";  }  
	public String formatSuffix() { return ".pretty"; }  
	public String contentType() { return "biosequence/pretty"; }  

	public boolean canread() { return false; }   //? not readable? - fix
	public boolean canwrite() { return true; }  
	public boolean interleaved() { return true; }  

	public BioseqWriterIface newWriter() { return new PrettySeqWriter(); }		
	//public BioseqReaderIface newReader() { return new PrettySeqReader(); }

}