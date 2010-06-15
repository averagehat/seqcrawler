package iubio.readseq;

import flybase.OpenString;


public class EmblSeqFormat extends BioseqFormat
{
	public String formatName() { return "EMBL|em"; }  
	public String formatSuffix() { return ".embl"; } 
	public String contentType() { return "biosequence/embl"; } 
	public BioseqReaderIface newReader() { return new EmblSeqReader(); }
	public BioseqWriterIface newWriter() { return new EmblSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }
	public boolean hasdoc() { return true; }
	
	protected boolean isAmino;
	
	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.startsWith(EmblSeqReader.kID)) {
      formatLikelihood += 80;
			if (line.indexOf(" AA.")>0) isAmino= true;
      if (recordStartline==0) recordStartline= atline;
      return false; //!?
      }
    else if (line.startsWith(EmblSeqReader.kAcc)) {
      formatLikelihood += 10;
      return false; //!
      }
    else if (line.startsWith(EmblSeqReader.kDesc)) {
      formatLikelihood += 10;
      return false; //!
      }
    else if (line.startsWith(EmblSeqReader.kSequence)) {
      formatLikelihood += 70;
      return false; //!?
      }
    else
    	return false;
	}

}