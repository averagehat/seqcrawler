package iubio.readseq;

import iubio.bioseq.BaseKind;


//public
class PlainSeqWriter  extends BioseqWriter
{
	public void writeRecordEnd() { if (!isoneline) writeln(); }  // writeloop does at least one newline at end of seq

	public void setOpts(WriteseqOpts newopts) { 
		if (newopts!=null) {
			// pick only relevant opts .seqwidth
			opts.seqwidth= newopts.seqwidth;
			}
		}
		
	// simple oneline seq output ?
	protected final int kMaxlongbuf = 2048;
	protected boolean isoneline;
	
	protected void writeLoop() // per sequence
	{
		if (opts.seqwidth < 9999 && opts.seqwidth > 0) // so width=0 width=-1 width=9999+ will make 1 line
			super.writeLoop();
		else { 
			isoneline= true;
			int bioseqlen= Math.min(offset+seqlen, bioseq.length());
			char[]  bs= new char[kMaxlongbuf];
		  for (int i=0, bufl=0 ; i < seqlen; ) {
	 	    if (l1 < 0) l1 = 0;
		    l1++; 
	      char bc;
	      if (offset+i>=bioseqlen) bc= BaseKind.indelEdge;   
	      else bc= (char)bioseq.base(offset+i,fBasePart); 
	      i++; 
	      if (testbaseKind == kUseTester) bc= (char) testbase.outSeqChar( bc);
	      if (bc>0) bs[bufl++] = bc; 
	    	if (bufl >= kMaxlongbuf || l1 == opts.seqwidth  || i == seqlen) { //? no || l1 == opts.seqwidth 
	    	  int buflen= bufl;
	     	 	bufl = 0;  l1 = 0;
	      	writeByteArray( bs, 0, buflen);
		    	if (i == seqlen || l1 == opts.seqwidth) writeln(); //writeSeqEnd();   //  
		      }
		  	}
			}
	}
	
		
};
