package iubio.readseq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;

public abstract class InterleavedSeqWriter  extends BioseqWriter
{
	protected File tempFile;
	protected FileIndex fileIndex;
	protected String saveLineEnd;
	protected boolean interleaved= true;
	
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

	public void writeHeader() throws IOException 			// per file
	{ 
		super.writeHeader();
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
	
	protected void interleaveHeader() {}
	protected void interleaf(int leaf) {}
	
	protected void interleaveOutput()
	{
		int sn= fileIndex.indexCount();
		long[] sindex= fileIndex.indices();
		int nlines = linesout; // # lines written for last seqq (set 0 each writeInit)
		try {
			RandomAccessFile tempis= new RandomAccessFile(tempFile, "r");
			for (int leaf=0; leaf<nlines; leaf++) {
				for (int iseq=0; iseq<sn; iseq++) {
					String line= "";
					tempis.seek( sindex[iseq]);
					for (int iline=0; iline <= leaf; iline++)
				  	if ((line= tempis.readLine())==null) line= "";
				 	if (tempis.getFilePointer() <= sindex[iseq+1]) {
				  	writeString( line);  //readLine DOESN'T retain newline??????
	      		writeln();
				  	}
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
