package iubio.readseq;

// here only for classic Readseq compatibility, see PhylipSeqFormat
public class Phylip2SeqFormat extends PhylipSeqFormat 
{
	public Phylip2SeqFormat() { super(); }
	public String formatName() { return "Phylip3.2"; }  
	public String formatSuffix() { return ".phylip2"; } 
	public String contentType() { return "biosequence/phylip2"; } 
	public BioseqWriterIface newWriter() { 
		PhylipSeqWriter c= new PhylipSeqWriter(); 
		c.setinterleaved(false); // always for this format?
		return c; 
		}
}