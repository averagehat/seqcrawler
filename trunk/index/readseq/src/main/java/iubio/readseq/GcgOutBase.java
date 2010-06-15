package iubio.readseq;

import iubio.bioseq.BaseKind;

class GcgOutBase extends OutBiobase
{
	public GcgOutBase( OutBiobaseIntf nextout) { super(nextout); }
	public int outSeqChar(int c) {
		if (outtest!=null) c= outtest.outSeqChar(c);   
		if (c==BaseKind.indelHard) return '.';
		else if (c==BaseKind.indelSoft) return '.';
		else if (c==BaseKind.indelEdge) return '.'; // for GCG-9/RSF, this should be ~
	 	else return c;
		}
}

