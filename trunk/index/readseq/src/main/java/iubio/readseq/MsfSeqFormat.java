package iubio.readseq;

import flybase.OpenString;



public class MsfSeqFormat extends BioseqFormat
{		
	public String formatName() { return "MSF";  }  
	public String formatSuffix() { return ".msf"; }  
	public String contentType() { return "biosequence/msf"; }  
	public boolean canread() { return true; }   
	public boolean canwrite() { return true; }  
	public boolean interleaved() { return true; }  

	public BioseqWriterIface newWriter() { return new MsfSeqWriter(); }		
	public BioseqReaderIface newReader() { return new MsfSeqReader(); }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		int m, t;
		
    if (line.startsWith("!!")) {
      // GCG version 9+ comment line
      // seen - !!NA_SEQUENCE  !!AA_SEQUENCE !!AAPROFILE  !!NAPROFILE  !!AA_MULTIPLE_ALIGNMENT !!NA_MUL...
      // !!RICH_SEQUENCE
   		if ( ( line.startsWith("!!NA") || line.startsWith("!!AA")) 
      	  && line.indexOf("MULTIPLE_ALIGNMENT") >= 0) {
        formatLikelihood += 92;  
      	return true;
      	}
      return false;  
      }
		else if ( (m= line.indexOf("MSF:")) >= 0 
		  && (t= line.indexOf("Type:", m)) > m 
		  && line.indexOf("Check:", t) > t ) {
	      formatLikelihood += 95;
        if (recordStartline==0) recordStartline= atline;
	      return true;
	      }
    else
    	return false;
	}
}