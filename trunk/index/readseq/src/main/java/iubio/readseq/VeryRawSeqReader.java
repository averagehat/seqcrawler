package iubio.readseq;

import flybase.Debug;
import iubio.bioseq.BioseqFiled;

import java.io.IOException;
import java.util.Vector;

//add VeryRawSeqFormat? - only bases, no newlines, formatting, id, ...
//or change plain to expect no newlines for single seq, mulitple lines == multiple entries

public class VeryRawSeqReader  extends PlainSeqReader
{
	// very simpler format -- no newlines, or anything but seq chars - use w/ BioseqFiled for large files
	protected Vector fileseqv;
	protected BioseqFiled filedseq;
	protected static Integer gNullfile= new Integer(0);
	protected boolean endOfFile;
	
	public VeryRawSeqReader() {
		margin	=  0;
		addfirst= true;
		addend 	= true;  
		ungetend= false;
		fileseqv= new Vector();
		}

	public boolean endOfFile() { if (inputFile!=null) return endOfFile; else return super.endOfFile(); }
	
	protected void read() throws IOException 
	{ 
		if (inputFile!=null) {
			atseq++;
		  if (choice == kListSequences) addit = false;
		  else addit = (atseq == choice);
		  if (addit) seqlen = seqlencount= 0;
		  
			seqid= SeqFileInfo.cleanSeqID(inputFile.getName());  
			seqid= SeqFileInfo.getNextBlankID(seqid);

		  if (addit) {
				filedseq= new BioseqFiled( inputFile, false);
				int foundends= filedseq.scanForEnd( atseq, (atseq>1) ? fileseqv.elementAt(atseq-2) : null);
					//^ scanForEnd sets file offset, length for seq
				if (foundends<atseq || filedseq.endOfFile()) { endOfFile= true; allDone= true; }  
				
				seqlencount= seqlen= filedseq.length();
				
				while (fileseqv.size()<atseq) fileseqv.addElement( gNullfile);
				fileseqv.setElementAt( filedseq, atseq-1);
				}
			
			
			//atseq= 1; // only 1? -- NO, one per line !
			//seqlencount= seqlen= filedseq.length();
			//allDone= true;
			
		  if (choice == kListSequences) addinfo(seqid); 
		  else {
		   	allDone = (atseq >= choice);
		    }
			
			Debug.println("VeryRawSeqReader from BioseqFiled, id="+seqid+", length="+seqlen+", file="+inputFile);
			}
		else
			super.read(); 
	}	

	public void copyto( SeqFileInfo si) 
	{
		if ( filedseq==null ) { super.copyto(si); return; }
		if (si==null) return;
		if (si.err==0) si.err= err;
		si.seqlen= seqlen;
		si.seqdoc= seqdoc;
		if (si.err==0 && (seqlen>0 || seqdoc!=null)) { 
			si.atseq= atseq; //<< counted to current sequence in file
			if (atseq>si.nseq) si.nseq= atseq;
			si.seqid= seqid;
			si.checkSeqID();
			//? si.offset= seqoffset; // will this bugger up output - fix

			si.seq= filedseq;
			//if (seqlen>0) si.seq= new Bioseq( bases, 0, seqlen); //, useBioseqBytes
			//else si.seq= null;
				
			clearSeqBuffer(); // sets bases= null - filedseq= null?
			filedseq= null;
			}		
	}

}
