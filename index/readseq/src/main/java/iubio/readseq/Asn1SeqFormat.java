package iubio.readseq;

import flybase.OpenString;




public class  Asn1SeqFormat extends BioseqFormat
{
	public String formatName() { return "ASN.1"; }  
	public String formatSuffix() { return ".asn"; } 
	public String contentType() { return "biosequence/asn1"; } 
	public BioseqReaderIface newReader() { return new Asn1SeqReader(); }
	public BioseqWriterIface newWriter() { return new Asn1SeqWriter(); }
	public boolean canread() { return false; }
	public boolean canwrite() { return false; }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if ( line.indexOf("::=")>=0 &&
       ( line.indexOf("Bioseq")>=0 ||       // Bioseq or Bioseq-set 
        line.indexOf("Seq-entry")>=0 ||
        line.indexOf("Seq-submit")>=0 ) ) { 
		      formatLikelihood= 90;
		      return true;
		      }
    else
    	return false;
	}
}