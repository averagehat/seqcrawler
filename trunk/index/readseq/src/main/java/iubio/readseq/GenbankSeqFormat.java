package iubio.readseq;

import flybase.OpenString;



public class GenbankSeqFormat extends BioseqFormat
{
	public String formatName() { return "GenBank|gb"; }  
	public String formatSuffix() { return ".gb"; } 
	public String contentType() { return "biosequence/genbank"; } 
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }
	public boolean hasdoc() { return true; }
	public BioseqReaderIface newReader() { return new GenbankSeqReader(); }
	public BioseqWriterIface newWriter() { return new GenbankSeqWriter(); }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.startsWith(GenbankSeqReader.kLocus)) {
      formatLikelihood= 80;
      if (recordStartline==0) recordStartline= atline;
    	return false;
      }
    else if (line.startsWith(GenbankSeqReader.kOrigin)) {
      formatLikelihood += 70;
      return false;
      }
    else if (line.startsWith("//")) {
      formatLikelihood += 20;
      return false;
      }
    else
    	return false;
	}
}

