//iubio/readseq/ReadseqException.java
//split4javac// iubio/readseq/Writeseq.java date=28-May-2001

// Writeseq.java
// d.g.gilbert, 1990-1999


package iubio.readseq;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

//import iubio.bioseq.Bioseq;

import flybase.OpenString;

 
// ReadseqException doesnt need to be public, but dang javac splitter wont do unless told otherwise

//split4javac// iubio/readseq/Writeseq.java line=18
public class ReadseqException extends IOException
{
	public ReadseqException() { 
		super(); 
		}
	public ReadseqException(String err) { 
		super(err); 
		}
//  public ReadseqException(String message, Throwable cause) {
//		super(err, cause); // java 1.4
//		}
}

/**
 * Writeseq is public wrapper for classes of BioseqWriter ancestry <p>
 * collect and write a batch of sequences to output stream
 *
 * @author  Don Gilbert
 * @version 	Jul 1999
 * @see iubio.readseq.Readseq
 */

