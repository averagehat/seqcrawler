//iubio/bioseq/SeqInfo.java
//split4javac// iubio/bioseq/SeqInfo.java date=12-Jul-2002

// iubio/bioseq/SeqInfo.java
// dgg oct'96++ (from dclap.biosequence.c++)


package iubio.bioseq;


//split4javac// iubio/bioseq/SeqInfo.java line=8
public class SeqInfo
{
	public final static short kOtherSeq= 0, kDNA= 1, kRNA= 2, kNucleic= 3, kAmino= 4, kIndelSeq= 5;
	protected short seqtype;
	protected int 	basecount;
	protected long 	checksum;
	
	public int getKind() { return seqtype; }
	public int getBaseCount() { return basecount; }
	public long getChecksum() { return checksum; }
	public void setKind(int seqtype) { this.seqtype= (short)seqtype; }
	public void setBaseCount(int 	basecount) { this.basecount= basecount; }
	public void setChecksum(long 	checksum) { this.checksum= checksum; }
	public boolean equals(SeqInfo si) {
		return (si.seqtype == seqtype && si.basecount == basecount && si.checksum == checksum);
		}
	public int[] getACGTcounts() { return null; }

			// stubs for implementers
	public void init(int maxlen) { }
	public void add( byte[] ba, int offset, int len) { } 
	public void add( char[] ca, int offset, int len) { } 
	public void add( String s, int offset, int len) { } 
	public void add( int c) { }

	public static SeqInfo getSeqInfo( int maxlen, boolean shorttest, boolean wantcrc) { 
			// need method to register other SeqInfo processers
		SeqKind sk= new SeqKind(maxlen, shorttest, wantcrc);
		return sk;
		}
	
	public String getKindLabel() { return getKindLabel(seqtype); }
	public static String getKindLabel(int kind) 	{
		switch (kind) {  
			case kAmino   : return "PROTEIN";  	//?"Protein";
			case kNucleic : return "NUCLEOTIDE"; //? "Nucleic";
 			case kRNA     : return "RNA";  
			default:
			case kIndelSeq:
			case kDNA     : 
			case kOtherSeq: return "DNA"; //? is other == DNA
			}
		}
		
}

//public interface DSeqChanges {
// 	public final static int kDelete = 1, kInsert = 2, kReorder = 4, kChange = 8; }
 
	
//! dang you Sun javac - I don't want to put this trivial public classes into separate files !

//public 
class SeqKind extends SeqInfo
{
	public static int kMaxSeqtest = 500;
	public boolean shorttest= true, wantcrc= false;
	protected int na, aa, po, nt, nu, ns, no, nd;
	protected int numa, numg, numc, numt;
	protected int testlen, maxlen; //maxlen
	
	public SeqKind() { init(0); }
	public SeqKind(int maxlen) { init(maxlen); }
	
	public SeqKind( int maxlen, boolean shorttest, boolean wantcrc) { 
		this.shorttest = shorttest;
		this.wantcrc = wantcrc;
		init(maxlen); 
		}
	
	public void init(int maxlen) {
		na = 0; aa = 0; po = 0; nt = 0; nu = 0; nd = 0; ns = 0; no = 0;
		numa= 0; numc= 0; numg= 0; numt= 0;
		basecount = 0;
	  seqtype= kOtherSeq;
		checksum = 0xffffffffL;
		//this.maxlen= maxlen;
		// aug04 bug fix, need to extend shorttest when see indel chars
	  this.maxlen= maxlen;
	  if (shorttest && (maxlen>kMaxSeqtest || maxlen<1))	testlen = kMaxSeqtest; 
	  else testlen= maxlen;
		}

	public long getChecksum() { return checksum ^ 0xffffffffL; }

	public int[] getACGTcounts() {
		int numo= basecount - numa - numc - numg - numt;
		return new int[] { numa, numc, numg, numt, numo };
		}
		
	public int getKind()
	{
	  if (basecount<2 || (no > 0) || (po+aa+na+nd == 0)) seqtype= kOtherSeq;
	 		//?? test probability of kOtherSeq ?, e.g.,
	  // else if (po+aa+na / maxtest < 0.70) return kOtherSeq;
	  else if (po > 0) seqtype= kAmino;
	  else if (aa == 0) { //  && na > 0 is implied here
	    if (na == 0 && nd>0) seqtype= kIndelSeq;
	    else if (nu > nt) seqtype= kRNA;
	    else seqtype= kDNA;
	    }
	  else if (na > aa) seqtype= kNucleic;
	  else seqtype= kAmino;
	  return seqtype;
	}

		// aug04 bug fix, need to extend shorttest when see indel chars
  private final boolean testmore() {
    return (testlen < maxlen && po==0 && aa == 0 && (nd > 0 || ns > 0 || no > 0));
    }
  
	private final void add1( int c) {
		//if (c>='a' && c<='z') c -= 32; // toupper
		basecount++;
		if (BaseKind.isProtonly(c)) po++;
    else if (BaseKind.isPrimenuc(c)) {
      na++;
      if (BaseKind.isDnanuc(c)) nt++;
      else if (BaseKind.isRnanuc(c)) nu++;
 			//? also count a,g,c,t,o for seqdoc ?
    	switch (c) {
      	case 'a': case 'A': numa++; break;
				case 'c': case 'C': numc++; break;  
				case 'g': case 'G': numg++; break; 
				case 't': case 'T': numt++; break;
      	}
      }
    else if (BaseKind.isAmino(c)) aa++;
		else if (BaseKind.isIndel(c)) nd++; // need to extend test to maxlen if here
    else if (BaseKind.isSeqsym(c)) ns++;
    else if (BaseKind.isAlphaseq(c)) no++;
		}

	private final void addcrc( int c) {
		if (c>='a' && c<='z') c -= 32; // toupper
		checksum = BaseKind.crctab[((int)checksum ^ c) & 0xff] ^ (checksum >> 8);
		}
		
	public final void add( byte[] ba, int offset, int len) {  
		int tlen= Math.min(len,testlen);
	  for (int i = 0; i < tlen; i++) add( (int)ba[offset+i] );
	  if (testmore()) { for (int i = tlen; i < maxlen; i++) add( (int)ba[offset+i] ); }
	  getKind(); // set seqtype !
		} 
		
	public final void add( char[] ca, int offset, int len) {  
		int tlen= Math.min(len,testlen);
	  for (int i = 0; i < tlen; i++) add( (int)ca[offset+i] );
	  if (testmore()) { for (int i = tlen; i < maxlen; i++) add( (int)ca[offset+i] ); }
	  getKind(); // set seqtype !
		} 
		
	public final void add( String s, int offset, int len) {  
		int tlen= Math.min(len,testlen);
	  for (int i = 0; i < tlen; i++) add( (int)s.charAt(offset+i) );
	  if (testmore()) { for (int i = tlen; i < maxlen; i++) add( (int)s.charAt(offset+i) ); }
	  getKind(); // set seqtype !
		} 
				
	public final void add( int c) {
		if (c > ' '  && (c < '0' || c > '9')) {
			add1(c); if (wantcrc) addcrc(c);
			}
		}
		
}


