//iubio/readseq/InterleavedSeqWriter.java
//split4javac// iubio/readseq/InterleavedSeqReader.java date=13-Jun-2003

// InterleavedSeqReader.java -- was seqread2.java
// low level readers & writers : interleaved formats
// d.g.gilbert, 1990-1999


package iubio.readseq;


import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;

import Acme.Fmt;
import flybase.Debug;
import flybase.OpenString;
//import flybase.Native;

import iubio.bioseq.BaseKind;
import iubio.bioseq.SeqInfo;
import iubio.bioseq.Bioseq;

// PaupSeqFormat reader needs work/test
// PrettySeqFormat not readable
// OlsenSeqFormat not readable yet

 

//split4javac// iubio/readseq/InterleavedSeqReader.java line=61
public abstract class InterleavedSeqWriter  extends BioseqWriter
{
	protected File tempFile;
	protected FileIndex fileIndex;
	protected String saveLineEnd;
	protected boolean interleaved= true, ifirst= true;
	protected int leafLines; // == linesout from BioseqWriter
	protected int lastlen;
	protected String lenerr;

	public InterleavedSeqWriter() {}
	
	
	public void finalize() throws Throwable  
	{ 
		//if (!(Debug.isOn)) 
		if (tempFile!=null) { tempFile.delete(); tempFile= null; }
		super.finalize();
	}
		
	public boolean interleaved() { return interleaved; }
	public void setinterleaved(boolean turnon) { interleaved= turnon; }

	public void writeRecordStart() 			// per seqq
	{
		if (interleaved()) fileIndex.indexit();  
		super.writeRecordStart();
	}
	
	public void writeRecordEnd() { 
		ifirst= false; //debug
		super.writeRecordEnd(); 
		}  
	
	protected void writeln() {
		super.writeln(); 
		if (ifirst) leafLines++; 
		}

	public void writeHeader() throws IOException 			// per file
	{ 
		super.writeHeader();
		ifirst= true; leafLines= 0;
			// redirect output per seqq to temp file after main header
		if (interleaved()) {
			try {
				//if (Debug.isOn)	
				//	tempFile= new File( dclap.DApp.application().getCodePath(), "interleave.tmp");
				//else 
					tempFile= Readseq.tempFile();
				Writer tos= new BufferedWriter( new FileWriter(tempFile));
				//douts= new DataOutputStream(tos);
				fileIndex= new FileIndex(tos);
				douts= fileIndex;
				saveLineEnd= lineSeparator;
				lineSeparator= "\n"; // Aaaarrgggggggghhhhhhhh!!!!!! for readLine by RandomAccessFile
				}
			catch (IOException ex) { ex.printStackTrace(); }
			}
	}
	
	public void writeTrailer()  // per file 
	{ 
		if (lenerr!=null) {
			BioseqReader.message("Warning: this format requires equal sequence lengths.");
		  BioseqReader.message("       : lengths may be padded/truncated to first."); //+lastlen);
			BioseqReader.message("       : first length " + lenerr);
			}

		if (interleaved()) {
			lineSeparator= saveLineEnd; // Aaaarrgggggggghhhhhhhh!!!!!!
			fileIndex.indexEOF(); 
			try { douts.close(); } catch (IOException ex) {}
					// reset output from temp to final stream
			setOutput(outs);
			/*
			if (outs instanceof BufferedWriter) douts=(BufferedWriter)outs ;//DataOutputStream
			else douts= new BufferedWriter(outs);//DataOutputStream
			*/
			interleaveHeader(); //??
			interleaveOutput();
			}
		super.writeTrailer();
	}

	protected int seqLen() { // override
	  return (lastlen > 0) ? lastlen : seqlen;
	  //return seqlen; //? any problems using lastlen ?
	  }
	
	public boolean setSeq( Object seqob, int offset, int length, String seqname,
						 Object seqdoc, int atseq, int basepart) 
	{
		if (lastlen > 0 && length != lastlen) {
			if (lenerr==null) lenerr= String.valueOf(lastlen) + " != ";
			lenerr += String.valueOf(length) + ", ";
			if (length>lastlen) length= lastlen; // can we pad/trunc to first length?
			}
		else lastlen= length;
		return super.setSeq(seqob,offset,length,seqname,seqdoc,atseq, basepart);
	}
	
 
	protected void interleaveHeader() {}
	protected void interleaf(int leaf) {}
	
	protected void writePadLine(RandomAccessFile tempis, int iseq, long starti) {}
	
	protected void interleaveOutput()
	{
		int sn= fileIndex.indexCount();
		long[] sindex= fileIndex.indices();
		long[] atindex= fileIndex.newIndices(); // test to speed up long lists
		int nlines = linesout; // # lines written for last seqq (set 0 each writeInit)
					// ?is linesout a safe value for # leaf lines?
		Debug.println("n leaf linesout="+linesout+", leafLines="+leafLines);
		//n leaf linesout=95, leafLines=92 filines=0

		try {
			RandomAccessFile tempis= new RandomAccessFile(tempFile, "r");
			for (int leaf=0; leaf<nlines; leaf++) {				
				for (int iseq=0; iseq<sn; iseq++) {
					String line= "";
					long starti= sindex[iseq];
					long ati= atindex[iseq];
					long endi= sindex[iseq+1];
					if (ati >= starti && ati <= endi) {
						tempis.seek(ati);
						line= tempis.readLine();
 						}
					else {
						tempis.seek(starti);
						for (int iline=0; iline <= leaf; iline++)
				  		line= tempis.readLine();
				  		// ^  save the last file index instead of reading to leaf each iter ?
				  	}
					long fini= tempis.getFilePointer();
				 	atindex[iseq]= fini;
				 	
				 	if (fini <= endi) {
				  	if (line!=null && line.length()>0) writeString( line);  //readLine DOESN'T retain newline 
				  	else if (leaf < leafLines) writePadLine(tempis,iseq,starti); // may06: for short seqs, pad?
	      		writeln();
				  	}
		      else if (leaf < leafLines) { writePadLine(tempis,iseq,starti); writeln();} // may06: for short seqs, pad?
				 }
			 interleaf(leaf); 
			 }
			tempis.close();
			}
		catch (Exception ex) { ex.printStackTrace(); }
		//if (!(Debug.isOn)) 
		{ tempFile.delete(); tempFile= null; }
	}
	
}




class FileIndex extends BufferedWriter
{
	long[]	fIndices;
	int	fMax, fNum;
	long 	written;
	
	FileIndex(Writer wr) 
	{
		super(wr);
		fNum= 0;
		fMax= 20;
		fIndices= new long[fMax];
		written= 0;
	}

 	int  	indexCount() { return fNum; }
 	
 	long[] indices() { return fIndices; }
	long[] newIndices() { 
		long[] itmp= new long[fNum];
		for (int i= 0; i<fNum; i++) itmp[i]= fIndices[i];
		return itmp; 
		}

	void indexit( long index)
	{
		if (fNum >= fMax) {
			fMax *= 2; 
			long[] itmp= new long[fMax];
			for (int i= 0; i<fNum; i++) itmp[i]= fIndices[i];
			fIndices= itmp;
			}
		fIndices[fNum++]= index;
	}

	public void write(char cbuf[], int off, int len) throws IOException {
		super.write(cbuf, off, len);
		written += len;
		}
	public void write(int c) throws IOException {
		super.write(c);
		written += 1;
		}
	public void write(String s, int off, int len) throws IOException {
		super.write(s,off,len);
		written += len;
		}

	public long size() { return written; }
	
	//final void indexit(DataOutputStream daos) { indexit( daos.size()); }
	final void indexit() { indexit( this.size()); }

	final void indexEOF() {
		indexit( this.size());
		fNum--;
		}

};





	// here only for classic Readseq compatibility, see PhylipSeqFormat
