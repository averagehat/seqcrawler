// iubio/bioseq/Biobase.java
// move to package dbio ?
// dgg oct'96
// cut from biosequence.java, may01 -- depreciate use of this class, too unwieldy (too may objects)


package iubio.bioseq;


// see if we can separate this into two versions, with mask for more complex uses
// class Biobase
// class BiobaseMasked

public class Biobase { 
	//protected ?!
	public byte	c, mask;
	
	public Biobase() { c= (byte)'?'; }
	public Biobase(byte base) { c= base; }
	public Biobase(char base) { c= (byte) base; }
	public Biobase(char base, boolean withmask) { 
		if (withmask) {
			c= (byte) (base & 0x00ff);
			mask= (byte) (base >> 8);
			}
		else c= (byte) base; 
		}
	public Biobase(byte base, byte mask) { c= base; this.mask= mask; }
	public Biobase(char base, byte mask) { c= (byte) base; this.mask= mask; }
	public Biobase(Biobase bb) { c= bb.c; mask= bb.mask; }

	
	// ACCESSORS =================================
	
	public byte base() { return c; }
	public byte mask() { return mask; }
	public char c()    { return (char) c; }
	public char cmask() { return (char) mask; }
	public char basemask() { return (char) (c | mask << 8); }
	public byte maskAsText() { return (byte) ((mask & 0x7F) + '?'); }

	// BASE KINDS =================================

	public final  boolean isPrimenuc() { return BaseKind.isPrimenuc((int)c); }
	public final  boolean isIUBsym() { return BaseKind.isIUBsym((int)c); }
	public final  boolean isDnanuc() { return BaseKind.isDnanuc((int)c); }
	public final  boolean isRnanuc() { return BaseKind.isRnanuc((int)c); }
	public final  boolean isAmino() { return BaseKind.isAmino((int)c); }
	public final  boolean isProtonly() { return BaseKind.isProtonly((int)c); }
	public final  boolean isSeqsym() { return BaseKind.isSeqsym((int)c); }
	public final  boolean isAlphaseq() { return BaseKind.isAlphaseq((int)c); }
	public final  boolean isAlnumseq() { return BaseKind.isAlnumseq((int)c); }
	public final  boolean isIndel() { return BaseKind.isIndel((int)c); }
	public final  boolean isPrint() { return BaseKind.isPrint((int)c); }

	public final  int isSeqChar() { return BaseKind.isSeqChar((int)c); }
	public final  int isGCGSeqChar() { return BaseKind.isGCGSeqChar((int)c); }
	public final  int isSeqNumChar() { return BaseKind.isSeqNumChar((int)c); }
	public final  int isAnyChar() { return BaseKind.isAnyChar((int)c); }


	// MASKS =================================

 	public final static int kMaskEmpty = -1;
			
  public final int maskBit( int masklevel) {
  	return maskBit( mask, masklevel); }

  public static int maskBit(byte maskbyte, int masklevel) 
  { 
    int b= maskbyte;
    switch (masklevel) {
      case 0: b &= 0x7f; break; // return full mask - top (0x80) bit
      case 1: b &= 1;  break;  
      case 2: b &= 2;  break;
      case 3: b &= 4;  break;
      case 4: b &= 8;  break;
          // 5-7 reserved for now
      case 5: b &= 16;  break;
      case 6: b &= 32;  break;
      case 7: b &= 64;  break;
      default: b = kMaskEmpty; break;
      }
    return b;
  }

  public final boolean isMasked( int masklevel) {
    return (maskBit(masklevel) > 0);
  	}
  	
  public static boolean isMasked(byte maskbyte, int masklevel) 
  { 
  	int b= maskBit( maskbyte, masklevel);
    return (b > 0);
  }

	public final byte setMask( int masklevel, int maskval) {
		mask= setMask( mask, masklevel, maskval); return mask; }

	public static byte setMask( byte b, int masklevel, int maskval) 
	{
		switch (masklevel) {
			case 1: if (maskval!=0) b |= 1; else b &= ~1; break;
			case 2: if (maskval!=0) b |= 2; else b &= ~2; break;
			case 3: if (maskval!=0) b |= 4; else b &= ~4; break;
			case 4: if (maskval!=0) b |= 8; else b &= ~8; break;
				// reserve bits 5, 6, 7 for now 
			case 5: if (maskval!=0) b |= 16; else b &= ~16; break;
			case 6: if (maskval!=0) b |= 32; else b &= ~32; break;
			case 7: if (maskval!=0) b |= 64; else b &= ~64; break;
				// -- 7 & 8 may go unused to store data in printchar form
			default: break;
			}
		return b;
	}

	public final byte flipMask( int masklevel) {
		mask= flipMask( mask, masklevel); return mask; }

	public static byte flipMask( byte b, int masklevel)
	{
		switch (masklevel) {
			case 1: b ^= 1;  break;  
			case 2: b ^= 2;  break;
			case 3: b ^= 4;  break;
			case 4: b ^= 8;  break;
					// 5-7 reserved?
			case 5: b ^= 16;  break;
			case 6: b ^= 32;  break;
			case 7: b ^= 64;  break;
			default:  break;
			}
		return b;
	}

};

