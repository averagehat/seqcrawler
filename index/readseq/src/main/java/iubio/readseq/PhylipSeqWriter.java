package iubio.readseq;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import Acme.Fmt;

//public
class PhylipSeqWriter extends InterleavedSeqWriter
{
	BufferedWriter tempOs;
	int lastlen;
	String lenerr;
	
	public void writeHeader() throws IOException { 
		super.writeHeader();
		if (!interleaved()) {
			// interleaveHeader(); //! need to count nseq, seqlen first !
			tempFile= Readseq.tempFile();
			tempOs= new BufferedWriter( new FileWriter(tempFile));
			douts= tempOs;
			}
		}
	
	public void writeRecordStart() {
		super.writeRecordStart();
   	opts.spacer = 10;
    opts.tab = 12; 
    l1  = -1; //??
		}

	public void writeDoc() {
		super.writeDoc();
			//!! must we TRuncate idword? -- probably, but causes hassles! e.g. #Mask cut
		writeString( Fmt.fmt( idword, 10, Fmt.TR + Fmt.LJ) + "  ");
    //linesout += 0; // no newline !
 		}

	protected void interleaf(int leaf) {
		writeln(); // is newline legal for phylip here?
		}
		
	protected void interleaveHeader() {
   	writeString(" " + nseq);  // these are 0 if not interleaved !?
    writeString(" " + seqlen);
  	if (interleaved()) 
     	writeln(); //sprintf( line, " %d %d\n", nseqs, nbases);  // " %d %d F\n"
   	else 
     	writeln(" I "); //sprintf( line, " %d %d I \n", nseqs, nbases);  // " %d %d FI \n"
	}

	public boolean setSeq( Object seqob, int offset, int length, String seqname,
						 Object seqdoc, int atseq, int basepart) 
	{
		if (lastlen > 0 && length != lastlen) {
			if (lenerr==null) lenerr= String.valueOf(lastlen) + " != ";
			lenerr += String.valueOf(length) + ", ";
			length= lastlen; // can we pad/trunc to first length?
			}
		else lastlen= length;
		return super.setSeq(seqob,offset,length,seqname,seqdoc,atseq, basepart);
	}
	
	public void writeTrailer()  // per file 
	{ 
		if (lenerr!=null) {
			BioseqReader.message("Warning: this format requires equal sequence lengths.");
			BioseqReader.message("       : lengths are padded/truncated to "+lastlen);
			BioseqReader.message("       : " + lenerr);
			}
			
		if (!interleaved()) {
			try { douts.close(); } catch (IOException ex) {} // == tempOs
					// reset output from temp to final stream
			setOutput(outs);

			interleaveHeader();  
			sequentialOutput();
			}
		super.writeTrailer();
	}
	
	
	protected void sequentialOutput()
	{
		try {
			Reader tempis= new FileReader(tempFile);
			char[] buf = new char[2048];
			int nread;
			while ( (nread= tempis.read(buf)) >= 0)  
				douts.write(buf, 0, nread);
			tempis.close();
			}
		catch (Exception ex) { ex.printStackTrace(); }
		//if (!(Debug.isOn)) 
		{ tempFile.delete(); tempFile= null; }
	}

	
};
