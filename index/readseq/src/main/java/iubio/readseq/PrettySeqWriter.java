package iubio.readseq;

import java.io.IOException;

import Acme.Fmt;



//public
class PrettySeqWriter extends InterleavedSeqWriter
{
	boolean firstseq;
	int interline;
	
	public void writeRecordStart() 			// per seqq
	{
		super.writeRecordStart(); // super does opts.plainInit()
		//if (!opts.userchoice) opts.prettyInit();
		if (firstseq) { opts.numwidth= Fmt.fmt(seqlen).length() + 1; firstseq= false; }
	}
		
	public void writeHeader()  throws IOException			// per file
	{ 
		super.writeHeader();
		if (!opts.userchoice) opts.prettyInit();
		interline= opts.interline;
		firstseq= true;
		if (opts.numtop) {
			// seqlen must be set to min/max seqlen
			opts.numline = 1;
			if (interleaved()) fileIndex.indexit(); 
			writeSeq(); // write number line (numline==1)
			opts.numline = 2;
			if (interleaved()) fileIndex.indexit(); 
			writeSeq(); // write tic line (numline==2)
			opts.numline = 0;
			}
	}

	protected void interleaf(int leaf) {
		for (int i= interline; i>0; i--) writeln(); // is newline legal for phylip here?
		}
	
	public void writeTrailer()  // per file 
	{ 
		if (opts.numbot) {
			opts.numline = 2;
			if (interleaved()) fileIndex.indexit();  
			writeSeq(); // write tic line (numline==2)
			opts.numline = 1;
			if (interleaved()) fileIndex.indexit(); 
			writeSeq(); // write number line (numline==1)
			opts.numline = 0;
			}
		super.writeTrailer();
	}
		
};