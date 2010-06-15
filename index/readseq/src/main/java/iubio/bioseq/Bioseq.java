// iubio/bioseq/Bioseq.java
// dgg oct'96 (from dclap.biosequence.c++)


package iubio.bioseq;


// may01 -- public class Biobase removed to Biobase.java -- depreciate it; removed from Biosequence

// revise this to store/use byte[] instead of Biobase for speed when desired
// -- drop Biobase may01, add FileBioseq / BioseqPersist ?


public class Bioseq 
	implements Cloneable
{
		// enum seqType == same as in SeqInfo
	public final static short kOtherSeq= 0, kDNA= 1, kRNA= 2, kNucleic= 3, kAmino= 4;
		// enum basePart -- dropped usage with removal of Biobase
	public final static int baseOnly = 0, maskOnly = 1, nucAndMask = 2, maskOnlyAsText= 3;
	public static boolean gUseByteArray = true;
	public int gBaseType= baseOnly;
	
	//protected boolean useByteArray= gUseByteArray;
	protected byte[] bSeq;

	//// need to add:
	// protected int offset;   // offset from bSeq start where this Bioseq starts
	// protected int length;	 // length of this Bioseq, independent of bSeq
	// protected int origin;	 // origin of bSeq (or offset?), where bSeq is subrange of full seq
	/// public indexed methods should use  bSeq[index+offset-origin] ??
 
	public Bioseq() { this(0); }

	public Bioseq( int maxlen) { bSeq= new byte[maxlen]; }

	public Bioseq( byte[] b) { copyFrom(b); }
		
	public Bioseq( byte[] b, int offs, int len) { copyFrom(b, offs, len); }
		
	public Bioseq( char[] c) { copyFrom(c); }
		
	public Bioseq( char[] c, int offs, int len) { copyFrom(c, offs, len); }

	public Bioseq( String s) { copyFrom(s); }

	//add: public Bioseq( OpenString s) { copyFrom(s); }
	
	public Bioseq( Object ob) { copyFrom(ob); }

	public void copyFrom( Object ob) { 
		if (ob instanceof byte[]) copyFrom((byte[])ob);
		else if (ob instanceof char[]) copyFrom((char[])ob);
		else if (ob instanceof String) copyFrom((String)ob);
		}
		
	public final void copyFrom(byte[] b) { copyFrom( b, 0, b.length); }
	public void copyFrom(byte[] b, int offs, int len) { 
		bSeq= new byte[len];
		System.arraycopy( b, offs, bSeq, 0, len);
		}
		
	public final void copyFrom(char[] c) { copyFrom( c, 0, c.length); }
	public void copyFrom(char[] c, int offs, int len) { 
		bSeq= new byte[len];
		for (int i=0; i<len; i++) bSeq[i]= (byte) c[i+offs];			
		}
		
	public void copyFrom( String s) { 
		bSeq= s.getBytes();
		//int len= s.length();
		//bSeq= new byte[len];
		//for (int i=0; i<len; i++) bSeq[i]= (byte) s.charAt(i);
		}
		
	public int length() { 
		//if (useByteArray) 
		return bSeq.length;
		}

	// add ?? -- treat Bioseq like String/OpenString where basic byte array can be used among
	// many Bioseq objects, w/ differing offset, length independent of bSeq.length
//	public final int getOffset() { return offset; }
//	public final void setOffset( int off) { offset= off; }
//	public final void setLength( int len) { count= len; }
		

	public void setbases( byte[] theBases) { bSeq= theBases; } //useByteArray= true;

	public void setbase(int i, byte b) { 
		//if (useByteArray) 
		bSeq[i]= b;  
		}
	public void setbase(int i, char c) { 
		setbase( i, (byte)c);
		}
	
	public byte basebyte(int i) {  
		//if (useByteArray) 
		return bSeq[i];
		}
		
	public char basechar(int i) { 
		return (char) basebyte(i);   
		}
	
	public char base(int i, int basepart) {  
		return (char) basebyte(i);   
		/*
		else {
			switch (basepart) {
				default:
				case baseOnly  : return fSeq[i].c(); 
				case maskOnly  : return (char)fSeq[i].mask();  
				case nucAndMask: return fSeq[i].basemask(); 
				case maskOnlyAsText: return (char)fSeq[i].maskAsText();
				}
			}
		*/
		}

	public boolean isBytes() { return true; } // useByteArray;


	public byte[] bases() { return toBytes(0); } //? drop this method
	//public Biobase[] bases() ; // was this
	//public void setbases( Biobase[] theBases) { fSeq= theBases; useByteArray= false; } 
	//public final Biobase base(int i);

		//? change to getBytes() ?
	public final byte[] toBytes() {  return toBytes(gBaseType); }
	public byte[] toBytes(int basepart) { 
		//if (useByteArray) 
		return bSeq;
		//else return toBytes( 0, -1, basepart); 
		}
	
	public byte[] toBytes( int offset, int count, int basepart) 
	{ 
		if (offset<0) offset= 0;
		if (bSeq!=null) { //useByteArray && 
			if (offset==0 && count == bSeq.length) return bSeq;
			else {
				int len= bSeq.length - offset;
				if (count>0 && count<len) len= count;
				byte[] b= new byte[len];
				System.arraycopy( bSeq, offset, b, 0, len);
				return b;
				}
			}
		/*
		else if (fSeq!=null) {
			int len= fSeq.length - offset;
			if (count>0 && count<len) len= count;
			byte[] b= new byte[len];
			int i;
			switch (basepart) {
				default:
				case baseOnly  : for (i=0; i<len; i++) b[i]= fSeq[i+offset].base(); break;
				case maskOnly  : for (i=0; i<len; i++) b[i]= fSeq[i+offset].mask(); break;
				case maskOnlyAsText: 
										for (i=0; i<len; i++) b[i]= fSeq[i+offset].maskAsText(); break;
				}
			return b;
			}
			*/
		else 
			return null;
	}


		// change to getChars() ??
	public final char[] toChars() { return toChars(gBaseType); }
	public final char[] toChars(int basepart) { return toChars( 0, -1, basepart); }
	
	public char[] toChars( int offset, int count, int basepart) { 
		if (offset<0) offset= 0;
		if (bSeq!=null) { //useByteArray && 
			int len= bSeq.length - offset;
			if (count>0 && count<len) len= count;
			char[] c= new char[len];
			for (int i= 0; i<len; i++) c[i]= (char) bSeq[i+offset];
			return c;
			}
		/*
		else if (fSeq!=null) {
			int len= fSeq.length - offset;
			if (count>0 && count<len) len= count;
			char[] c= new char[len];
			int i;
			switch (basepart) {
				default:
				case baseOnly  : for (i=0; i<len; i++) c[i]= fSeq[i+offset].c(); break;
				case maskOnly  : for (i=0; i<len; i++) c[i]= (char)fSeq[i+offset].mask(); break;
				case nucAndMask: for (i=0; i<len; i++) c[i]= fSeq[i+offset].basemask(); break;
				case maskOnlyAsText: 
							for (i=0; i<len; i++) c[i]= (char)fSeq[i+offset].maskAsText(); break;
				}
			return c;
			}
		*/
		else return null;
		}

	public String toString() { return new String(toChars()); }

	public Object clone() {
		try {
			Bioseq bs= (Bioseq) super.clone();
			byte[] ba= new byte[bSeq.length];
			System.arraycopy( bSeq, 0, ba, 0, bSeq.length);
			bs.bSeq= ba;
			//else bs.fSeq= this.dup(); 
	    return bs;
			}
		catch(CloneNotSupportedException ex) { throw new Error(ex.toString()); }
		}


	//public static int kMaxSeqtest = 500;
	
	public int getSeqtype() 
	{
		int len= length();
		SeqInfo sk= SeqInfo.getSeqInfo(len, true, false);
		//SeqKind sk= new SeqKind( len, true, false);
		//if (useByteArray) 
		sk.add( bSeq, 0, len);
		return sk.getKind();
	}

	public static int getSeqtype( String s, int offset, int seqlen) 
	{
		//SeqKind sk= new SeqKind(seqlen, true, false);
		SeqInfo sk= SeqInfo.getSeqInfo(seqlen, true, false);
		sk.add( s, offset, seqlen);
		return sk.getKind();
	}
		
	public static int getSeqtype( byte[] ba, int offset, int seqlen)
	{  
		//SeqKind sk= new SeqKind(seqlen, true, false);
		SeqInfo sk= SeqInfo.getSeqInfo(seqlen, true, false);
		sk.add( ba, offset, seqlen);
		return sk.getKind();
	} 

	public final SeqInfo getSeqStats() { return getSeqStats( 0, length()); }

	public SeqInfo getSeqStats( int offset, int seqlen) {
		//SeqKind sk= new SeqKind( seqlen, false, true);
		SeqInfo sk= SeqInfo.getSeqInfo( seqlen, false, true);
		//if (useByteArray) 
		sk.add( bSeq, offset, seqlen);
		return sk;
		}

				// for efficiency w/ huge sequences (100+MB chromosomes)
				// move this to writer-writebyte part, and do rev-comp on a per-byte basis -- 
				//  char bc= bioseq.base(offset+i,fBasePart); 

	public void reverseComplement() { reverseComplement( 0, length()); }
		
 	public void reverseComplement( int offset, int count) 
	{
		byte[] ba= new byte[count];    
		byte[] bases= this.toBytes(offset, count, baseOnly); //? or toBytes(offset, count) ??
		int seqkind= this.getSeqtype();
		boolean isamino= (seqkind == Bioseq.kAmino);
		boolean isrna= (seqkind == Bioseq.kRNA);
		int baend= count  - 1;
		if (isamino) { //? need amino complement !
			for (int i= 0; i<count; i++) ba[baend-i]= bases[i];
			}
		else {
			for (int i= 0; i<count; i++)  
				ba[baend-i]= BaseKind.nucleicComplement( bases[i], isrna);
			}
		this.setbases( ba);
	}

			
	public final boolean equals(Bioseq b) { return equals( bSeq, b.bases()); }
	public static boolean equals(byte[] a, byte[] b) { 
		int n = a.length;
		if (n == b.length) {
			while (n-- != 0) if (a[n] != b[n]) return false;
			return true;
			}
		return false;
		}
		
	public final boolean equalsIgnoreCase(Bioseq b) { return equalsIgnoreCase( bSeq, b.bases()); }
	public static boolean equalsIgnoreCase(byte[] a, byte[] b) { 
		int n = a.length;
		if (n == b.length) {
			while (n-- != 0) 
				if (Character.toUpperCase((char)a[n]) != Character.toUpperCase((char)b[n])) 
					return false;
			return true;
			}
		return false;
		}


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
		
   // change to ? compareTo(Bioseq b, boolean ignoreCase) 
	public final int cmp(Bioseq b, int len) { return cmp( bSeq, b.bases(), len); }
	public static int cmp(byte[] a, byte[] b, int len) { 
		for (int i=0; i<len; i++) 
			if (a[i]  < b[i] ) return -1;
			else if (a[i]  > b[i] ) return 1;
		return 0;
		}

			// add String/OpenString methods  for indexOf(), lastIndexOf()
	public final int indexOf(byte b, int len) { return indexOf( bSeq, b, len); }
	public static int indexOf(byte[] a, byte b, int len) { 
		for (int i=0; i<len; i++) if (a[i] == b) return i;
		return -1;
		}

	// add
	// public final Bioseq substring(int beginIndex) {
  // public Bioseq substring(int beginIndex, int endIndex) 
  // public Bioseq concat(Bioseq str)  

		
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
		/*
		if (dst.equals(src) && dstOffset >= srcOffset && dstOffset <= srcOffset+len) {
			int d = dstOffset + len;
			int s = srcOffset + len;
			for ( ; len>0; len--) dst[--d]= src[--s];
			}
		else
			for ( ; len>0; len--) dst[dstOffset++]= src[srcOffset++];
		*/
	}
		
}

