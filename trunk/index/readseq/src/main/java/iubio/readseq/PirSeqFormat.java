package iubio.readseq;

import flybase.OpenString;




public class PirSeqFormat extends BioseqFormat
{
	public String formatName() { return "PIR|CODATA"; }  
	public String formatSuffix() { return ".pir"; } 
	public String contentType() { return "biosequence/codata"; } 
	public BioseqReaderIface newReader() { return new PirSeqReader(); }
	public BioseqWriterIface newWriter() { return new PirSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }
	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.startsWith(PirSeqReader.kEntry)) {
      formatLikelihood += 80;
      if (recordStartline==0) recordStartline= atline;
      return false;
      }
    else if (line.startsWith(PirSeqReader.kSequence)) {
      formatLikelihood += 70;
      return false;
      }
    else if (line.startsWith("///")) {
      formatLikelihood += 20;
      return false;
      }
    else
    	return false;
	}
}