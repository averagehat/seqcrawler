package iubio.readseq;

import iubio.bioseq.BaseKind;



class PaupOutBase extends OutBiobase
{
	public PaupOutBase(OutBiobaseIntf nextout) { super(nextout); }
	public int outSeqChar(int c) {
		if (outtest!=null) c= outtest.outSeqChar(c);   
		if (c==BaseKind.indelSoft) return BaseKind.indelHard;
		else if (c==BaseKind.indelEdge) return BaseKind.indelHard;  
	 	else return c;
		}
}
