package iubio.readseq;

import flybase.OpenString;




public class PearsonSeqFormat extends BioseqFormat
{
	public String formatName() { return "Pearson|Fasta|fa"; }  
	public String formatSuffix() { return ".fasta"; } 
	public String contentType() { return "biosequence/fasta"; } 
	public BioseqReaderIface newReader() { return new PearsonSeqReader(); }
	public BioseqWriterIface newWriter() { return new PearsonSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.charAt(0) == '>') {
      formatLikelihood = 55;
      if (recordStartline==0) recordStartline= atline;
      return false; //!
      }
    else
    	return false;
	}

}
