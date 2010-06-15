package iubio.readseq;

import flybase.OpenString;


public class NbrfSeqFormat extends BioseqFormat
{
	public String formatName() { return "NBRF"; }  
	public String formatSuffix() { return ".nbrf"; } 
	public String contentType() { return "biosequence/nbrf"; } 
	public BioseqReaderIface newReader() { return new NbrfSeqReader(); }
	public BioseqWriterIface newWriter() { return new NbrfSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.charAt(0) == '>' && line.charAt(3) == ';') {
      formatLikelihood += 70; //?
      if (recordStartline==0) recordStartline= atline;
      return false; //?
      }
    else
    	return false;
	}

}
