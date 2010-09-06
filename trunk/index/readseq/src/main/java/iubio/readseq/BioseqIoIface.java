//iubio/readseq/BioseqIoIface.java
//split4javac// iubio/readseq/BioseqReaderIface.java date=25-Aug-1999

// BioseqReaderIface.java
// d.g.gilbert


package iubio.readseq;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.util.Hashtable;

import flybase.OpenString;



//split4javac// iubio/readseq/BioseqReaderIface.java line=18
public interface BioseqIoIface
{
	public int formatID(); //? formatID() ? hashCode of name or content-type 
	public void setFormatID(int id); //? Readseq sets id?
}

