package iubio.readseq;

import flybase.OpenString;


public class  GcgSeqFormat extends BioseqFormat
{
	public String formatName() { return "GCG"; }  
	public String formatSuffix() { return ".gcg"; } 
	public String contentType() { return "biosequence/gcg"; } 
	public BioseqReaderIface newReader() { return new GcgSeqReader(); }
	public BioseqWriterIface newWriter() { return new GcgSeqWriter(); }
	public BioseqWriterIface newWriter(int nseqs) { 
		// can't write more than one seq in this format !
		if (nseqs>1)
			return BioseqFormats.newWriter(
				BioseqFormats.formatFromContentType("biosequence/msf"),nseqs);
		else 
			return new GcgSeqWriter();
		}
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
    if (line.startsWith("!!")) {
      // GCG version 9+ comment line
      //if (recordStartline==0) return true; //? only if 1st line?
      // seen - !!NA_SEQUENCE  !!AA_SEQUENCE !!AAPROFILE  !!NAPROFILE  !!AA_MULTIPLE_ALIGNMENT !!NA_MUL...
      // !!RICH_SEQUENCE
      if (line.indexOf("MULTIPLE_ALIGNMENT") >= 0) {
      	// not Gcg -> MSF
      	formatLikelihood = 0; //??
      	return false;
      	}
      else if ( line.startsWith("!!NA") 
      	|| line.startsWith("!!AA")) {
      	  formatLikelihood += 92; //??
          return true; // are no others possible?
          }
     	else if ( line.startsWith("!!RICH_SEQUENCE")) { // RSF - need parser ??
      	  formatLikelihood += 92; // fix me - parse it or need other handler !
          return true;  
          }
      formatLikelihood += 50; //??
      return false; //!?
      }
		else if (line.indexOf("..")>0 && line.indexOf("Check:")>0) {
      formatLikelihood += 80; //92; //??
      //if (recordStartline==0) recordStartline= atline;
      return false; //!?
      }
    else
    	return false;
	}
}
