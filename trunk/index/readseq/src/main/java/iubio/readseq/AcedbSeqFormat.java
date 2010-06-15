package iubio.readseq;

import flybase.OpenString;


public class AcedbSeqFormat extends BioseqFormat
{
	public String formatName() { return "ACEDB"; }  
	public String formatSuffix() { return ".ace"; } 
	public String contentType() { return "biosequence/acedb"; } 
	public BioseqReaderIface newReader() { return new AcedbSeqReader(); }
	public BioseqWriterIface newWriter() { return new AcedbSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.indexOf(" : ")>0 && line.indexOf('"')>0) {
      formatLikelihood = 70;
      if (recordStartline==0) recordStartline= atline;
      if ( line.startsWith("DNA : ") 
      	|| line.startsWith("Peptide : ") 
      	|| line.startsWith("PROTEIN : ")) return true;
      else return false;  
      }
    else
    	return false;
	}

}
