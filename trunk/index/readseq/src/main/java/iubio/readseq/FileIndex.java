package iubio.readseq;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

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
		//fIndices[fNum++]= 0; //????
		written= 0;
	}

 	long[] indices() { return fIndices; }
 	int  	indexCount() { return fNum; }
	
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

