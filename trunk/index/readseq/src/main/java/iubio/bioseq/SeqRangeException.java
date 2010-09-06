//iubio/bioseq/SeqRangeException.java
//split4javac// iubio/bioseq/SeqRange.java date=26-Jun-2001

// SeqRange.java
// sequence location parsing
// d.g.gilbert, 1997++


package iubio.bioseq;

//split4javac// iubio/bioseq/SeqRange.java line=8
public class SeqRangeException extends Exception
{
	public SeqRangeException() { super(); }
	public SeqRangeException(String err) { super(err); }
}

/**
 parse, hold and edit biosequence feature locations <pre>
 examples to parse 
 258
 255..457
 105^106
 order(M55673:2559..>3688,<1..254)
 join(M55673:1820..2274,M55673:2378..2558,255..457)
 </pre>
*/

//public interface DSeqChanges1 { // dup of biosequence.java iface !
// 	public final static int kDelete = 1, kInsert = 2, kReorder = 4, kChange = 8;
//};


