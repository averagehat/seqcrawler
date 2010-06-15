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



public interface BioseqReaderIface
	extends BioseqIoIface
{
		/** flag for setChoice() to return only list of sequence IDs */
	public final static int kListSequences =  -1; 

		/** set input stream */
	public void setInput( Reader ins);
	//public void setInput( InputStream ins); // not using byte-input streams now

		/** end of sequence input stream was reached */
	public boolean endOfFile();
	
		/** reset input stream for re-read (interleaved formats) */
	public void reset();			
	
		/** reset sequence storage for new sequence */
	public void resetSeq();  

		/** select which sequence to read 
			* @param seqchoice - index from 1 of sequence in stream
			*/
	public void setChoice(int seqchoice);
			
		/** get total number of sequences read <p>
			* may not be valid till all data read, 
			* or return number of current sequence after doRead() 
			*/
	public int  getNseq();  
	
		/** read next sequence to internal data
			* @see setChoice()
			*/
	public void doRead() throws IOException;
	
		/** copy last read sequence to storage record */
	public void copyto( SeqFileInfo si); 
	
		/** skip past non-sequence info at top of stream <p>
			* @param skiplines - presumed number of lines to skip past
			*/
	public void skipPastHeader(int skiplines);
	
		/** reads all of sequence entries in stream, writing to writer <p>
			* for piping from one format to another 
			*/
	public void readTo( BioseqWriterIface writer, int skipHeaderLines)  throws IOException;

		/** read one sequence to SeqFileInfo structure */
	public SeqFileInfo readOne( int whichEntry) throws IOException;
	
};






