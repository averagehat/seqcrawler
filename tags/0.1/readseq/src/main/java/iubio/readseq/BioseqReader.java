//iubio/readseq/BioseqReader.java
//split4javac// iubio/readseq/BioseqReader.java date=28-Jun-2002

// iubio.readseq.BioseqReader.java -- was seqreader.java
// d.g.gilbert, 1990-1999

// ? fast version - read chunks to OpenString
	
package iubio.readseq;

import java.io.*;

import flybase.Debug;
import flybase.OpenString;
import flybase.Utils;

	// interfaces
import iubio.readseq.BioseqReaderIface;
import iubio.readseq.BioseqWriterIface;
import iubio.readseq.BioseqDoc;

import iubio.bioseq.BaseKind;
import iubio.bioseq.Bioseq;

	// can we do w/o these?
import iubio.readseq.SeqFileInfo;
import iubio.readseq.GenbankDoc;
	

 
//split4javac// iubio/readseq/BioseqReader.java line=28
public abstract class BioseqReader
	implements BioseqReaderIface
	// implements Cloneable
	//??? extends Reader
{
	public static String tempFile = "bioseqtemp.";
	public static boolean verbose;
	public static String lineEnd= "\n";  
	
	final static boolean useBioseqBytes= true;
	//public static String gBlankSeqid = "nameless";
	
	static { lineEnd= System.getProperty("line.separator"); }

		// these were public static  - not used that way?
	final static int kBigChunk= 500000; // 1M ? 
	int readChunkSize= 2048; //8192;
	int readFreeSize = readChunkSize * 100;
	//int kBigSeqBuf = readFreeSize;
			
	public BioseqReader() {
		margin	=  0;
		addfirst= false;
	  addend  = true; 
	  ungetend= false;
		}
		
	public BioseqReader(Reader ins) {
		this();
		setInput(ins);
		}


	// uncertain methods from Iface

	//public void processDocument( DocumentData e) {}
	//public void processBases( BaseData e) {}

	public static MessageApp messageapp= null;
	
	public static void message(String s) {
		// need opt to print to app text component
		if (messageapp!=null) messageapp.infomessage(s); else
		System.err.println(s);
		}


	// public accessors =============================
	
	//public int error() { return err; } // drop this for Exceptions
	//public int seqLen() { return seqlen; } //? drop seqlen, use seq.length() ?
	//public String seqId() { return seqid; } //? drop for SeqFileInfo.id
	
	public void setChoice(int seqchoice) { 
		choice= seqchoice; 
		}

	public int formatID() { return formatId; }
	public void setFormatID(int id) { formatId= id; } //? Readseq sets id?

	public Reader getInput() { return this.fIns; }

	public void setInput( InputStream ins) { 
		throw new Error("InputStream not supported");
		}

	public void setInput(Reader ins) {  
		this.fIns= ins; 
		fEof= false; 
		setReaderBuf(fIns);
		}

	public void doRead() throws IOException { 
		if (fIns==null) throw new FileNotFoundException(); //err= Readseq.eFileNotFound;
		read();
		
		// jun01 - dont make blank seqdoc? - want for fasta -mar02
		// ! but need to reinit/remake seqdoc w/ each entry - resetSeq !
		if (seqdoc==null) 
			seqdoc= new BasicBioseqDoc( seqid.toString()); //? to allow adding features?
			  // seqdoc= new GenbankDoc( seqid.toString()); 
		}


	public int getNseq() { return nseq; }
	protected void setNseq(int nsequences) { this.nseq= nsequences; }
	
	public int getReadChunkSize() { return readChunkSize; }
	public void setReadChunkSize(int chunksize) {
		readChunkSize= Math.min( kBigChunk , Math.max(chunksize, 2048)); // 2048; //8192;
		readFreeSize = Math.max(readChunkSize, Math.min(kBigChunk, readChunkSize * 100));
		Debug.println("BioseqReader.setReadChunkSize=" + readChunkSize);
		}
		
	protected void clearSeqBuffer()
	{
		if (maxseqlen > readFreeSize) {
			seqlen= maxseqlen= 0;
			bases= null;
			//? don't do this unless 1+ seqs are less than bigseq ?
			// garbage collect?
			}
	}
	
	public void resetSeq() {
		seqlen= seqlencount= 0; 
		allDone= false;
		idword= SeqFileInfo.gBlankSeqid;
		seqid= SeqFileInfo.gBlankSeqid;
		seqdoc= null; 
		//maxseqlen= 0; seq= null; //? leave storage for next and copy prior?
		}

	public void skipPastHeader(int skiplines) 
	{
  	int i;
    OpenString s;
    try {
	    for (i= skiplines; i > 0; i--) getline();
	    do {
	      s= getline();
	      if (s!=null) // skip also leading blank lines
	        for (i= s.length()-1; i>0 && s.charAt(i) <= ' '; i--) ;
	    } while (s!=null && i == 0 && !endOfFile());
    } catch (IOException e) {}
	}
	
	public void copyto( SeqFileInfo si) 
	{
			// method any? caller uses to get back seq data 
		if (si==null) return;
		if (si.err==0) si.err= err;
		si.seqlen= seqlen;
		si.seqdoc= seqdoc;
		if (si.err==0 && (seqlen>0 || seqdoc!=null)) { // && seq!=null
			si.atseq= atseq; //<< counted to current sequence in file
			if (atseq>si.nseq) si.nseq= atseq;
			si.seqid= seqid;
			si.checkSeqID();
			//? si.offset= seqoffset; // will this bugger up output - fix
// if (USEARRAY)
			if (seqlen>0) si.seq= new Bioseq( bases, 0, seqlen); //, useBioseqBytes
			else si.seq= null;
// else 
		/*
			Biobase[] bb= Bioseq.expand(seq.bases(), seqlen);
			si.seq= new Bioseq( bb);
 		*/
			clearSeqBuffer();
			}		
	}



	protected boolean skipdocs;
	public void setSkipDocs(boolean turnon) { skipdocs= turnon; }
		
		// from Readseq.readTo() -- more efficient to do here
	public void readTo( BioseqWriterIface writer, int skipHeaderLines)  throws IOException 
	{
		int skiplines= skipHeaderLines;
		// gain some speed w/ verbose formats by not parsing doc if writer doesn't handle it
	 	this.setSkipDocs( ! writer.wantsDocument());
		//! writer.writeHeader(); // not if calling more here more than once per output 
		
		boolean more= true;
		int atseqNum= 1; // must start w/ 1 not 0 !!
		for ( ; more; atseqNum++) {  
			this.skipPastHeader( skiplines);  skiplines= 0;
				// ^^ gosh - bug time - need this call even if skiplines == 0 - to load 1st line in buf
			SeqFileInfo sfi= this.readOne(atseqNum);
			
				//? modify here and like calls to create new output seqRecord for each seqdoc feature
				// indicated as separare record (e.g. source, gene, ...)
				
			if (sfi == null) more= false;
			else {
				if (Debug.isOn || verbose) 
					message("read "+atseqNum+" id=" + sfi.seqid + " seqlen=" + sfi.seqlen);
				if (writer.setSeq( sfi)) writer.writeSeqRecord();
				//?? if (fWriteMask && writer.setMask(sfi, sfi.gMaskName)) writer.writeSeqRecord();
				}
			}
			
		//! writer.writeTrailer();// not if calling more here more than once per output 
	}
				 
	public SeqFileInfo readOne(  int whichEntry) throws IOException
	{
		this.resetSeq();
		this.setChoice( whichEntry);  // endOfFile may depend on whichEntry == choice
    if ( this.endOfFile() ) return null; 
		
		this.doRead(); 
		if (err!=0) Debug.println("readOne error " + err);
		//? if (seqlen==0 && ??) return null; //?
		
		SeqFileInfo sfi= new SeqFileInfo();
		this.copyto( sfi);
		sfi.nseq= this.getNseq(); // or is this atseq?
   	return sfi;	
	}

	
	/*
	public Object clone() {
		try {
			BioseqReader c= (BioseqReader) super.clone();
			c.readbuf= null;
			c.nbuf= 0;
			c.seq= null;
			c.seqdoc= null; //??
			c.clearSeq();
			//c.ins= null;//??
	    return c;
			}
		catch(CloneNotSupportedException ex) { throw new Error(ex.toString()); }
		}*/
		
	
	public boolean endOfSequence() { return false; }

		
		

	
	// protected =============================	
	
	protected int formatId= -1;
	protected int			seqlen, seqoffset, seqlencount, maxseqlen, memstep, 
										// is this offset == seq-start-base?
										choice, nseq, margin = 0, err= 0, atseq= 0;
	protected boolean addit = true, addfirst = false, addend = true, 
										needString = false,
										allDone= false, ungetline= false, ungetend = false;
	protected String 	idword= SeqFileInfo.gBlankSeqid, seqid= SeqFileInfo.gBlankSeqid;

	protected Object  seqdoc;
	private   Bioseq	seq; // change to Object, or some more flexible class
	private   byte[]  bases; // local storage - faster than Bioseq 
									 // ? change to use/share Bioseq's bSeq buffer, and work w/ BioseqFiled class?
	
	protected final static int kUseTester= 1, kAnyChar= 2, kAlphaChar= 3;
	protected TestBiobase	testbase= new TestBiobase();
	protected int testbaseKind= kAlphaChar;
		
		
	// add data =============================	

	private final int testbase(int c) {
		switch (testbaseKind) {
  		default:
  		case kAlphaChar	: if (c<=' ' || (c >= '0' && c <= '9')) return 0; else return c;
  		case kAnyChar		: if (c<' ') return 0; else return c;
  		case kUseTester	: return testbase.isSeqChar(c); 
  		}
		}
	
	protected final void addseq( OpenString s, int offset) {
		addseq( s, offset, s.length()); 
		}
		
	protected void addseq( OpenString s, int offset, int nb)
	{
	  if (!addit) return;
		char[] cv= s.getValue();
		offset += s.getOffset();
		nb += offset;
		for (int i=offset; i<nb; i++) { 
	  	int c= testbase(cv[i]);
	    if (c > 0) { //? c>=0 ?
	      if (seqlen >= maxseqlen) if (!expand()) return;
// if (USEARRAY)
		   	bases[seqlen++]= (byte) c;  
// else
	     	//seq.setbase(seqlen++, new Biobase((char)c));
	      }
	    }
	}

			// special for Blast parser - others?
	protected void setSeqoffset(int seqoffset) 
	{
	  if (!addit) return;
	  this.seqoffset= seqoffset;
			// maybe later change Bioseq to handle offsets...
		byte offsetc= (byte)BaseKind.indelEdge; 
	  for (int i=0; i<seqoffset; i++) { 
      if (seqlen >= maxseqlen) if (!expand()) return;
// if (USEARRAY)
	   	bases[seqlen++]= offsetc;  
// else
     	// seq.setbase(seqlen++, new Biobase(offsetc));
      }
	}
	
	protected void addseq( char[] b, int offset, int nb) 
	{
	  if (!addit) return;
	  nb += offset;
	  for (int i=offset; i<nb; i++) { 
	  	int c= testbase( b[i]);
	    if (c > 0) {
	      if (seqlen >= maxseqlen) if (!expand()) return;
// if (USEARRAY)
		   	bases[seqlen++]= (byte) c;  
// else
	     	// seq.setbase(seqlen++, new Biobase((char)c));
	      }
	    }
	}

	protected final void countseq( OpenString s, int offset) {
		countseq( s, offset, s.length()); 
		}
		
	protected void countseq( OpenString s, int offset, int nb)
	{
		char[] cv= s.getValue();
		offset += s.getOffset();
	  nb += offset;
		for (int i=offset; i<nb; i++) { 
	    if (testbase( cv[i]) > 0) seqlencount++;  
	    }
	}
	
	 /** must count all valid seqq chars, for some formats 
	 		even if we are skipping seqq... */
	protected void countseq( char[] b, int offset, int nb)
	{
		nb += offset;
		for (int i=offset; i<nb; i++) { 
	    if (testbase( b[i]) > 0) seqlencount++; 
	    }
	}
	
	protected int countseqline( char[] b, int offset, int nb)
	{
		int count= 0;
		nb += offset;
		for (int i=offset; i<nb; i++) { 
	    if (testbase( b[i]) > 0) count++; 
	    }
	  return count;
	}

	protected void addinfo( String ids)
	{
		ids= Utils.formatNum(atseq,3) + ")  " + ids.trim() + lineEnd; //"\n";
		for (int i= 0; i<ids.length(); i++) { 
			char c= ids.charAt(i);
      if (seqlen >= maxseqlen) if (!expand()) return;
	   	bases[seqlen++]= (byte) c;  
      }
	}
	

	protected boolean expand() 
	{
			/* revise this to 
			(a) store all of long seqq on disk file (tmpnam()),
			  -- offload to disk instead of realloc until done reading
			(b) increase aStartLength if seqq is getting large -- larger chunksize
			(c) check ftell(V->f) to get max seqq size, reduce if see it is 
				 multiseq file?
			*/
  	maxseqlen += readChunkSize; //? do 2* increments? - better for huge seqs
   	memstep++;
   	/*#
		//protected	static Runtime javart= Runtime.getRuntime();
		if (memstep % 10 == 0 && javart.freeMemory() < readFreeSize) {
			// reduce memory fragmenting... write to disk, free, then malloc. 
			File tempname= new File(tempFile + new Random.nextInt());
			RandomAccessFile tempf= new RandomAccessFile(tempname, "rw");
			tempf.write( seq.bytes(), 0, seqlen);
			tempf.seek(0); // rewind
			
			seq= null;
			javart.gc(); // throw out garbage
			
			seq= new Bioseq(maxseqlen);
			if (seq != null) tempf.read( seq.bytes(), 0, seqlen);
			tempf.close();
			tempname.delete();
			}
		else 
		*/ 
		
//if (USEARRAY) 
		if (bases==null) bases= new byte[maxseqlen];
		else {
			byte[] tb= new byte[maxseqlen];
			System.arraycopy( bases, 0, tb, 0, seqlen);
			bases= tb;
			}
  	return (bases!=null);
//else
/*  
		if (seq==null) seq= new Bioseq(maxseqlen);
		else seq.expand(maxseqlen);
    if (seq==null || seq.bases()==null) {
    	throw new NullPointerException(); //err= Readseq.eMemFull;
      //return false;
      }
    else 
    	return true;
*/
    	 
	}

	
	// input data =============================	

		// input line
	protected int			nWaiting= 0;
	protected OpenString	sWaiting= new OpenString("");
	
		// input buffer
	private Reader fIns;
	private int lastc= -1;
	private final int kMaxbuf= 8192; //>> slower?? 20480; //10240;
	private int nbuf= 0;
	//private char[] readbuf;
	private boolean fEof= false;
	
	private OpenString osbuf;
	private char[] osval;
	private int oslinei, oslen, osnewlinesize= 1, insreadlen;
	private char osnewline; // add skipfactor for \r\n newlines?

	
	protected final char getreadbuf(int i) { return osval[oslinei+i]; }
	protected final void setreadbuf(int i,char c) { osval[oslinei+i]= c; }
	protected final char[] getreadchars() { return osval; }
	protected final int getreadcharofs() { return oslinei; }
	
	public boolean endOfFile() { return fEof; }
	public void ungetline()  { ungetline= true; }
  public int getInsReadlen() { return insreadlen; }
  
	private void setReaderBuf(Reader ins) {
		//osbuf= null; //?
		oslen= oslinei= 0;  
		insreadlen= 0; // aug03
		if (osbuf!=null) osbuf.setLength(0);
		}			
		
	public void reset() {
		nbuf= seqlen= seqlencount= err= atseq= 0;
		ungetline= allDone= false;
		fEof= false; 
		if (fIns!=null) try {
			fIns.reset();
			fEof= !fIns.ready();
			setReaderBuf(fIns); //bufins.reset(); //?
			}
		catch (IOException ex) { 
			Debug.println( getClass().getName() + ".reset() err=" + ex.getMessage());
			}
		}


	public void getlineBuf()  throws IOException  {
		if (ungetline) {
			ungetline= false;
			bufToString();
			}
		else  
			readLine();  
		}

	public OpenString getline()   throws IOException 
	{
		if (ungetline) {
			ungetline= false;
			bufToString();
			}
		else  
			readLine();
		return sWaiting;
	}
	
	
	final OpenString bufToOs(int offs, int len) {
		//! we need to make dup of readbuf to prevent overwrite
		int at= oslinei+offs;
		return osbuf.substring( at, at+len);
		}
					
	protected final void bufToString() {
		//sWaiting= bufToOs( 0, nbuf);
		int e= oslinei + nbuf;
		if (e > oslen) sWaiting= osbuf.substring( oslinei);
		else sWaiting= osbuf.substring( oslinei, e);
		nWaiting= sWaiting.length();
		}
	
	protected final void readLine() throws IOException {
		if (nbuf>0) { oslinei += nbuf; nbuf= 0; }
		if (osbuf==null) getOsBuf();
		int e= osbuf.indexOf( osnewline, oslinei);
		if (e<oslinei) {
			getOsBuf();		// may set fEof
			e= osbuf.indexOf( osnewline, oslinei);
			}
		if (e<oslinei) {
			if (oslinei>=oslen) sWaiting= new OpenString("");
			else sWaiting= osbuf.substring( oslinei);
			}
		else {
			int e1= e+osnewlinesize; if (e1>oslen) e1= oslen; // subtle off1 bug for /r/n newlines
			sWaiting= osbuf.substring( oslinei, e1); //+osnewlinesize for newline
			}
		nWaiting= nbuf= sWaiting.length();
		insreadlen += nWaiting; //??
		}

	/*
		// ! can combine readLineBuf & bufToString as readLine() for here
	protected final void readLine() throws IOException {
		readLineBuf();
		bufToString();
		}

	private void readLineBuf() throws IOException  
	{
		if (nbuf>0) { oslinei += nbuf; nbuf= 0; }
		if (osbuf==null) getOsBuf();
		int e= osbuf.indexOf( osnewline, oslinei);
		if (e<oslinei) {
			getOsBuf();		
			e= osbuf.indexOf( osnewline, oslinei);
			if (e<oslinei) e= oslen-1; // end of file!? if(fEof)?
			}
		nbuf= osnewlinesize + e - oslinei; if (nbuf<0) nbuf= 0;
		nWaiting= nbuf; 
	}	
	*/

	private void getOsBuf()  throws IOException {
		int offs, len, oldlen;
		if (osbuf==null) {
			offs= 0; 
			len= kMaxbuf;
			oldlen= len;
			osval= new char[len];
			osbuf= new OpenString(osval);
			oslinei= 0;
			oslen= 0;
			}
		else {
			offs= oslen - oslinei; // unused remainder
			len= kMaxbuf - offs;
			oldlen= osbuf.length();
			if (offs>0) System.arraycopy( osval, oslinei, osval, 0, offs); // shift down
				//!? or make new osval/osbuf and copy to - so existing OpenStrings on this buf don't get mangled
			oslinei= 0;
			oslen= offs;
			}

		int rlen= fIns.read( osval, offs, len);  
		
		//Debug.println("getOsBuf("+offs+","+rlen+")");
		if (rlen<0) { if (oslen<=0) fEof= true; } 
		else oslen += rlen;
		//insreadlen += rlen; -- not here, wait till read by caller
		if (oslen!=oldlen) osbuf= new OpenString(osval,0,oslen); // in case oslen changes from kMaxbuf??
		if (osnewline==0) {
			osnewlinesize= 1;
			for (int i=0; i<oslen; i++) {
				if (osval[i] == '\n')  { 
					osnewline= '\n'; 
					if (osval[i+1] == '\r') osnewlinesize= 2;
					break; 
					}
				else if (osval[i] == '\r')  { 
					osnewline= '\r'; 
					if (osval[i+1] == '\n') osnewlinesize= 2;
					break; 
					}
				}
			}
		}
		

	protected int indexOfBuf(char c) {
		return sWaiting.indexOf( c);
		}
		
	protected int indexOfBuf( String s) {
		return sWaiting.indexOf( s);
		}

	protected final OpenString bufSubstring( int offset, int endoff) {
		return bufToOs(offset, endoff-offset); 
		//return new OpenString( readbuf, offset, endoff-offset);
		}
	protected final OpenString bufSubstring( int endoff) {
		return bufToOs(0, endoff); // new OpenString( readbuf, 0, endoff);
		}
	
	////////////////


		// this method is for subclassing 
	protected void read() throws IOException { 
		readLoop(); 
		}	

	protected void readLoop()  throws IOException  
	{
		atseq++;
		//if (Debug.isOn) Debug.println( "readLoop: "+atseq+":"+seqid.trim());
	  if (choice == kListSequences) addit = false;
	  else addit = (atseq == choice);
	  if (addit) seqlen = seqlencount= 0;

	  if (addfirst) addseq( sWaiting, 0, nWaiting);
		boolean done;
	  do {
	    getlineBuf();
	    // if (needString) bufToString();//! getlineBuf() here does this: 
	   	done = endOfFile() || endOfSequence();
	    if (addit && (addend || !done) && (nWaiting > margin)) {
	      addseq( sWaiting, margin, nWaiting-margin);
	    }
	  } while (!done);

	  if (choice == kListSequences) addinfo(seqid);
	  else {
	   	allDone = (atseq >= choice);
	    if (allDone && ungetend) ungetline();
	    }
	}


};







//public 
