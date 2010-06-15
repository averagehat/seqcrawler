package iubio.readseq;

import flybase.OpenString;



public class ZukerSeqFormat extends BioseqFormat
{
	public String formatName() { return "Zuker"; }  
	public String formatSuffix() { return ".zuker"; } 
	public String contentType() { return "biosequence/zuker"; } 
	public BioseqReaderIface newReader() { return new ZukerSeqReader(); }
	//public BioseqWriterIface newWriter() { return new ZukerSeqWriter(); }
	public boolean canread() { return false; }
	public boolean canwrite() { return false; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.charAt(0) == '(') {
      formatLikelihood= 25;
      if (recordStartline==0) recordStartline= atline;
      return false; //!
      }
    else
    	return false;
	}
}
