package iubio.readseq;

import flybase.OpenString;



public class ClustalSeqFormat extends BioseqFormat
{		
	public String formatName() { return "Clustal";  }  
	public String formatSuffix() { return ".aln"; }  
	public String contentType() { return "biosequence/clustal"; }  
	public boolean canread() { return true; }   
	public boolean canwrite() { return true; }  
	public boolean interleaved() { return true; }  

	public BioseqWriterIface newWriter() { return new ClustalSeqWriter(); }		
	public BioseqReaderIface newReader() { return new ClustalSeqReader(); }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		////CLUSTAL W (1.8) multiple sequence alignment
		if ( line.startsWith("CLUSTAL ") ) {
	      formatLikelihood  = 95;
        if (recordStartline==0) recordStartline= atline;
	      //if (line.indexOf("multiple sequence alignment")>0) return true; else 
	      return true; // ??
	      }
    else
    	return false;
	}
}
