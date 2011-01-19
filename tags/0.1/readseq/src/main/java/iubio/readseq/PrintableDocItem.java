//iubio/readseq/PrintableDocItem.java
//split4javac// iubio/readseq/BioseqDocItems.java date=07-Jun-2001

// BioseqDocItems.java
// sequence format information (document,features) handlers
// d.g.gilbert, 1997++


package iubio.readseq;


import java.io.*;
import java.util.*;

import flybase.*;
import iubio.bioseq.*;

//split4javac// iubio/readseq/BioseqDocItems.java line=15
public interface PrintableDocItem
{
	public void print( PrintWriter pr, BioseqDocImpl doc, String label, String value);
}

