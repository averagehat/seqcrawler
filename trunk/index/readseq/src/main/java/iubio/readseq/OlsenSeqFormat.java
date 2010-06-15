package iubio.readseq;

import flybase.OpenString;

public class OlsenSeqFormat extends BioseqFormat
{		
	public String formatName() { return "Olsen";  }  
	public String formatSuffix() { return ".olsen"; } // .gb ?
	public String contentType() { return "biosequence/olsen"; } // genbank ?
	
	public boolean canread() { return false; } //! need some work!
	public boolean canwrite() { return false; } // do as genbank
	public boolean interleaved() { return true; }

	public BioseqWriterIface newWriter() { 
		// Olsen editor can read GenBank
		return BioseqFormats.newWriter( 
			BioseqFormats.formatFromContentType("biosequence/genbank"),-1);
		}
		
	public BioseqReaderIface newReader() { return new OlsenSeqReader(); }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.indexOf("identity:   Data:") >=0) {
      formatLikelihood += 95;
      return true;
      }
    else
    	return false;
	}
}
