package iubio.readseq;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Hashtable;

public interface BioseqWriterIface
extends BioseqIoIface
{
public void setOutput( Writer outs);
public void setOutput( OutputStream outs); // mainly for System.out
public void close() throws IOException;
public int getError();

	// add some opts - dochecksum, ??
	/** true if reader should collect documentation/features for this writer.
			false can mean speedier processing. */
public boolean wantsDocument();
	
		/** per output stream */
public void setNseq( int nsequences);
public void writeHeader() throws IOException;  
public void writeTrailer();  


		/** per sequence */
public boolean setSeq( SeqFileInfo si);  
public boolean setMask( SeqFileInfo si, String masktag);
public boolean setSeq( Object seqob, int offset, int length, String seqname,
					 Object seqdoc, int atseq, int basepart);  

public void setSeqName( String name); //? don't need in iface? 

	/** set list of features to extract sequence of (good only if hasdoc) */
public void setFeatureExtraction(Hashtable featurelist);  

	/** get output translation/conversion of sequence chars */
public OutBiobaseIntf getOutputTranslation();

	/** set output translation/conversion of sequence chars */
public void setOutputTranslation( OutBiobaseIntf tester);

	/** write a full seq record, given setSeq() <p>
		* implementation does writeRecordStart(), writeDoc(), writeSeq(), writeRecordEnd()
		*/
public void writeSeqRecord() throws IOException;  
		
	/** start of sequence record, initialize per seq, subclasses customize as needed */
public void writeRecordStart();  

	/**  write documentation for record, form where all doc is known from setSeq() */
public void writeDoc();   

	/**  write sequence data, when all seq is known from setSeq()  */
public void writeSeq();  

	/**  end of seq before newline or end of record - called at end of writeSeq() generally */
public void writeSeqEnd();  

	/**  end of seq record  */
public void writeRecordEnd();  

};
