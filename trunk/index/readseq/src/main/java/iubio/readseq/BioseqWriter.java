//iubio/readseq/BioseqWriter.java
//split4javac// iubio/readseq/BioseqWriter.java date=25-Jun-2002

// iubio.readseq.BioseqWriter.java -- was seqwriter.java
// d.g.gilbert, 1990-1999

	
package iubio.readseq;

import java.io.*;

import java.util.Hashtable;
import java.util.zip.Checksum;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

import Acme.Fmt;

import flybase.OpenString;


import iubio.bioseq.BaseKind;
import iubio.bioseq.Bioseq;
import iubio.bioseq.SeqRange;
import iubio.bioseq.SeqRangeException;


//split4javac// iubio/readseq/BioseqWriter.java line=24
public class BioseqWriter
	implements BioseqWriterIface
	// implements Cloneable
	//?? extends Writer
{
	public static boolean gJavaChecksum= true;
	public static boolean gShortChecksum= false;
	public static Checksum summer;
	public static String kNocountsymbols= "_.-?";


	protected Writer outs;
	protected Writer douts;  // ! don't want to make a new writer here, in case caller passes special writer !
	//protected BufferedWriter douts; // was this, apr'00 change to Writer
	//protected PrintWriter prouts;
	//private DataOutputStream douts;  //? both forms, let caller choose? - too messy - child classes need to use
	protected String lineSeparator;

	protected int formatId;
	protected int seqlen, offset, nseq, atseq;
	protected String seqid= SeqFileInfo.gBlankSeqid;
	protected String idword= SeqFileInfo.gBlankSeqid;

	protected Bioseq	bioseq; // change to Object, or some more flexible class
	protected Object  seqdoc;
	protected Hashtable  exfeatures;
	protected SeqRange  featSubrange;
	
	protected final static int kSpaceAll = -9;
	private   final static int kMaxseqwidth = 2048; //512; //? need this? buffer size
	
	protected WriteseqOpts opts = new WriteseqOpts();
  protected int l1 = 0, linesout = 0; 
  protected String nocountsymbols;
  protected long checksum = 0;
	public static long checksumTotal;
	protected int fBasePart= Bioseq.baseOnly;
	
	protected boolean dochecksum; // make a protected nonstatic
	protected String checksumtype; 
	public boolean getChecksum() { return dochecksum; }
	public void setChecksum(boolean turnon) { dochecksum= turnon; }
	public void setChecksum(boolean turnon, String checktype) { dochecksum= turnon; checksumtype=checktype; }
	
	protected boolean doReverse;  
	public boolean getReverseComplement() { return doReverse; }
	public void setReverseComplement(boolean turnon) { doReverse= turnon; }
	
	
	public BioseqWriter() {
		lineSeparator = System.getProperty("line.separator");
		}

	public void setOpts(WriteseqOpts newopts) { 
		if (newopts!=null) opts= newopts;  //? only if instanceof PrettySeqWriter?
		}
	public WriteseqOpts getOpts() { 
		return opts;
		}
	
			//
			// interface BioseqWriterIface
			//
			
	public int formatID() { return formatId; }
	public void setFormatID(int id) { formatId= id; } //? Readseq sets id?

	public Writer getOutput() { return this.outs; }

	public final void setOutput( OutputStream outs) { // mainly for System.out
		setOutput( new OutputStreamWriter( outs)); 
		}
		
	public void setOutput( Writer outs) { 
		this.outs= outs; 
		//if (outs instanceof PrintWriter) prouts= (PrintWriter) outs; // apr'00 - okay? no - subclass uses douts
		//else 
		douts= outs; // apr'00 fix - keep caller's writer
		//if (outs instanceof BufferedWriter) douts=(BufferedWriter)outs ;//DataOutputStream
		//else douts= new BufferedWriter(outs);//DataOutputStream
		//prouts= new PrintWriter(douts); // gack !!!
		err= 0; 
		}
		
	public void close() throws IOException { 
		outs.close(); //douts.close();  
		}

	protected final static int kUseTester= 1, kAnyChar= 2, kAlphaChar= 3;
	protected OutBiobaseIntf testbase= new OutBiobase(null);
	protected int testbaseKind= kAlphaChar;
	
	
	public void setOutputTranslation( OutBiobaseIntf tester)
	{
		testbase= tester;
		if (tester==null) testbaseKind= kAlphaChar;
		else testbaseKind= kUseTester;
	}
	
	public OutBiobaseIntf getOutputTranslation() {
		if (testbaseKind == kUseTester) return testbase;
		else return null;
		}
		
		//
		// per file methods
		//
		
	protected int seqLen() { return seqlen; }
	protected int getNseq() { return nseq; }

	public void setNseq(int nsequences) { this.nseq= nsequences;  }

	public void writeHeader()  throws IOException {   
		if (douts==null) throw new FileNotFoundException(); //err= Readseq.eFileNotFound;
		checksumTotal= 0; 	
		nseq= 0;	
		} 
		
	public void writeTrailer() {  
		try { douts.flush(); } catch (IOException ex) {}
		} 


		//
		// per sequence methods
		//
		
	//public void writeDoc( DocumentData doc) {}  // for streaming pipe ? handle chunks of doc
	//public void writeSeq( BaseData seq) { writeLoop(); } // for streaming pipe ? handle chunks of seq
	public void writeDoc() {}   
	public void writeSeq() { writeLoop(); }  
	public void writeSeqEnd() { }  
	public void writeRecordEnd() { writeln(); }  
		
	//protected void writeSeqdoc() {} // per sequence --> writeDoc( DocumentData)
	
	public void writeRecordStart() // was writeInit()
	{
		atseq++; nseq++; // which do we use?
		l1= linesout = 0;
		checksum = 0; // reader may have checksum...
		//! don't need plainInit() can bollux up other uses
		// if (!opts.userchoice) opts.plainInit();
		opts.numwidth= Fmt.fmt(seqlen).length() + 1;
	}
		
			// a convenience class - drop for Writeseq method?
	public void writeSeqRecord() throws IOException {  
	  if (testbaseKind == kUseTester) testbase.outSeqChar( -1); // reset
		writeRecordStart(); // was writeInit();  
		writePostInit(); 		//? need after writeInit subclassing; move to writeSeqDoc()?
		writeDoc();  
		writeSeq();   
		writeRecordEnd();
		}

	public void setFeatureExtraction(Hashtable featurelist) {
		exfeatures= featurelist;
		} 

			// ! distinguish extract-range and subrange
			//  featuresubrange == relative to feature range
			//  extractrange == absolute start..stop (for all features)
	public void setFeatureSubrange(SeqRange extractSubrange) {
		featSubrange= extractSubrange;
		} 
		
	public boolean wantsDocument() {
		if (exfeatures!=null) return true;
		BioseqFormat wrformat= BioseqFormats.bioseqFormat( this.formatID()); 
	 	return wrformat.hasdoc();
		}
		
	public boolean setMask( SeqFileInfo si, String masktag) {
		int  bpart= Bioseq.baseOnly;
		if (si.ismask)	
			bpart=  Bioseq.baseOnly;
		else if (si.hasmask)  
			bpart= Bioseq.maskOnlyAsText;
		else return false;
		masktag= si.seqid.toString() + masktag;
		return setSeq( si.seq, si.offset, si.seqlen, masktag, si.seqdoc, si.atseq, bpart);
		}	

	public boolean setSeq( SeqFileInfo si) {
		if (si.ismask) return false;
		return setSeq( si.seq, si.offset, si.seqlen, si.seqid, si.seqdoc, si.atseq, Bioseq.baseOnly);
		}
		
	public boolean setSeq( Object seqob, int offset, int length, String seqname,
						 Object seqdoc, int atseq, int basepart) 
	{
		if ((length<1 || seqob==null) && seqdoc!=null) {
			//? use if no seq - now need dummy seqob
			seqob= new byte[] {(byte)'N'};
			length= 1;
			}
			
		this.seqlen= length;
		if (length>0 && seqob!=null) {
			if (seqob instanceof Bioseq) {
				this.bioseq= (Bioseq) seqob;
				}
			else if (seqob instanceof byte[]) {
				this.bioseq= new Bioseq();
				this.bioseq.setbases((byte[])seqob);
				} 
			this.atseq= atseq; 
			this.offset= offset;
			this.seqdoc= seqdoc;
			setSeqName(seqname);
			setSeqPart(basepart);
			
			if (exfeatures!=null && seqdoc instanceof BioseqDocImpl) {
				BioseqDocImpl bdi= (BioseqDocImpl)seqdoc;
				bdi.setWantedFeatures(exfeatures);
				SeqRange featsr= bdi.getFeatureRanges( offset, seqlen); // need offset, seqlen here !?
				
				// featsr is union/joinRange of all extracted feats
				// ?? modify here by  SeqRange subrange= featsr.subrange(featSubrange);
				// or do we need to modify each feat in getFeatureRanges ?
				// 17may01 -- not ready? needs at least output seq range (WriteOpts.origin)
				// check when extractrange misses feat range - get only intersection?
				
				// ! need option here? to split this record into several for each extracted
				// feature... rather than extract multiple feature seqs into one record
				// -- use with  writeSeqRecord, as in
				//   if (seqwriter.setSeq( si)) seqwriter.writeSeqRecord();

				
				if (featSubrange!=null && !featSubrange.isEmpty()) 
					featsr= featsr.subrange(featSubrange);
				
				if (extractBases( featsr))
					bdi.replaceDocItem( BioseqDocVals.kSeqlen, 
						new DocItem("length", String.valueOf(seqlen), BioseqDocVals.kSeqlen, BioseqDocVals.kField));
				}
				
			else if (doReverse) {
				bioseq.reverseComplement( offset, seqlen);	
				if (seqdoc instanceof BioseqDocImpl) {
					BioseqDocImpl bdi= (BioseqDocImpl)seqdoc;
					bdi.addComment( "NOTE:  This is reverse-complement of original sequence." );
					}
				}
				
			}		
		return (this.seqlen>0); //?
	}


		// NOW same as BioseqRecord.extractBases()/XmlWriter.extractBases()
	public static Bioseq extractBioseqBases(Bioseq inseq, SeqRange range, int seqlen, String seqid) 
	    throws SeqRangeException
	{
		if (range==null) throw new SeqRangeException("Null SeqRange");  

		// if (!range.isComplex()) revise to use same Bioseq.bSeq array, w/ offset, length change
		//int seqstop = range.max(); //bad for unordered parts
		//int seqbases= seqstop - seqstart + 1;
    	 	
		//xr//boolean hasremote= range.isPartRemote(null); // null should be default loc id:
		int totlen=0;
		int seqstart= range.start();
		int seqend= seqstart; //range.max(); //? or rely on below
		for (SeqRange sr= range; sr!=null; sr= sr.next())  {
			//xr//if (sr.isRemote()) continue;
			totlen += sr.nbases(); 
			  //^^ !BUG - need to read from range.min to range.max for bases
			  // iubio.bioseq.BioseqFiled.readBases
			int srb= sr.start();  int sre= sr.stop();
			if (srb < seqstart) seqstart= srb; 
			if (sre > seqend) seqend= sre;
			}
		int totbyterange= seqend - seqstart + 1;
	 
	  //DEBUG//
	  //DEBUG//System.err.println(" extractBioseqBases="+range+" start="+seqstart+" len="+totlen+" totrange="+totbyterange);

    //?extractBioseqBases=join(complement(3R:17191746),17182690..17182691) start=-9056 len=3
    // why srb < 0 ?? -- bad GenomeMap2Seq manip. >> now have, but no compl(1base)
    //extractBioseqBases=join(complement(3R:17191746),17182690..17182691) start=0 len=3

		if (totlen<=0)	throw new SeqRangeException("Empty SeqRange");  
			
    int blen=0; //int bat= 0;
    byte[] ba= new byte[totlen];
    byte[] bases= null;  
    if (inseq.isBytes()) {
      seqstart= 0;
      bases= inseq.toBytes();
      }
    else {
      bases= inseq.toBytes( seqstart, totbyterange, 0); // only get needed bytes?
      }
      
    int seqkind= (bases==null)? 0 : inseq.getSeqtype(bases,0,bases.length);
    boolean isamino= (seqkind == Bioseq.kAmino);
    boolean isrna= (seqkind == Bioseq.kRNA);
		boolean mainrevcomp= range.isComplement() ;   // test each .next ?
		boolean isfirst= true;
		StringBuffer msg= new StringBuffer("extractBioseqBases - Bad range ");
    
    for (SeqRange sr= range; sr!=null; sr= sr.next())  {
      //xr//if (sr.isRemote()) continue; // warn?
      int start= sr.start(); // is this correct for this.offset>0 ?
      int len= sr.nbases();
      start -= seqstart;

      boolean err= false;
      if (start < 0 ) { err= true; msg.append(start).append( " start<0"); start= 0; }
      else if (start+len > seqlen ) { err= true; 
        msg.append(start+len).append( " end>").append(seqlen);
        len= Math.max(0, seqlen - start);
        }
      else if (blen+len > totlen) { err= true; 
        msg.append(blen+len).append("  size>").append(totlen);
        len= Math.max(0, totlen - start);
        }
      else err= false;
      if (err) {
        msg.append(" of sr=").append(sr).append( " in r=").append( range);
        msg.append(", record=").append(seqid); 
        //? if (Debug.isOn) Debug.println(msg.toString()); else 
        throw new SeqRangeException(msg.toString());
        }
        
      int bastart= blen;
      int baend  = blen + len - 1;
      boolean isreversed= false;
      int srop= sr.oper2(isfirst); // !! dang; ! sr.opint()
      if (srop < sr.opMax) { 
        isreversed= (srop == sr.opComplement);  
        }
      if (mainrevcomp) {
        isreversed= ! isreversed;  
        bastart= totlen - blen - len; // always add parts backwards for mainrevcomp
        }
        
      baend= bastart + len - 1; // always; if this part is reversed 
      
      // fix index into ba[] for flip/flop revcomp -- dont use bat ?  
      // does this differ if mainrevcomp or local rev?
      //baend= totlen - bat - 1;
      //baend= bat + len - 1;
      // co( co(1..2), 3..4, co(5..6) ) -> (j5..6,c4..3,j1..2)
      //DEBUG// 
	    //DEBUG//System.err.println("exb isreversed="+isreversed+" start="+start+" len="+len
	    //DEBUG//       +" b.start="+bastart+" ba.end="+baend+" ba.len="+blen);

      if (isreversed) {
        if (isamino) { for (int i= 0; i<len; i++) ba[baend-i]= bases[start+i]; }
        else {
          for (int i= 0; i<len; i++)  
            ba[baend-i]= BaseKind.nucleicComplement( bases[start+i], isrna);
          }
        }
        
      else
        System.arraycopy( bases, start, ba, bastart, len);
        
      blen += len; isfirst= false;
      }
    Bioseq newseq= new Bioseq();
    newseq.setbases(ba);
    return newseq;

	}


	protected boolean extractBases( SeqRange range)
	{
    try {
      Bioseq outseq= BioseqWriter.extractBioseqBases(this.bioseq, range, seqlen, seqid);
			this.seqlen= outseq.length();
		  // this.bioseq.setbases( outseq.toBytes());
		  //? can we just//
		  this.bioseq= outseq;
  		return true;
			}
		catch (SeqRangeException sre) {
	    System.err.println(sre);
		  return false; // throw again?
		  }
	}
	
	  
  /*****
	protected boolean extractBases( SeqRange range)
	{
		if (range==null) return false;  
		int totlen= 0;
		boolean mainrevcomp= range.isComplement() ;   // test each .next ?

		int seqstart= range.start();
		int seqstop = range.max();
		int seqbases= seqstop - seqstart + 1;

		//xr//boolean hasremote= range.isPartRemote(null); // null should be default loc id:
		for (SeqRange sr= range; sr!=null; sr= sr.next())  {
			//xr//if (sr.isRemote()) continue;
			totlen += sr.nbases();
			}
			
		if (totlen > 0) {
			int bat= 0;
			byte[] ba= new byte[totlen];
			byte[] bases= null; //= bioseq.toBytes(); // always? or check bioseq.isBytes() ?

			if (this.bioseq.isBytes()) {
				seqstart= 0;
				bases= this.bioseq.toBytes();
				}
			else {
				bases= this.bioseq.toBytes(seqstart, seqbases, 0); // only get needed bytes?
				}
					
			int seqkind= (bases==null) ? 0 : this.bioseq.getSeqtype(bases,0,bases.length);

			boolean isamino= (seqkind == Bioseq.kAmino);
			boolean isrna= (seqkind == Bioseq.kRNA);
			for (SeqRange sr= range; sr!=null; sr= sr.next())  {
				//xr//if (sr.isRemote()) continue;
				int start= sr.start(); // is this correct for this.offset>0 ?
				start -= seqstart;
				int len= sr.nbases();
				
				boolean revcomp;
				//if (mainrevcomp) revcomp= mainrevcomp; // no, need to check part op 1st
				//else revcomp= sr.isComplement(); 
				if (sr.opint()<sr.opMax) revcomp= sr.isComplement(); // should be sr.hasOp()
				else  revcomp= mainrevcomp;
				
				String err;
				if (start < 0 ) err= String.valueOf(start) + " start<0";
				else if (start+len > seqlen ) err= String.valueOf(start+len ) + " end>" + String.valueOf(seqlen);
				else if (bat+len > totlen) err= String.valueOf(bat+len) + "  size>" + String.valueOf(totlen);
				else err= null;
				if (err!=null) {
					String msg= idword + " seq range error: "+ err + " for "+ sr;
					System.err.println(msg); 
					return false; // what else to do?
					// throw new SeqRangeException(msg.toString());
					}
					
				else if (revcomp) {
					int baend= totlen - bat - 1;
					if (isamino) {
						for (int i= 0; i<len; i++) ba[baend-i]= bases[start+i];
						}
					else {
						for (int i= 0; i<len; i++)  
							ba[baend-i]= BaseKind.nucleicComplement( bases[start+i], isrna);
						}
					}
					
				else
					System.arraycopy( bases, start, ba, bat, len);
					
				bat += len;
				}
			this.bioseq.setbases( ba);
			}
		this.seqlen= totlen;
		return true;
	}
  ******/

	
	
	public void setSeqName( String name) { 
		int i;
		if (name==null) return;
		else if (name.equals(SeqFileInfo.gBlankSeqid)) 
			name= SeqFileInfo.getNextBlankID();
		seqid= name;
		seqid= seqid.trim();
		if ( seqid.indexOf("checksum") >0 ) {
	  	i= seqid.indexOf("bases");
	    if (i>0) {
	    	for ( ; i > 0 && seqid.charAt(i) != ','; i--) ;
	      if (i>0) seqid= seqid.substring(0, i);
	      }
	    }
		i= seqid.indexOf(' ');
		if (i<=0) idword= seqid;
		else { if (i>30) i= 30; idword= seqid.substring(0,i); }
		}

	public void setSeqPart(int basepart) { 
		fBasePart= basepart;
		/*switch (basepart) {
			Bioseq.baseOnly: break;
			Bioseq.maskOnly: break;
			Bioseq.maskOnlyAsText: break;
			Bioseq.nucAndMask: break;
			}*/
		}


		
	protected void writePostInit() // per sequence
	{
		if (dochecksum && checksum==0) checksum= calculateChecksum();

	 	nocountsymbols=  kNocountsymbols;
  	if (opts.baseonlynum) {
	    if (nocountsymbols.indexOf(opts.gapchar)<0)  
	    	nocountsymbols += String.valueOf(opts.gapchar);
	    if (opts.domatch && nocountsymbols.indexOf(opts.matchchar)>0)  
	     	nocountsymbols= nocountsymbols.replace(opts.matchchar, ' ');
	    }
	}
	
	protected void writeLoop() // per sequence
	{
		int i, j, k, ib, bufl, ibase;
		int spacen= opts.spacer;
		if (spacen>0) spacen++; // fix for below tests
			// check for seqlen mismatch
			// pretty calls here for writeHeader  - null bioseq !?
		int bioseqlen= offset+seqlen;
		if  (bioseq!=null) bioseqlen= Math.min(bioseqlen, bioseq.length());
		int wseqlen= seqlen;
		if (opts.numline>0 && wseqlen==0) wseqlen= opts.seqwidth; //?
		
	  opts.seqwidth = Math.min(opts.seqwidth,kMaxseqwidth); //? dont do this - may want single line/seq - see PlainSeqWriter
		char[]  bs= new char[kMaxseqwidth];
	  // add opts.origin to print -ibase values ?!
	  if (testbaseKind == kUseTester) testbase.outSeqChar( -1); // reset

	  for (i=0, bufl=0, ibase = 0; i < wseqlen; ) {

	    if (l1 < 0) l1 = 0;
	    else if (l1 == 0) {
	      if (opts.nameleft) {
	      	if (opts.numline>0) writeString( Fmt.fmt( "", opts.namewidth, 0) );  
	      	else writeString( Fmt.fmt( idword, opts.namewidth, opts.nameflags) ); //opts.nameflags
	      	}
	      if (opts.numleft) { 
	      	if (opts.nameleft) writeByte(' ');
	      	if (opts.numline>0)  
	      		writeString( Fmt.fmt( "", opts.numwidth, 0) ); //opts.numflags
	       	else  {
	          if (opts.reversed) ib= opts.origin-ibase; else ib= ibase+opts.origin;
	      		writeString( Fmt.fmt( ib, opts.numwidth, opts.numflags) ); //opts.numflags
	      		}
					}
	      for (j=0; j<opts.tab; j++) writeByte(' '); 
	      }

	    l1++;                 // dont count spaces for seqwidth 
	    if (opts.numline>0) {
	      if (spacen==kSpaceAll || (spacen != 0 && ((bufl+1) % spacen) == 1)) {
	        if (opts.numline==1) writeByte(' '); 
	        bs[bufl++] = (char)' ';
	        }
	      if (l1 % 10 == 1 || l1 == opts.seqwidth) {
	        if (opts.numline==1) writeString( Fmt.fmt( offset+i+1, 10, Fmt.LJ));
	        bs[bufl++]= (char)'|'; // == put a number here 
	        }
	      else bs[bufl++]= (char)' ';
	      i++;
	      }

	    else {
	      if (spacen==kSpaceAll || (spacen != 0 && ((bufl+1) % spacen) == 1))
	        bs[bufl++] = (char) ' ';
	      char bc;
	      if (offset+i>=bioseqlen) bc= BaseKind.indelEdge;   
	      else bc= (char)bioseq.base(offset+i,fBasePart); i++; 
	      
	      //FIXME:
	      // if (opts.domatch && atseq > 1 && topbioseq.base(offset+i) == bc) bc= opts.matchchar;
	      
	      if (testbaseKind == kUseTester) bc= (char) testbase.outSeqChar( bc);
	      if (bc>0) { 
	      	bs[bufl++] = bc; 
		      if (!opts.baseonlynum) ibase++;
		      else if (nocountsymbols.indexOf(bc)<0) ibase++;
		      }
	      }

	    if (l1 == opts.seqwidth || i == wseqlen) {
	    		 
	      if (opts.blankpad) //(outform==kPretty)
	       for ( ; l1<opts.seqwidth; l1++) {
	        if (spacen==kSpaceAll || (spacen != 0 && (bufl+1) % spacen == 1))
	          bs[bufl++] = (char)' ';
	        bs[bufl++]= (char)' '; // pad w/ blanks 
	        }
	        
	      //bs[bufl] = (char)'\0'; //!? what is null here for? Java is not C
	      int buflen= bufl;
	      bufl = 0; l1 = 0;

	      if (opts.numline>0) {
	        if (opts.numline==2) writeByteArray( bs, 0, buflen);
	         // finish numberline ! and | 
	        }
	      else {
	      	writeByteArray( bs, 0, buflen);
	        if (opts.numright || opts.nameright)  writeByte(' ');  
	        if (opts.numright) {  
	          if (opts.reversed) ib= opts.origin-ibase+1; else ib= ibase+opts.origin-1;
	        	writeString( Fmt.fmt( ib, opts.numwidth, 0));  //opts.numflags
	        	if (opts.nameright) writeByte(' ');
	        	}
	        if (opts.nameright) 
	        	writeString( Fmt.fmt( idword, opts.namewidth, Fmt.LJ)); //opts.nameflags
		    	if (i == wseqlen) writeSeqEnd();  // this can differ from writeRecordEnd() !?!
       		}
	      writeln();  
	      }
	    }
  }
  
  

		//
		// low level writers - optimize!
		//
	protected int err;
	public int getError() { return err; }
	
	protected void writeString( String s) {
		//prouts.print(s);
		//if (s!=null) 
		//if (prouts!=null) prouts.print(s); else 
		try { douts.write(s); } catch (IOException ex) { err++; }
		}

	protected void writeString( OpenString s) {
		//prouts.print(s);
		//if (s!=null) 
		//if (prouts!=null) prouts.print(s); else 
		try { douts.write(s.toString()); } catch (IOException ex) { err++; }
		}

	protected void writeByteArray(char[] ba, int offset, int len) {
		//if (prouts!=null) prouts.write( ba, offset, len); else 
		try { douts.write( ba, offset, len); } catch (IOException ex) { err++; }
		}
		
	protected void writeByte(int c) {
		//if (prouts!=null) prouts.print(c); else 
		try { douts.write(c); } catch (IOException ex) { err++; }
		}
		
	protected void writeln() {
		writeString( lineSeparator);  
		//prouts.println();
		linesout++; 
		}

	protected final void writeln( String s) {
		writeString(s); writeln();
		//prouts.println(s);
		}
		
	protected final void writeln( OpenString s) {
		writeString(s); writeln();
		//prouts.println(s);
		}
		
		
	// 07jun: add SwissProt variant of CRC32 for matching SW's output?
	// see ftp://ftp.ebi.ac.uk/pub/software/swissprot/Swissknife/old/SPcrc.tar.gz

	protected long calculateChecksum() {
	  if (checksumtype!=null) {
	    // starsWith() CRC, Adler, GCG ...
	    if(checksumtype.equalsIgnoreCase("CRC32")) { gJavaChecksum= true; summer= new CRC32(); }// alternate CRC32checksum() ?
	    else if(checksumtype.equalsIgnoreCase("Adler32")) { gJavaChecksum= true; summer= new Adler32(); }
	    else if(checksumtype.equalsIgnoreCase("GCG")) { gShortChecksum=true; gJavaChecksum= false; } // FIXME
	    else if(checksumtype.equalsIgnoreCase("CRC")) { gShortChecksum=false; gJavaChecksum= false; } // FIXME
	    // ^^ this seems to be same result as Java CRC32 , if slower
	    }
		if (gJavaChecksum && summer==null) summer= new Adler32();
		return calculateChecksum( bioseq, offset, seqlen, summer);
		}
	
	protected String checksumString() {
		if (checksum==0) return ""; else 
		return new String(  Fmt.fmt( checksum, 0, Fmt.HX).toUpperCase() + " checksum"); //classic readseq
		//return new String("0x" + Fmt.fmt( checksum, 0, Fmt.HX) + " checksum");
		}

	public static long checksumTime;		
			
	public static long calculateChecksum( Bioseq seq, int offset, int seqlen, Checksum summer) {
		long tstart= System.currentTimeMillis();
		long ckv;
		if (gJavaChecksum) ckv= ZipChecksum( seq, offset, seqlen, summer);
	 	else if (gShortChecksum) ckv= GCGchecksum( seq, offset, seqlen);
	 	else ckv= CRC32checksum( seq, offset, seqlen);
	 	checksumTime += System.currentTimeMillis() -tstart;
	 	return ckv;
		}

	public static long ZipChecksum( Bioseq seq, int offset, int seqlen, Checksum summer) {
		byte[] ba= seq.toBytes( offset, seqlen, Bioseq.baseOnly);
		//! can't do this w/o dupping
		//for (int i=0; i<ba.length; i++) if (ba[i]>='a' && ba[i]<='z') ba[i] -= 32;
 		if (summer==null) summer= new Adler32();
 		summer.reset();
		//summer.update( ba, 0, ba.length); // doesn't ignore case !
		// or for case-less value:
		for (int i=0; i<ba.length; i++) { byte b= ba[i]; if (b>='a' && b<='z') b -= 32; summer.update(b); }
	 	long c = summer.getValue();
	  checksumTotal += c;
	  return c;
		}

	public static long CRC32checksum( Bioseq seq, int offset, int seqlen) {
		// CRC32checksum: modified from CRC-32 algorithm found in ZIP compression source 
	 	long c = 0xffffffffL;
	 	if (seq.isBytes()) {
		 	byte[] ba= seq.toBytes();
			for (int i=0; i<seqlen; i++) {
				byte b= ba[i+offset]; 
				if (b>='a' && b<='z') b -= 32;
				c = BaseKind.crctab[((int)c ^ b) & 0xff] ^ (c >> 8);
				}
	 		}
	 	else {
		 	//Biobase[] bb= seq.bases();
			for (int i=0; i<seqlen; i++) {
				byte b= seq.basebyte(offset+i);
				//byte b= bb[i+offset].c; 
				if (b>='a' && b<='z') b -= 32;
				c = BaseKind.crctab[((int)c ^ b) & 0xff] ^ (c >> 8);
				}
			}	
	  c= c ^ 0xffffffffL;
	  checksumTotal += c;
	  return c;
		}
 		 
	public static long GCGchecksum( Bioseq seq, int offset, int seqlen) 
	{
		int check = 0, count = 0;
	 	if (seq.isBytes()) {
		 	byte[] ba= seq.toBytes();
		  for (int i = 0; i < seqlen; i++) {
				byte b= ba[i+offset]; 
				if (b>='a' && b<='z') b -= 32;
		    count++;
				check += count * b;
		    if (count == 57) count = 0;
		    }
	 		}
	 	else {
	 		//Biobase[] bb= seq.bases();
		  for (int i = 0; i < seqlen; i++) {
				//byte b= bb[i+offset].c; 
				byte b= seq.basebyte(offset+i);
				if (b>='a' && b<='z') b -= 32;
		    count++;
				check += count * b;
		    if (count == 57) count = 0;
		    }
	 		}
	  check %= 10000;
	  checksumTotal += check;
	  checksumTotal %= 10000;
	  return check;
		}
		
}



//! dang Sun javac -- these should all be public !

//public
class OutBiobase implements OutBiobaseIntf
{
	protected OutBiobaseIntf outtest;
	
	public OutBiobase( OutBiobaseIntf nextout) {
		this.outtest= nextout;
		}
		
	public int outSeqChar(int c) { 
		if (outtest!=null) return outtest.outSeqChar(c);  // order of chain is critical - 1st? or last
		else return c; 
		}
}

//public
class ToUppercaseBase extends OutBiobase
{
	public ToUppercaseBase(OutBiobaseIntf nextout) { super(nextout); }
	public int outSeqChar(int c) { 
		if (outtest!=null) c= outtest.outSeqChar(c);   
		if (c>='a' && c<='z') return c - 32; else return c; 
		}
}

//public
class ToLowercaseBase extends OutBiobase
{
	public ToLowercaseBase(OutBiobaseIntf nextout) { super(nextout); }
	public int outSeqChar(int c) { 
		if (outtest!=null) c= outtest.outSeqChar(c);   
		if (c>='A' && c<='Z') return c + 32; else return c; 
		}
}

//public
class ToDegappedBase extends OutBiobase
{
	char degapc= '-';
	
	public ToDegappedBase( OutBiobaseIntf nextout) {  super(nextout);  }
	public ToDegappedBase( int gapc, OutBiobaseIntf nextout) { 
		super(nextout); 
		if (gapc>0) degapc= (char) gapc;
		}
	public int outSeqChar(int c) { 
		if (outtest!=null) c= outtest.outSeqChar(c);   
		if (c == degapc) return 0;  else return c; 
		}
}

//public
class ToTranslatedBase extends OutBiobase
{
	String intrans,  outtrans;
	public ToTranslatedBase(String intrans, String outtrans, OutBiobaseIntf nextout) { 
		super(nextout);  
		this.intrans= intrans; this.outtrans= outtrans;
		}
		
	public int outSeqChar(int c) { 
		if (outtest!=null) c= outtest.outSeqChar(c);   
		int at= intrans.indexOf(c);
		if (at>=0) return outtrans.charAt(at);
		else return c;
		}
}

//public
class ToDnaComplement extends OutBiobase
{
	boolean isrna;
	public ToDnaComplement( OutBiobaseIntf nextout) {  super(nextout);  }
	public ToDnaComplement( boolean isrna, OutBiobaseIntf nextout) { 
		super(nextout); this.isrna= isrna;
		}
	public int outSeqChar(int c) { 
		if (outtest!=null) c= outtest.outSeqChar(c);   
		return (int) BaseKind.nucleicComplement( (byte)c, isrna);
		}
}


