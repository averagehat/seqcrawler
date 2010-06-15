package iubio.readseq;

import flybase.OpenString;
import iubio.bioseq.SeqInfo;

public class PlainSeqFormat extends BioseqFormat
{
	public String formatName() { return "Plain|Raw"; }  
	public String formatSuffix() { return (reallyRaw) ? ".raw" : ".seq"; }  
	public String contentType() { return  (reallyRaw) ? "biosequence/raw" : "biosequence/plain"; } 
	//public BioseqReaderIface newReader() { return new PlainSeqReader(); }
	public BioseqWriterIface newWriter() { return new PlainSeqWriter(); }
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }

	protected int nnewlines;
	protected boolean reallyRaw;
	protected SeqInfo seqkind;
	public void setSeqInfoTester(SeqInfo si) { seqkind= si; }

	public void setVariant(String varname) {
		if ("raw".equalsIgnoreCase(varname)) reallyRaw= true;
		// format tester cant distinguish multi-record raw and line-broken plain sequence
		}

	public void formatTestInit() { 
		super.formatTestInit(); 
		nnewlines= 0;
		reallyRaw= false;
		seqkind= null;
		}

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		nnewlines= atline;
		/*  // dang - caller eats newlines from line !
		if (nnewlines < 2 && line.length()>0) {
			char endc= line.charAt( line.length()-1 );
			if ( endc == '\n' || endc == '\r' ) nnewlines++; 
			else if (atline > 0 && nnewlines>0) nnewlines++; // partial 2nd line
			}
		*/
 		return false;
	}

	public int formatTestLikelihood() { 
		if (seqkind!=null) {
			if (seqkind.getKind() == SeqInfo.kOtherSeq) reallyRaw= false;
			else reallyRaw= (nnewlines < 2);  
			}
		return formatLikelihood; 
		}

	public BioseqReaderIface newReader() {
		if (reallyRaw) return new VeryRawSeqReader(); 
		else return new PlainSeqReader();
		}

}
