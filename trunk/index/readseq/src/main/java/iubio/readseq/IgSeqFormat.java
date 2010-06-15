package iubio.readseq;

import flybase.OpenString;


public class IgSeqFormat extends BioseqFormat
{
	public String formatName() { return "IG|Stanford"; }  
	public String formatSuffix() { return ".ig"; } 
	public String contentType() { return "biosequence/ig"; } 
	public BioseqReaderIface newReader() { return new IgSeqReader(); }
	public BioseqWriterIface newWriter() { return new IgSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.charAt(0) == ';') {
      formatLikelihood= 55;
      if (recordStartline==0) recordStartline= atline;
      return false; //!
      }
    else
    	return false;
	}

}