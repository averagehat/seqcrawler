//iubio/readseq/Testseq.java
//split4javac// iubio/readseq/Testseq.java date=04-Jun-2003

// iubio.readseq.Testseq.java
// d.g.gilbert, 1990-1999


package iubio.readseq;

import java.io.*;

import flybase.OpenString;
import flybase.Debug;

import iubio.bioseq.SeqInfo;
//import iubio.bioseq.SeqKind;
import iubio.bioseq.Bioseq;


/**
 * Test biosequence input stream for known formats
 *
 * @author  Don Gilbert
 * @version 	Jul 1999
 * @see iubio.readseq.Readseq
 */
//split4javac// iubio/readseq/Testseq.java line=24
public class Testseq 
{
	public static int maxlines2check= 500; // change to use kBufferSize
	public static int kBufferSize = 4094; //8192; 
	protected static int plainFormatID= BioseqFormats.formatFromContentType("biosequence/plain");

	protected boolean done;
	protected int	splen, nbytes, nlines;
	protected int	format, skiplines, maxbytes2check;
	protected SeqInfo seqkind;
	protected Reader fIns, fNewIns;

	private final static boolean bTestHtmlFilter = true;  
	private final static boolean bTestGzipFilter = false;  
	private final static boolean bSuckAll = true; //! bTestHtmlFilter; // not if attaching FilterReader's to Reader // true;
	
	public Testseq()	{
		initTest();
		}
	
	public int getFormat() { return format; }
	public int getStartLine() { return skiplines; }
	public Reader getPossibleNewInputReader() { return (fNewIns!=null) ? fNewIns : fIns; }
	public Reader getFilterReader() { return fNewIns; }
	
	protected void initTest() {
		format= BioseqFormats.kUnknown;
		done= false;
		nbytes= nlines= splen= skiplines= 0;
		maxbytes2check= kBufferSize - 200;
		//seqkind= new SeqKind( maxbytes2check, false, false);
		seqkind= SeqInfo.getSeqInfo( maxbytes2check, false, false);
		}
		
	protected final OpenString readLine() {
if (bSuckAll) {
		return readOSLine();
} else {
		return readInsLine();
}
		}

	private OpenString sp;
	private BufferedReader dis;
	
	protected OpenString readInsLine()  
	{
		//OpenString 
		sp= null; 
		splen= 0;
		try {
			//done |= dis.available() <= 0; 
			String s= dis.readLine();
			if (s==null) done= true;
			else {
				sp= new OpenString( s); 
				splen = sp.length(); 
				++nlines; nbytes += splen; 
				}
			}
		catch (IOException ex) { done= true; }
		return sp;
	}

	private char[] osval;
	private OpenString osbuf;
	private int oslinei, oslen, osnewlinesize= 1;
	private char osnewline; // add skipfactor for \r\n newlines?
	
	protected OpenString readOSLine()
	{
		//OpenString sp; 
		if (oslinei<0) {
			done= true;
			splen= 0;
			sp= null;
			}
		else {
			int e= osbuf.indexOf( osnewline, oslinei);
			if (e<oslinei) {
				sp= osbuf.substring( oslinei);
				oslinei= -1; // eof
				}
			else {
				sp= osbuf.substring( oslinei, e); //? e+osnewlinesize
				oslinei= e+osnewlinesize;  
				nbytes += osnewlinesize;
				}
			splen= sp.length(); // oslen - oslinei
			++nlines; nbytes += splen;
			}
		return sp;	
	}
	
	protected void openStream( Reader ins) {	
if (bSuckAll) {
			// suck all into one openstring -- reader will reset stream as needed !?
		if (osval==null) osval= new char[kBufferSize];
		try { 
			oslen= ins.read( osval); 
			osbuf= new OpenString( osval, 0, oslen);
			oslinei= 0;
			} catch (IOException e) { oslen= 0; oslinei= -1; }
		
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
} else {
		//DataInputStream dis= new DataInputStream( ins);
		if (ins instanceof BufferedReader) dis= (BufferedReader)ins;
		else dis= new BufferedReader( ins, kBufferSize);
		try { dis.mark( kBufferSize); } catch (IOException e) {}
}
	}

	protected boolean checkMailHeader()
	{
  	int hat= sp.indexOf("From ");
 		if (hat!=0) hat= sp.indexOf("Received");
    if (hat==0) {
     	int k= 0;
      do { 	         			// skip all lines until find one blank line 
  			sp= readLine();  
      	for (k=0; (k<splen) && (sp.charAt(k)<=' '); k++) ;
        } while ((!done) && (k < splen));
      skiplines = nlines; // !? do we want #lines or #bytes ?? 
      return true;
     	}
		return false;
	}

   
	public final static int GZIP_MAGIC = 0x8b1f; // GZIP header magic number.

	protected boolean checkCompressedData()
	{
 		// if (readUShort(in) != GZIP_MAGIC)  
 	/*
		if (bTestGzipFilter && sp.length()>=1) {
					// need to change openStream(), other .read() handlers to test 1st two bytes
					// for GZIP_MAGIC 
			int b0= (byte) sp.charAt(0);
			int b1= (byte) sp.charAt(1);
			int v= (b1 << 8) | b0;
			if (v == GZIP_MAGIC) {
				fIns.reset(); // can do? - dont count on it
				fNewIns= new GZIPInputStream(fIns); 
				fIns= fNewIns;
				openStream(fNewIns);//? causing problems?
 				skiplines = 0; //!? not w/ filter? - reset should skip nbytes
   			nlines= 1; //!?
				sp= readLine();
				return true; //?
				}
			}
		*/
		return false;
	}
	
	
	protected boolean checkHtmlHeader()
	{
		boolean gotpre= false;
		int hat= sp.indexOf("<HTML");
		if (hat<0) hat= sp.indexOf("<html");
		if (hat<0) hat= sp.indexOf("<BODY");
		if (hat<0) hat= sp.indexOf("<body");
		if (hat<0) { // gosh, srs html is messy - sends only <PRE>LOCUS... on many servers
			hat= sp.indexOf("<PRE");
			if (hat<0) hat= sp.indexOf("<pre");
			if (hat>=0) { gotpre= true; sp= sp.substring(hat+5); splen = sp.length(); } // -= at + 5;			 
			}
		if (hat>=0) {
	    while (!(gotpre || done)) {
				sp= readLine();
				if (!done) {
					int at= sp.indexOf("<PRE>");
					if (at<0) at= sp.indexOf("<pre>");
					if (at>=0) { gotpre= true; sp= sp.substring(at+5); splen = sp.length(); } // -= at + 5;
	        }
	      }  
	    skiplines = nlines-1; //! need skipbytes? need to index past <pre> 
	if (bTestHtmlFilter) {
			fNewIns= new HTMLFilterReader(fIns, nbytes - splen - 1); // need -1 
			Debug.println("using HTMLFilterReader");
	   	skiplines = 0;  
	  	nlines= 1; 
			/*
			//! filter is dropping newlines, throwing off skiplines value!
			fNewIns= new HTMLFilterReader(fIns, 0); //nbytes - splen
			Debug.println("using HTMLFilterReader");
			// openStream(fNewIns);//? causing problems?
	    //skiplines = 0; //!? not w/ filter? - reset should skip nbytes
	    //nlines= 1; //!?
			*/
	}
			return true;
			}
		return false;
	}	
	
	
	public int testFormat( Reader ins, SeqFileInfo si) 
	{
	  int maybeskip = 0, iform, bestform = -1, bestpct = 0,
				otherlines= 0, aminolines= 0, dnalines= 0;
		
		fIns= ins;
	  if (ins == null) { 
	  	si.err = -1; //eFileNotFound
	  	format= BioseqFormats.kNoformat; 
	  	return format; 
	  	// throw new FileNotFoundException();
	  	} 
	 
		initTest();
		BioseqFormats.formatTestInit();
	 	openStream( ins); 			
		boolean headChecked= false;
		
		//? hack to pass seqkind to PlainSeqFormat tester??
		BioseqFormat psf= BioseqFormats.bioseqFormat(
								BioseqFormats.formatFromName("raw"));
		if (psf instanceof PlainSeqFormat)  
		  ((PlainSeqFormat)psf).setSeqInfoTester(seqkind);
			
	  while ( !done ) {
	    //OpenString sp;
	    sp= readLine();

	    			// check for mailer head & skip if found 
	    if (nlines < 10 && !done && !headChecked) {
				//? add test of instream for gzip format !?  
				// if (checkCompressedData()) ; 
				if (checkHtmlHeader()) headChecked= true;
	   		else if (checkMailHeader()) headChecked= true;
	      }

	    boolean testLineFound= false;
	    if (sp!=null && splen > 0) {
				for (iform=1; iform <= BioseqFormats.nFormats(); iform++) 
				try {
					if (BioseqFormats.formatTestLine( iform, sp, nlines, skiplines) ) {
						testLineFound= true;
						//?? break;
						}
					}
				catch (Exception fmte) { if (Debug.isOn) fmte.printStackTrace(); }

		   	if (!testLineFound || nlines - skiplines > 10) {
					seqkind.add( sp.getValue(), sp.getOffset(), splen);
					//seqkind.add( sp.toString(), 0, splen);		     	
		      		// kRNA && kDNA are fairly certain...
		      int skind= seqkind.getKind();
		      //Debug.println("Testseq line="+nlines+" len="+splen+" seq kind="+skind);
		      switch (skind) {
		        case SeqInfo.kDNA     :
		        case SeqInfo.kRNA     : if (splen>20) dnalines++; break;
		        case SeqInfo.kAmino   : if (splen>20) aminolines++; break;
	        	case SeqInfo.kOtherSeq: otherlines++; break;
		        default:
		        case SeqInfo.kNucleic : break; // not much info ? 
			    	}
	      	}
				}
			
			for (iform=1, bestform= -1, bestpct= 0; 
				iform<= BioseqFormats.nFormats(); iform++) 
				try {
					int percent= BioseqFormats.formatTestLikelihood(iform); 
					if (percent > bestpct) {
						bestform= iform;
						bestpct= percent;
						}
					}
			  catch (Exception fmte) { if (Debug.isOn) fmte.printStackTrace(); }
	
			if (bestform>=0 && bestpct >= 90) {
				format= BioseqFormats.formatFromIndex(bestform);  
				done= true;
				}

	    else if (done 
	    	//|| (dnalines > 1)  
	    	//|| (aminolines > 1) //!?!? need for prot data, but confused with regular text !!!
	    	|| (nbytes > maxbytes2check)
	    	) {
	          // decide on most likely format 
	      if (bestform >= 0 && bestpct > 50) 
	      	format= BioseqFormats.formatFromIndex(bestform);  
	     
	          // no format chars: 
	      else if (otherlines > 0) format= BioseqFormats.kUnknown;
	      else if (dnalines > 0) format= plainFormatID;  
	      else if (aminolines > 0) format= plainFormatID;  
	      else format= BioseqFormats.kUnknown;

	      done= true;
	      }

	    	// need this for possible long header in olsen format (
	     //else if (sp.indexOf("): ")>=0)   
	       // maxlines2check++;
	    }
				
		if (bestform>=0) maybeskip= BioseqFormats.recordStartLine(bestform) - 1;
		if (skiplines==0 && maybeskip>0) skiplines= maybeskip;  
		si.format= format;
		si.skiplines= skiplines;	
	  return format;
	}  

	
} //Testseq




//public 
class HTMLFilterReader extends FilterReader
{
		//? assume underlying Reader is buffered, so by-char work should be okay?
	
  public HTMLFilterReader(Reader in, int skipbytes) { super(in); this.skipbytes= skipbytes; }

	final int kMaxbuf= 128;
	private char[] inbuf = new char[kMaxbuf];
	private int nbuf, atbuf;
	private int skipbytes;

	private boolean didskipbytes; // DEBUG
	
	public void reset() throws IOException {
		in.reset();
		if (skipbytes>0) { 
			in.skip( skipbytes); //!? what is messed up w/ this? - causes ready() == available() to fail!
			//char[] cbuf= new char[skipbytes]; in.read(cbuf);
			didskipbytes= true;  //! this is bad, for some reason!
			//Debug.println("Hrdr skip=" + skipbytes);
			}
    }

	public boolean ready() throws IOException {
		if (didskipbytes) return true; //! this is the bug - in.ready() is false for some reason after skip()
		else return in.ready();
    }
	
  
  public int read() throws IOException 
  {
		//return in.read();
		didskipbytes= false;
		
		if (nbuf>0) {
  		if (atbuf<nbuf) return inbuf[atbuf++];
  		else { atbuf= nbuf= 0; }
  		}
  		
  	int c= in.read();
  		//? can we leave in these <urls> ??
  	 
  	if (c == '<') {
  		while (c != '>') { c= in.read(); if (c<0) return c; }
  		return this.read();
  		}
  	else  
  	 
  	if (c == '&') {
   		// atbuf == nbuf == 0 here
   		if (nbuf<kMaxbuf) inbuf[nbuf++]= (char) c;
 			while (c != ';' && nbuf<kMaxbuf) {
  			c= in.read();
  			if (c<0) { return inbuf[atbuf++]; }
   			else inbuf[nbuf++]= (char) c;
  			}
  		String s= new String( inbuf, atbuf, nbuf-atbuf);
  		if ("&lt;".equals(s)) { nbuf= atbuf; return '<'; }
  		else if ("&gt;".equals(s)) { nbuf= atbuf; return '>'; }
  		else if ("&amp;".equals(s)) { nbuf= atbuf; return '&'; }
   		else if ("&nbsp;".equals(s)) { nbuf= atbuf; return ' '; }
 			//? others?
 			else return inbuf[atbuf++];
  		}
  	return c;
	}

 	/*public int read(char cbuf[], int off, int len) throws IOException {
  	int nread= read0(cbuf,off,len);
  	if (nread>=0) Debug.println("Hrdr:" + new String(cbuf,off,nread));
  	return nread;
  	}*/
  	
  public int read(char cbuf[], int off, int len) throws IOException {
		//return in.read(cbuf, off, len);
		int nread= 0;
		for (int i=0; i<len; i++) {
			int c= this.read(); 
			if (c<0) return (i==0 ? -1 : nread); // i
			cbuf[off+i]= (char)c; nread++; // won't i do?
			}
		return nread; //len; //?
  	}
 
	
}
