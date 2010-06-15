// biobasekind.java
// by d.g.gilbert, 1990 -- 1997


package iubio.bioseq;



public class BaseKind { 

	public static char indelHard = '-', indelSoft = '~', indelEdge = '.';

	// BASE KINDS =================================

	public static boolean isPrimenuc(int c) { return (c>127||c<0)?false:(cflags[c]&primenucflag)!=0; }

	public static boolean isIUBsym(int c) { return (c>127||c<0)?false:(cflags[c]&iubsymflag)!=0; }

	public static boolean isDnanuc(int c) { 
		return (c>127||c<0)?false:(cflags[c]&primenucflag)!=0 && c!='u' && c!='U';}

	public static boolean isRnanuc(int c) { 
		return (c>127||c<0)?false:(cflags[c]&primenucflag)!=0 && c!='t' && c!='T';}

	public static boolean isAmino(int c) { return (c>127||c<0)?false:(cflags[c]&aminoflag)!=0;}

	public static boolean isProtonly(int c) { return (c>127||c<0)?false:(cflags[c]&protonlyflag)!=0;}

	public static boolean isSeqsym(int c) { return (c>127||c<0)?false:(cflags[c]&seqsymflag)!=0;}

	public static boolean isAlphaseq(int c) { return (c>127||c<0)?false:(cflags[c]&alphaseqflag)!=0;}

	public static boolean isAlnumseq(int c) { return (c>127||c<0)?false:(cflags[c]&alnumseqflag)!=0;}

	public static boolean isPrint(int c) { return (c>127||c<0)?false:(cflags[c]&allprintflag)!=0;}

	public static boolean isIndel(int c) { 
		return (c>127||c<0) ? false : (c==indelHard||c==indelSoft||c==indelEdge); }

	public static boolean isBasekind(int c,int flag) { return (c>127||c<0)?false:(cflags[c]&flag)!=0; }


	public static int isSeqChar(int c) { if (isAlphaseq(c)) return c; else return 0; }

	public static int isGCGSeqChar(int c) {
	  if (isAlphaseq(c))  {
	  		/* do the indel translate */
	    if (c == '.') return '-';  else return c;
	    }
	  else return 0;
		}

	public static int isSeqNumChar(int c) { if (isAlnumseq(c)) return c; else return 0; }

	public static int isAnyChar(int c) { if (isPrint(c)) return c; else return 0; }



	// BASE CONVERSION =================================

	public final static int nucleicBits(char nuc) { return nucleicBits((byte)nuc); }
	public static int nucleicBits(byte nuc)
	{
		switch (nuc) {
			case 'a': case 'A': return kMaskA;  
			case 'c': case 'C': return kMaskC;   
			case 'g': case 'G': return kMaskG;   
			case 't': case 'T': return kMaskT;   
			case 'u': case 'U': return kMaskU;  // ==  ( kMaskT | kMaskExtra); 
			case 'y': case 'Y': return (kMaskC | kMaskT);  
			case 'k': case 'K': return (kMaskG | kMaskT);  
			case 's': case 'S': return (kMaskG | kMaskC);  
			case 'r': case 'R': return (kMaskG | kMaskA);  
			case 'm': case 'M': return (kMaskA | kMaskC);  
			case 'w': case 'W': return (kMaskA | kMaskT);  
			case 'h': case 'H': return (kMaskA | kMaskC | kMaskT);  
			case 'b': case 'B': return (kMaskG | kMaskC | kMaskT);  
			case 'v': case 'V': return (kMaskG | kMaskC | kMaskA);  
			case 'd': case 'D': return (kMaskG | kMaskT | kMaskA);  
			case ' ': case '_': return 0;  //? spacers
			case  0 : return 0;  
			case 'n': case 'N': return kMaskNucs; 
			default : 
				{
				if (nuc == indelHard || nuc == indelSoft)  return kMaskIndel;
				else if (nuc == indelEdge) return 0; //return kMaskNucs; ???
				else return 0; //kMaskNucs;  //?? match all or match none ??
				}
			}
	}

	public static byte nucleicComplement(byte b, boolean isrna)
	{
		if (b<0 || b>127) 
			return b;
		else {
			byte c= nucomp[b];
			if (c==0) return b;
			else if (isrna) { 
				if (b=='A') return (byte)'U'; 
				else if (b=='a') return (byte)'u'; 
				else return c; 
				}
			else return c;
			}
	}

  public static boolean isNucleicMatch(int xNucBits, int yNucBits) { 
    // note: Indel/spaces match nothing, including other indel/space ...
  	return 0 != ( (kMaskNucs & xNucBits) & (kMaskNucs & yNucBits));
  	}

  public static byte nucleicConsensus(int xNucBits, int yNucBits) 
  { 
    boolean indel= (xNucBits == kMaskIndel || yNucBits == kMaskIndel);
    if (xNucBits==0 || yNucBits==0) 
      return (byte)indelSoft; //'n';  //?? either is null, yields space ?
    else switch ( (kMaskNucs & xNucBits) | (kMaskNucs & yNucBits) ) {
			case kMaskA: return (byte)((indel) ? 'a' : 'A');
      case kMaskC: return (byte)((indel) ? 'c' : 'C');
      case kMaskG: return (byte)((indel) ? 'g' : 'G');
      case kMaskT: {  
    		boolean isrna= (kMaskU == xNucBits || kMaskU == yNucBits);
        if (isrna) return (byte)((indel) ? 'u' : 'U'); 
        else return (byte)((indel) ? 't' : 'T'); 
        }
			case kMaskY: return (byte)((indel) ? 'y' : 'Y'); 
			case kMaskK: return (byte)((indel) ? 'k' : 'K');
			case kMaskS: return (byte)((indel) ? 's' : 'S');
			case kMaskR: return (byte)((indel) ? 'r' : 'R');
			case kMaskM: return (byte)((indel) ? 'm' : 'M');
			case kMaskW: return (byte)((indel) ? 'w' : 'W');
			case kMaskH: return (byte)((indel) ? 'h' : 'H');
			case kMaskB: return (byte)((indel) ? 'b' : 'B');
			case kMaskV: return (byte)((indel) ? 'v' : 'V');
			case kMaskD: return (byte)((indel) ? 'd' : 'D');
			default:
			case kMaskN: return (byte)((indel) ? 'n' : 'N');
			}
  }

	/*
  public static byte nucleicConsensus1(int xNucBits, int yNucBits) 
  { 
    byte nuc; 
    boolean isrna, indel;
    if (xNucBits==0 || yNucBits==0) 
      return 'n';  //?? either a space yields space ?
    else {
      indel= (xNucBits == kMaskIndel || yNucBits == kMaskIndel);
      isrna= (kMaskU == xNucBits || kMaskU == yNucBits);
      switch ( (kMaskNucs & xNucBits) | (kMaskNucs & yNucBits)) {
        case kMaskA: if (indel) nuc= 'a'; else nuc= 'A'; break;
        case kMaskC: if (indel) nuc= 'c'; else nuc= 'C'; break;
        case kMaskG: if (indel) nuc= 'g'; else nuc= 'G'; break;
        case kMaskT: 
          if (isrna) { if (indel) nuc= 'u'; else nuc= 'U'; }
          else { if (indel) nuc= 't'; else nuc= 'T'; }
          break;
            //? do other ambig codes ?
        default:  nuc= '.'; break; //? use this as Consensus other ?
        }
      }
    return nuc;
  }
	*/

  public static String Amino123(char amino1) { 
		switch (amino1) {
			 case 'A': case 'a': return "Ala";
	     case 'M': case 'm': return "Met";
	     case 'R': case 'r': return "Arg";
	     case 'F': case 'f': return "Phe";
	     case 'N': case 'n': return "Asn";
	     case 'P': case 'p': return "Pro";
	     case 'D': case 'd': return "Asp";
	     case 'S': case 's': return "Ser";
	     case 'C': case 'c': return "Cys";
	     case 'T': case 't': return "Thr";
	     case 'Q': case 'q': return "Gln";
	     case 'W': case 'w': return "Trp";
	     case 'E': case 'e': return "Glu";
	     case 'Y': case 'y': return "Tyr";
	     case 'G': case 'g': return "Gly";
	     case 'V': case 'v': return "Val";
	     case 'H': case 'h': return "His";
	     case 'I': case 'i': return "Ile";
	     case 'B': case 'b': return "Asx";
	     case 'L': case 'l': return "Leu";
	     case 'Z': case 'z': return "Glx";
	     case 'K': case 'k': return "Lys";
	     case 'X': case 'x': return "Xaa";
	     case '*': return "End";
	     case ' ': return "   ";
	     default : return "???"; //??
	     }
    }
    
  public static char Amino321(String amino) 
  { 
    int i; char c;
    char aac[] = new char[3];
    i=0; c= amino.charAt(i); if (c>'Z') c += 'A' - 'a'; aac[i]= c;
    i++; c= amino.charAt(i); if (c<'a') c -= 'A' - 'a'; aac[i]= c;
    i++; c= amino.charAt(i); if (c<'a') c -= 'A' - 'a'; aac[i]= c;
    amino= new String( aac);
         if (amino.equals("Ala")) return 'A';
    else if (amino.equals("Met")) return 'M';
    else if (amino.equals("Arg")) return 'R';                 
    else if (amino.equals("Phe")) return 'F';
    else if (amino.equals("Asn")) return 'N';               
    else if (amino.equals("Pro")) return 'P';
    else if (amino.equals("Asp")) return 'D';    
    else if (amino.equals("Ser")) return 'S';
    else if (amino.equals("Cys")) return 'C';                 
    else if (amino.equals("Thr")) return 'T';
    else if (amino.equals("Gln")) return 'Q';               
    else if (amino.equals("Trp")) return 'W';
    else if (amino.equals("Glu")) return 'E';    
    else if (amino.equals("Tyr")) return 'Y';
    else if (amino.equals("Gly")) return 'G';                      
    else if (amino.equals("Val")) return 'V';
    else if (amino.equals("His")) return 'H';
    else if (amino.equals("Ile")) return 'I';               
    else if (amino.equals("Asx")) return 'B';
    else if (amino.equals("Leu")) return 'L';                      
    else if (amino.equals("Glx")) return 'Z';
    else if (amino.equals("Lys")) return 'K';                       
    else if (amino.equals("End")) return '*';                       
    else if (amino.equals("   ")) return ' '; //??                       
    else return 'X';
  }
  

  public static byte aminoConsensus(byte x, byte y) 
  { 
 		if (x == y) return x;
 		if (isIndel(y)) return (byte)Character.toLowerCase((char)y);
 		else if (isIndel(x)) return (byte)Character.toLowerCase((char)x);
		// !! need to find amino consensus table !
		return x;
  }

		/* NucleicBits (values in 0..31):  
				%00001 = A
				%11000 = kMaskU = U (rna) 
				%10000 = kMaskIndel = indel (-)
				%01111 = kMaskNucs = N or .
				%00000 = non-nucleic */
	public final static int 
		kBitA	= 0,  kMaskA = 1, 
		kBitC = 1,  kMaskC = 2,
		kBitG	= 2,	kMaskG = 4,
		kBitT	= 3,	kMaskT = 8,		
    kMaskY = kMaskC + kMaskT, //10
		kMaskK = kMaskG + kMaskT, //12
		kMaskS = kMaskG + kMaskC, //6
		kMaskR = kMaskG + kMaskA, //5
		kMaskM = kMaskA + kMaskC, //3
		kMaskW = kMaskA + kMaskT, //9
		kMaskH = kMaskA + kMaskC + kMaskT, //11
		kMaskB = kMaskG + kMaskC + kMaskT, //14
		kMaskV = kMaskG + kMaskC + kMaskA, //7
		kMaskD = kMaskG + kMaskT + kMaskA, //13
		kMaskN = kMaskA + kMaskC + kMaskG + kMaskT, //15 == kMaskNucs
		
		kBitExtra = 4, kMaskExtra = 16,
		kMaskIndel= 16, //kMaskExtra,
		kMaskU 		= 8+16, kMaskRna = 8+16, // kMaskT + kMaskExtra, kMaskRna = kMaskU,
		kMaskNucs = 1+2+4+8, //kMaskA + kMaskC + kMaskG + kMaskT,
		kBit5 = 5,  kMask5 = 32,  //unused NucleicBits in byte
		kBit6 = 6,	kMask6 = 64,
		kBit7 = 7,	kMask7 = 128
		;

	// enum seqflag 
	public final static int 
		primenucflag = 1, iubextraflag = 2, 
		//dnanucflag = 2, //rnanucflag = 4, 
		aminoflag  = 8, protonlyflag = 0x10, 
		seqsymflag = 0x20,
		digitflag  = 0x40, otherprintflag = 0x80,
		iubnucflag = primenucflag + iubextraflag,
		iubsymflag = iubnucflag + seqsymflag,
		alphaseqflag = iubsymflag + aminoflag,
		alnumseqflag = alphaseqflag + digitflag,
		allprintflag = alnumseqflag + otherprintflag
		;
	protected final static byte opf = (byte) otherprintflag;
	protected final static byte aseqf = (byte) alphaseqflag;

		// jun01 = change N/n to iubsymflag
	protected final static byte[] cflags = {
	  0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,
	  0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,
	  /*  ,  ! ,  " ,  # ,  $ ,  % ,  & ,  ' ,  ( ,  ) ,  * ,  + ,  , ,  - ,  . ,  / ,*/
	  0x0 ,0x20,0x20,0x20, opf, opf,0x20,0x20,0x20,0x20,0x28,0x20, opf,0x20,0x20,0x20,
	  /*0 ,  1 ,  2 ,  3 ,  4 ,  5 ,  6 ,  7 ,  8 ,  9 ,  : ,  ; ,  < ,  = ,  > ,  ? ,*/
	  0x40, 0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x20,0x20,0x20,0x20,0x20,0x20,
	  /*@ ,  A ,  B ,  C ,  D ,  E ,  F ,  G ,  H ,  I ,  J ,  K ,  L ,  M ,  N ,  O ,*/
	  0x20,0x9 ,0xA ,0x9 ,0xA ,0x18,0x18,0x9 ,0xA ,0x18, opf,0xA ,0x8 ,0xA ,aseqf , opf,
	  /*P ,  Q ,  R ,  S ,  T ,  U ,  V ,  W ,  X ,  Y ,  Z ,  [ ,  \ ,  ] ,  ^ ,  _ ,*/
	  0x18,0x18,0xA ,0xA ,0x9 ,0x1 ,0xA ,0xA ,0xA ,0xA ,0x18,0x20,0x20,0x20,0x20,0x20,
	  /*` ,  a ,  b ,  c ,  d ,  e ,  f ,  g ,  h ,  i ,  j ,  k ,  l ,  m ,  n ,  o ,*/
	  0x20,0x9 ,0xA ,0x9 ,0xA ,0x18,0x18,0x9 ,0xA ,0x18, opf,0xA ,0x8 ,0xA ,aseqf , opf,
	  /*p ,  q ,  r ,  s ,  t ,  u ,  v ,  w ,  x ,  y ,  z ,  { ,  | ,  } ,  ~ , del,*/
	  0x18,0x18,0xA ,0xA ,0x9 ,0x1 ,0xA ,0xA ,0xA ,0xA ,0x18,0x20,0x20,0x20,0x20,0x0 
		};

	protected final static byte[] nucomp = {
	  0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,
	  0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,
	  /*  , ! ,  " ,  # ,  $ ,  % ,  & ,  ' ,  ( ,  ) ,  * ,  + ,  , ,  - ,  . ,  / ,*/
	  0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,
	  /*0 , 1 ,  2 ,  3 ,  4 ,  5 ,  6 ,  7 ,  8 ,  9 ,  : ,  ; ,  < ,  = ,  > ,  ? ,*/
	  0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,
	  /*@ , A ,  B ,  C ,  D ,  E ,  F ,  G ,  H ,  I ,  J ,  K ,  L ,  M ,  N ,  O ,*/
	  0x0 ,(byte)'T' ,(byte)'V' ,(byte)'G' ,(byte)'H' ,0x0 ,0x0 ,(byte)'C' ,(byte)'D' ,0x0 ,0x0 ,(byte)'M' ,0x0 ,(byte)'K' ,0x0 , 0x0,
	  /*P , Q ,  R ,  S ,  T ,  U ,  V ,  W ,  X ,  Y ,  Z ,  [ ,  \ ,  ] ,  ^ ,  _ ,*/
	  0x0 ,0x0 ,(byte)'Y' ,0x0 ,(byte)'A' ,(byte)'A' ,(byte)'B' ,0x0 ,0x0 ,(byte)'R' ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,
	  /*` , a ,  b ,  c ,  d ,  e ,  f ,  g ,  h ,  i ,  j ,  k ,  l ,  m ,  n ,  o ,*/
	  0x0 ,(byte)'t' ,(byte)'v' ,(byte)'g' ,(byte)'h' ,0x0 ,0x0 ,(byte)'c' ,(byte)'d' ,0x0 ,0x0 ,(byte)'m' ,0x0 ,(byte)'k' ,0x0 , 0x0,
	  /*p , q ,  r ,  s ,  t ,  u ,  v ,  w ,  x ,  y ,  z ,  { ,  | ,  } ,  ~ , del,*/
	  0x0 ,0x0 ,(byte)'y' ,0x0 ,(byte)'a' ,(byte)'a' ,(byte)'b' ,0x0 ,0x0 ,(byte)'r' ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 ,0x0 
		};


		// Table of CRC-32's of all single byte values 
		// (made by makecrc.c of ZIP source) 
	public static long crctab[] = {
  0x00000000L, 0x77073096L, 0xee0e612cL, 0x990951baL, 0x076dc419L,
  0x706af48fL, 0xe963a535L, 0x9e6495a3L, 0x0edb8832L, 0x79dcb8a4L,
  0xe0d5e91eL, 0x97d2d988L, 0x09b64c2bL, 0x7eb17cbdL, 0xe7b82d07L,
  0x90bf1d91L, 0x1db71064L, 0x6ab020f2L, 0xf3b97148L, 0x84be41deL,
  0x1adad47dL, 0x6ddde4ebL, 0xf4d4b551L, 0x83d385c7L, 0x136c9856L,
  0x646ba8c0L, 0xfd62f97aL, 0x8a65c9ecL, 0x14015c4fL, 0x63066cd9L,
  0xfa0f3d63L, 0x8d080df5L, 0x3b6e20c8L, 0x4c69105eL, 0xd56041e4L,
  0xa2677172L, 0x3c03e4d1L, 0x4b04d447L, 0xd20d85fdL, 0xa50ab56bL,
  0x35b5a8faL, 0x42b2986cL, 0xdbbbc9d6L, 0xacbcf940L, 0x32d86ce3L,
  0x45df5c75L, 0xdcd60dcfL, 0xabd13d59L, 0x26d930acL, 0x51de003aL,
  0xc8d75180L, 0xbfd06116L, 0x21b4f4b5L, 0x56b3c423L, 0xcfba9599L,
  0xb8bda50fL, 0x2802b89eL, 0x5f058808L, 0xc60cd9b2L, 0xb10be924L,
  0x2f6f7c87L, 0x58684c11L, 0xc1611dabL, 0xb6662d3dL, 0x76dc4190L,
  0x01db7106L, 0x98d220bcL, 0xefd5102aL, 0x71b18589L, 0x06b6b51fL,
  0x9fbfe4a5L, 0xe8b8d433L, 0x7807c9a2L, 0x0f00f934L, 0x9609a88eL,
  0xe10e9818L, 0x7f6a0dbbL, 0x086d3d2dL, 0x91646c97L, 0xe6635c01L,
  0x6b6b51f4L, 0x1c6c6162L, 0x856530d8L, 0xf262004eL, 0x6c0695edL,
  0x1b01a57bL, 0x8208f4c1L, 0xf50fc457L, 0x65b0d9c6L, 0x12b7e950L,
  0x8bbeb8eaL, 0xfcb9887cL, 0x62dd1ddfL, 0x15da2d49L, 0x8cd37cf3L,
  0xfbd44c65L, 0x4db26158L, 0x3ab551ceL, 0xa3bc0074L, 0xd4bb30e2L,
  0x4adfa541L, 0x3dd895d7L, 0xa4d1c46dL, 0xd3d6f4fbL, 0x4369e96aL,
  0x346ed9fcL, 0xad678846L, 0xda60b8d0L, 0x44042d73L, 0x33031de5L,
  0xaa0a4c5fL, 0xdd0d7cc9L, 0x5005713cL, 0x270241aaL, 0xbe0b1010L,
  0xc90c2086L, 0x5768b525L, 0x206f85b3L, 0xb966d409L, 0xce61e49fL,
  0x5edef90eL, 0x29d9c998L, 0xb0d09822L, 0xc7d7a8b4L, 0x59b33d17L,
  0x2eb40d81L, 0xb7bd5c3bL, 0xc0ba6cadL, 0xedb88320L, 0x9abfb3b6L,
  0x03b6e20cL, 0x74b1d29aL, 0xead54739L, 0x9dd277afL, 0x04db2615L,
  0x73dc1683L, 0xe3630b12L, 0x94643b84L, 0x0d6d6a3eL, 0x7a6a5aa8L,
  0xe40ecf0bL, 0x9309ff9dL, 0x0a00ae27L, 0x7d079eb1L, 0xf00f9344L,
  0x8708a3d2L, 0x1e01f268L, 0x6906c2feL, 0xf762575dL, 0x806567cbL,
  0x196c3671L, 0x6e6b06e7L, 0xfed41b76L, 0x89d32be0L, 0x10da7a5aL,
  0x67dd4accL, 0xf9b9df6fL, 0x8ebeeff9L, 0x17b7be43L, 0x60b08ed5L,
  0xd6d6a3e8L, 0xa1d1937eL, 0x38d8c2c4L, 0x4fdff252L, 0xd1bb67f1L,
  0xa6bc5767L, 0x3fb506ddL, 0x48b2364bL, 0xd80d2bdaL, 0xaf0a1b4cL,
  0x36034af6L, 0x41047a60L, 0xdf60efc3L, 0xa867df55L, 0x316e8eefL,
  0x4669be79L, 0xcb61b38cL, 0xbc66831aL, 0x256fd2a0L, 0x5268e236L,
  0xcc0c7795L, 0xbb0b4703L, 0x220216b9L, 0x5505262fL, 0xc5ba3bbeL,
  0xb2bd0b28L, 0x2bb45a92L, 0x5cb36a04L, 0xc2d7ffa7L, 0xb5d0cf31L,
  0x2cd99e8bL, 0x5bdeae1dL, 0x9b64c2b0L, 0xec63f226L, 0x756aa39cL,
  0x026d930aL, 0x9c0906a9L, 0xeb0e363fL, 0x72076785L, 0x05005713L,
  0x95bf4a82L, 0xe2b87a14L, 0x7bb12baeL, 0x0cb61b38L, 0x92d28e9bL,
  0xe5d5be0dL, 0x7cdcefb7L, 0x0bdbdf21L, 0x86d3d2d4L, 0xf1d4e242L,
  0x68ddb3f8L, 0x1fda836eL, 0x81be16cdL, 0xf6b9265bL, 0x6fb077e1L,
  0x18b74777L, 0x88085ae6L, 0xff0f6a70L, 0x66063bcaL, 0x11010b5cL,
  0x8f659effL, 0xf862ae69L, 0x616bffd3L, 0x166ccf45L, 0xa00ae278L,
  0xd70dd2eeL, 0x4e048354L, 0x3903b3c2L, 0xa7672661L, 0xd06016f7L,
  0x4969474dL, 0x3e6e77dbL, 0xaed16a4aL, 0xd9d65adcL, 0x40df0b66L,
  0x37d83bf0L, 0xa9bcae53L, 0xdebb9ec5L, 0x47b2cf7fL, 0x30b5ffe9L,
  0xbdbdf21cL, 0xcabac28aL, 0x53b39330L, 0x24b4a3a6L, 0xbad03605L,
  0xcdd70693L, 0x54de5729L, 0x23d967bfL, 0xb3667a2eL, 0xc4614ab8L,
  0x5d681b02L, 0x2a6f2b94L, 0xb40bbe37L, 0xc30c8ea1L, 0x5a05df1bL,
  0x2d02ef8dL };


};
