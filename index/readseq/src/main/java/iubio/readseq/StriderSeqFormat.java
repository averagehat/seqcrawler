package iubio.readseq;

import flybase.OpenString;




public class  StriderSeqFormat extends BioseqFormat
{
	public String formatName() { return "DNAStrider"; }  
	public String formatSuffix() { return ".strider"; } 
	public String contentType() { return "biosequence/strider"; } 
	public BioseqReaderIface newReader() { return new StriderSeqReader(); }
	public BioseqWriterIface newWriter() { return new StriderSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }
	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.charAt(0) == ';' && line.indexOf("Strider")>0) {
      formatLikelihood= 75;
      if (recordStartline==0) recordStartline= atline;
      return true;
      }
    else
    	return false;
	}

}
