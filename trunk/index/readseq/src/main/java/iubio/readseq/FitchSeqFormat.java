package iubio.readseq;

import flybase.OpenString;





public class FitchSeqFormat extends BioseqFormat
{
	public String formatName() { return "Fitch"; }  
	public String formatSuffix() { return ".fitch"; } 
	public String contentType() { return "biosequence/fitch"; } 
	public BioseqReaderIface newReader() { return new FitchSeqReader(); }
	//public BioseqWriterIface newWriter() { return new FitchSeqWriter(); }
	public boolean canread() { return false; }
	public boolean canwrite() { return false; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		int splen= line.length();
		boolean isfitch= true;
    for (int k=0; isfitch && (k < splen); k++) {
      if (k % 4 == 0) isfitch &= (line.charAt(k) == ' ');
      else isfitch &= (line.charAt(k) != ' ');
      }
    if (isfitch && (splen > 20)) formatLikelihood += 10;
    return false;
	}
	
}