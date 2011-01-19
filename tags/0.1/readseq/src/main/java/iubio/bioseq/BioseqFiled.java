//iubio/bioseq/BioseqFiled.java
//split4javac// iubio/bioseq/BioseqFiled.java date=06-Jun-2001

// iubio/bioseq/BioseqFiled.java
// file, persistant Bioseq for large sequences (e.g. 300+ MB human chromosome)

package iubio.bioseq;


import java.io.File;
import java.io.RandomAccessFile;
import flybase.Native; // for java1.1 tempFile()
import flybase.Debug;


//split4javac// iubio/bioseq/BioseqFiled.java line=12
public class BioseqFiled 
	extends Bioseq 
{
	
	//protected boolean useByteArray= gUseByteArray;
	//protected byte[] bSeq;
	// use bSeq for holding portion of seq, and keep file offset, length
	
	static final int kMaxbuf = 20480; //? for bSeq buffer of file
	protected RandomAccessFile raf;
	protected File file;
	protected boolean istempfile, readwrite;
	protected boolean needsFiling;  // if bSeq has been changed and not filed
	
	protected long bufoffset;  // or int? use with base indexing to keep bSeq in valid range
	protected long bufend;   // this one indexes end of bSeq buf, <= flength
	protected long foffset;  // this one is start of seq in file
	protected long flength;  // this is length of seq
	   // need foffset - start of sequence.  bufoffset above is start of bseq buffer
		
	public BioseqFiled() { this(0); }
	public BioseqFiled(int maxlen) { super( maxlen); }

	public BioseqFiled(byte[] b) { this( b, 0, b.length); }
	public BioseqFiled(byte[] b, int offs, int len) { 
		super( b, offs, len); //? copy to bSeq or spool to disk
		}
		
	public BioseqFiled(char[] c) { this( c, 0, c.length); }
	public BioseqFiled(char[] c, int offs, int len) {  
		super( c, offs, len); //? copy to bSeq or spool to disk
		}

	public BioseqFiled( String s) {  
		super( s); //? copy to bSeq or spool to disk
		}

	public BioseqFiled( File f, boolean readwrite) {
		super(0);
		this.readwrite= readwrite;
		copyFrom( f);
		}

	public void copyFrom( Object ob) { 
    Debug.println( this.getClass().getName()+".copyFrom(ob)");
		if (ob instanceof File) copyFrom((File)ob);
		// add URL, others ?
		else super.copyFrom(ob);
		}
		
	public void copyFrom( File f) { 
		finibf();
		file= f;
		// copy foffset, flength
		istempfile= false; // dont delete
		initbf();
		}
  
	protected void finibf() {
		try {
			if (raf!=null) { raf.close(); raf= null; }
			if (istempfile && file!=null) { file.delete(); file= null; }
			}
		catch (Exception ex) { ex.printStackTrace(); }
	}

	protected void initbf() {
		// java1.2 File has temp file methods - not j1.1 -- use file= Native.tempFile() ?
		//if (istempfile) file= Native.tempFile();
		//else file= new File(filename);
		
    Debug.println( this.getClass().getName()+".initbf  "+file);
		bufoffset= 0; bufend= 0;  
		if (bSeq==null) bSeq= new byte[0]; // make an empty buf?
		try {
			raf= new RandomAccessFile( file, (readwrite) ? "rw" : "r");
			setFileOffset( 0, raf.length()); //? bad?
			//flength= raf.length();
			//bufend= bufoffset+flength; //? keep == bufoffset+flength
			}
		catch (Exception ex) { ex.printStackTrace(); }
	}
		
  public void clear() { 
    finibf();
    bufoffset= 0; bufend= 0;  
    super.clear();
    }

	public void finalize() throws Throwable { 
		finibf();
		super.finalize();
		}

	public void setFileOffset(long offset, long length) {
		//? check/keep raf.length()
		if (offset != foffset || length != flength) {
			bufoffset= 0; bufend= 0;  
			foffset= offset; 
			flength= length; 
			}
		//bufend= bufoffset;
		//? bufend= bufoffset+flength; // bufend <= flength - smaller when bSeq buf is partial flength
		}

	protected boolean endFile;
	public boolean endOfFile() { return endFile; } // only from scanForEnd ?
	public long fileLength() { 	
		if (raf == null) return 0;
		else try { return raf.length(); } catch (Exception e) { return 0; } 
		}  
			
		// call after new
	public int scanForEnd( int seqnum, Object lastBioseqFiled) 
	{
		if (raf == null) { endFile= true; return -1; }
		int nfound= 0;
		long startoff= 0;
		BioseqFiled lastf= null;
		//seqnum--; // change to 0 origin
		if (lastBioseqFiled instanceof BioseqFiled) {
		  lastf= (BioseqFiled) lastBioseqFiled;
		  startoff= lastf.foffset + lastf.flength; //? +1 to skip last \n \r
		  nfound= seqnum-1; //?
			}
		else {
			// else need to use seqnum to find start ??
			
			}
 		
		if (startoff >= fileLength()) {
			setFileOffset( startoff, 0); //?
			endFile= true;
			return nfound;
			}	
			
		long foff= startoff;
		long fend= startoff;
		long laststart= startoff;
		int nred= 1;
		byte[] buf= new byte[kMaxbuf]; //? use bSeq
		while (nfound<seqnum && nred>0) {
			nred= readBytes( foff, buf, 0, kMaxbuf) ;
			int k= 0; boolean more= true;
			for ( ; k<nred && more; k++) {
				if (buf[k] == '\n' || buf[k] == '\r') { 
					if (k > 1 || foff != startoff || (k == 1 && buf[0] >= ' ')) { 
						nfound++; 
						if (nfound<seqnum) laststart= 1+ k+foff; 
						else more= false;
						break; 
						}
					else if (foff == startoff && (k == 0 || (k == 1 && buf[0] < ' ') )) {
						laststart += 1;
						}
					}
				}
			foff += k; //if (nred>0) foff += nred;
			}
		if (nred <= 0) endFile= true; // got end of file !?
		setFileOffset( laststart, foff - laststart); //?
		
    Debug.println("BioseqFiled.scanForEnd, seqnum="+nfound+", offset="+foffset+", len="+flength);
		return nfound;
	}
			
		//
		// add file methods
		// -- base-indexing callers (e.g. writer), including reverse-complement(?)
		//    use bSeq as read buffer
		// -- spoolToDisk (during reading, so read buffer doesn't get huge)
		
	public int length() { 
		return (flength>0) ? (int) flength : bSeq.length;
		}
		
	public void setbases( byte[] theBases) { bSeq= theBases;   } //useByteArray= true;

	public void setbase(int i, byte b) { 
		if (bSeq.length>i) bSeq[i]= b;  // ! fixme for file
		}
	
	
	public byte basebyte(int i) {  
		if (raf!=null) {
			if (i >= bufoffset && i < bufend) return bSeq[i-(int)bufoffset];
			else { // load in bytes ?
				//long foff= i; //!
				//if ( i < bufoffset ) ; //?
				//else if ( i + kMaxbuf > flength ) ; //?
				int nred= readBases( i,  kMaxbuf) ;
				
				if (i >= bufoffset && i < bufend) return bSeq[i-(int)bufoffset];
				else return (byte)0; //? failed read?
				}
			}
		else if (i<bSeq.length) return bSeq[i];
		else return (byte)0; //?
		}
		

	public boolean isBytes() { 
		if (raf!=null) return false; //?
		else if (bSeq!=null && bSeq.length>0) return true;  	
		else return false;
		}

		
	public byte[] toBytes(int basepart) { 
		//if (useByteArray) 
		if (isBytes()) return bSeq;
		else return toBytes( 0, -1, basepart); //?
		}

	protected int readBytes( long foffset, byte[] buf, int bufoffset, int length) 
	{ 
    //Debug.println( this.getClass().getName()+".readBytes  "+foffset+","+length);
		try {
			raf.seek( foffset);
			return raf.read( buf, bufoffset, length);
 			}
		catch (Exception ex) { ex.printStackTrace(); return -1; } //? throw
	}	

	protected int readBases(int baseoffset, int length) 
	{ 
		try {
		  if (bSeq.length < length) bSeq= new byte[length]; //?
				// getting messy - foff = file offset, but need multiline foffset
				// bufoffset, bufend == seq index relative to foffset so bSeq[i-bufoffset] gets base(i)
				// real file offset/seek is seqstartFileOffset + bufoffset
			// foff is full file offset for seek() == foffset + bufoffset
			
	    Debug.print( this.getClass().getName()+".readBases at="+foffset+baseoffset+", want="+length);
			this.bufoffset= baseoffset;  
			this.bufend= this.bufoffset;
			raf.seek( baseoffset + foffset);
			int nred= raf.read( bSeq, 0, length);
			this.bufend= bufoffset+nred; 
	    Debug.println(" nread="+nred);

      SeqInfo sk= SeqInfo.getSeqInfo(length, true, false);
      sk.add( bSeq, 0, length);
      mySk= sk;

			return nred;
 			}
		catch (Exception ex) { ex.printStackTrace(); return -1; } //? throw
	}	
		
	public byte[] toBytes( int offset, int count, int basepart) 
	{ 
		if (offset<0) offset= 0;
		if (bSeq!=null) { //useByteArray && 
			if (offset==0 && count == bSeq.length) return bSeq;
			
			else if (raf!=null)  {
				int len= (int) flength;
				if (count>0 && count<len) len= count;
				
				int nred= readBases( offset, len);
				//bSeq= new byte[len];  //? save b in bSeq ??
				//int nred= readBytes( offset, bSeq, 0, len);
				
				if (nred < 0) {
				  return null; 
				  }
				if (nred==len) return bSeq;
				// else do following...
				}
				 
			int len= bSeq.length - offset; // can be 0, never <0 ?
				//^ need bSeqLength() == bufend-bufoffset or bSeq.length ?
				
			if (count>0 && count<len) len= count;
			byte[] b= new byte[len];
			System.arraycopy( bSeq, offset, b, 0, len);
			return b;
			}
		else 
			return null;
	}
	
	public char[] toChars( int offset, int count, int basepart) { 
		byte[] buf;
		if (isBytes()) buf= bSeq;
		else buf= toBytes( offset, count, basepart);
		if (offset<0) offset= 0;
		if (buf!=null) { //useByteArray && 
			int len= buf.length - offset;
			if (count>0 && count<len) len= count;
			char[] c= new char[len];
			for (int i= 0; i<len; i++) c[i]= (char) buf[i+offset];
			return c;
			}
		else return null;
		}


	public Object clone() {
		//try {
			BioseqFiled bs= (BioseqFiled) super.clone();
			//byte[] ba= new byte[bSeq.length];
			//System.arraycopy( bSeq, 0, ba, 0, bSeq.length);
			//bs.bSeq= ba;
			
			// ?? clone File ??
			
	    return bs;
			//}
		//catch(CloneNotSupportedException ex) { throw new Error(ex.toString()); }
		}

	
	SeqInfo mySk= null;
	
	public int getSeqtype() 
	{
		if (mySk==null) {
		int len= length();
		byte[] buf;
		if (isBytes()) buf= bSeq;
		else { len= Math.min(len, 8192); buf= toBytes( 0, len, 0); }

		SeqInfo sk= SeqInfo.getSeqInfo(len, true, false);
		sk.add( buf, 0, len);
		mySk= sk;
		}
		return mySk.getKind();
	}

	public SeqInfo getSeqStats( int offset, int seqlen) {
		if (mySk==null) {
		byte[] buf;
		if (isBytes()) buf= bSeq;
		else {  buf= toBytes( offset, seqlen, 0); offset= 0; }
		SeqInfo sk= SeqInfo.getSeqInfo( seqlen, false, true);
		sk.add( buf, offset, seqlen);
		mySk= sk;
		}
		return mySk;
		}


	// which of these super methods can we handle w/ large, filebased seq?
	/*******		
	public final void compress(byte gapc) {
		byte[] cb= compress(bSeq, 0, bSeq.length, gapc);
		setbases(cb);
		}
	public final void compress(int offset, int seqlen, byte gapc) {
		byte[] cb= compress(bSeq, offset, seqlen, gapc);
		setbases(cb);
		}
	public static byte[] compress(byte[] src, int offset, int seqlen, byte gapc)
	{
		int i, j;
		byte[] dst= new byte[seqlen];
		for (i= 0, j= 0; i<seqlen; i++) {
			if (src[i+offset] != gapc) dst[j++]= src[i+offset];
			}
		if (j != seqlen) return expand(dst, j);
		else return dst;
	}


	public final void replace(int len, char oldbase, char newbase) { 
		replace( this, len, oldbase, newbase); 
		}
 	public static void replace(Bioseq seq, int len, char oldbase, char newbase) {
  	for (int i=0; i<len; i++) 
  		if (seq.bSeq[i] == (byte)oldbase) seq.bSeq[i] = (byte)newbase;  
		}


	public final void replace(int len, String oldbases, String newbases) { 
		replace( this, len, oldbases, newbases); 
		}
 	public static void replace(Bioseq seq, int len, String oldbases, String newbases) {
  	if (oldbases==null||newbases==null) return;
  	int nbases= oldbases.length();
  	for (int j=0; j<nbases; j++) {
  		byte oldb= (byte)oldbases.charAt(j);
  		byte newb= (byte)newbases.charAt(j);
  		for (int i=0; i<len; i++) if (seq.bSeq[i] == oldb) seq.bSeq[i] = newb;  
  		}
		}

	public final void expand(int newlen) { 
		byte[] exb= expand( bSeq, newlen); 
		setbases(exb);
		}
	public static byte[] expand(byte[] src, int newlen) {
		int i, minlen= src.length; 
		if (minlen>newlen) minlen= newlen;
		byte[] b= new byte[newlen];
		System.arraycopy( src, 0, b, 0, minlen); // uses same bytes/Biobases
		for (i=minlen; i<newlen; i++) b[i]= (byte)'?'; //new Biobase();
		return b;
		}

	public final byte[] dup() { return dup( bSeq); }
	public static byte[] dup(byte[] src) { 
		int len= src.length;
		byte[] b= new byte[len];
		System.arraycopy( src, 0, b, 0, len); // bad for Biobase - need below
		//for (int i=0; i<len; i++) b[i]= new Biobase(src[i]);
		return b;
		}
		
	public final int cmp(Bioseq b, int len) { return cmp( bSeq, b.bases(), len); }
	public static int cmp(byte[] a, byte[] b, int len) { 
		for (int i=0; i<len; i++) 
			if (a[i]  < b[i] ) return -1;
			else if (a[i]  > b[i] ) return 1;
		return 0;
		}

	public final int indexOf(byte b, int len) { return indexOf( bSeq, b, len); }
	public static int indexOf(byte[] a, byte b, int len) { 
		for (int i=0; i<len; i++) if (a[i] == b) return i;
		return -1;
		}


	public final boolean equals(Bioseq b) { return equals( bSeq, b.bases()); }
	public static boolean equals(byte[] a, byte[] b) { 
		if (a.length != b.length) return false;
		for (int i=0; i<a.length; i++) 
			if (a[i] < b[i]) return false;
			else if (a[i] > b[i]) return false;
		return false;
		}
		
		
	public final void copy(byte[] dst, int dstOffset, int len) { 
		// this is source
		copy(bSeq, 0, dst, dstOffset, len); 
		}
		
		// note: params are Java/Pascal-esque forward  copy(source -> dest)
		// rather than NCBI Toolbox/C-ish bassakwards  copy(dest <- source)
	public static void copy(byte[] src, int srcOffset,
													byte[] dst, int dstOffset, int len) 
	{ 
		System.arraycopy( src, srcOffset, dst, dstOffset, len);
		 
		//if (dst.equals(src) && dstOffset >= srcOffset && dstOffset <= srcOffset+len) {
		//	int d = dstOffset + len;
		//	int s = srcOffset + len;
		//	for ( ; len>0; len--) dst[--d]= src[--s];
		//	}
		//else
		//	for ( ; len>0; len--) dst[dstOffset++]= src[srcOffset++];
		 
	}
		
	*****************/
		
}


