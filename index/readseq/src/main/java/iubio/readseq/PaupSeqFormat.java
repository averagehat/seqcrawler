package iubio.readseq;

import flybase.OpenString;



public class PaupSeqFormat extends BioseqFormat
{		
	public String formatName() { return "PAUP|NEXUS";  }  
	public String formatSuffix() { return ".nexus"; }  
	public String contentType() { return "biosequence/nexus"; }  

	public boolean canread() { return true; }   //? ((Debug.isOn)?true:false);
	public boolean canwrite() { return true; }  
	public boolean interleaved() { return true; }  
	public boolean needsamelength() { return true; }  

	public BioseqWriterIface newWriter() { return new PaupSeqWriter(); }		
	public BioseqReaderIface newReader() { return new PaupSeqReader(); }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
	 	if ( line.indexOf("#NEXUS") == 0 ) {
      formatLikelihood += 90;
      if (recordStartline==0) recordStartline= atline;
      return true;
      }
    else
    	return false;
	}
}