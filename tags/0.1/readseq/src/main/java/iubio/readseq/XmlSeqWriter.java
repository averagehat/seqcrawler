//iubio/readseq/XmlSeqWriter.java
//split4javac// iubio/readseq/XmlSeqWriter.java date=27-May-2001

// iubio/readseq/XmlSeqWriter.java

package iubio.readseq;


import java.io.*;
import java.util.*;

import flybase.*;

import iubio.bioseq.BaseKind;
import iubio.bioseq.SeqInfo;
//import iubio.bioseq.Biobase;
import iubio.bioseq.Bioseq;
import iubio.bioseq.SeqRange;
import iubio.bioseq.SeqRangeException;


//split4javac// iubio/readseq/XmlSeqWriter.java line=19
public class XmlSeqWriter 
	//extends BioseqDocImpl
	implements BioseqWriterIface
	//implements BioseqReaderIface
  //	, EntityResolver, DTDHandler, DocumentHandler, ErrorHandler // == SAX HandlerBase
{
	public boolean showErrors;
	public static boolean includeDTD= true;
	public static String dtdUrl; // set from Environ?
	
	public XmlSeqWriter() {
		showErrors= Debug.isOn;
		}
	 
	final static int kSeqwidth= 78; // up from default 50, jul'99 - ASN.1 uses 78
	//final static int kLinewidth= 78;

	protected XmlPrintWriter xpr;
	
	//protected boolean newline= true;
	//protected boolean noendeol, needindent; //? same as newline
	//protected int writecol ;
	
	protected int formatId;
	protected int err;
	protected int seqlen, offset, nseq, atseq;
  protected long checksum = 0;
  
	protected int fBasePart= Bioseq.baseOnly;
	protected String seqid= SeqFileInfo.gBlankSeqid;
	protected String idword= SeqFileInfo.gBlankSeqid;
	protected Bioseq	bioseq; // change to Object, or some more flexible class
	//protected byte[]  seqbytes;
	protected Object  seqdoc;
	protected Hashtable  exfeatures;
	protected int level= 0;
	protected String dtdfile;
	
	protected String	tagBioseqSet,
		tagBioseq,
		tagSeqdata,
		tagName,
		tagDescription,
		tagChecksum,
		tagSeqlen,
		tagSeqkind;

		//
		// interface BioseqWriterIface
		//

	public int formatID() { return formatId; }
	public void setFormatID(int id) { formatId= id; } //? Readseq sets id?

	protected boolean dochecksum= true; // make a protected nonstatic
	public boolean getChecksum() { return dochecksum; }
	public void setChecksum(boolean turnon) { dochecksum= turnon; }

	protected boolean doReverse;  
	public boolean getReverseComplement() { return doReverse; }
	public void setReverseComplement(boolean turnon) { doReverse= turnon; }
	
	public final void setOutput( OutputStream outs) { // mainly for System.out
		setOutput( new OutputStreamWriter( outs)); 
		}
		
	public void setOutput( Writer outs)
	{
		if (outs instanceof XmlPrintWriter) {
			this.xpr= (XmlPrintWriter) outs;
			}
		else {
		  this.xpr= new XmlPrintWriter(outs);
//		  // this can be bad -- let caller buffer if wanted...
//			BufferedWriter bufout;
//			if (outs instanceof BufferedWriter) bufout=(BufferedWriter)outs; 
//			else bufout= new BufferedWriter(outs); 
//			this.xpr= new XmlPrintWriter(bufout);
			}
	}
	
	public void close() throws IOException { 
		xpr.close();  // no IOException from PrintWriter
		if (xpr.checkError()) throw new IOException("close error");
		}

	public int getError() { 
		if (xpr!=null &&  xpr.checkError()) err++;
		return err;
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
		if (testbaseKind == kUseTester) return testbase; else 
		return null;
		}

	public void setFeatureExtraction(Hashtable featurelist) {
		exfeatures= featurelist;
		} 

	public boolean wantsDocument() {
		if (exfeatures!=null) return true;
		BioseqFormat wrformat= BioseqFormats.bioseqFormat( this.formatID()); 
	 	return wrformat.hasdoc(); // true always?
		}

		//
		// per file methods
		//
		
	public int seqLen() { return seqlen; }
	public int getNseq() { return nseq; }

	public void setNseq(int nsequences) { this.nseq= nsequences;  }

	
	public String getDTD(String rezname) 
	{
		rezname= AppResources.global.findPath(rezname);
		if (rezname==null) return null;
		return AppResources.global.getData(rezname);
	}

	public void writeHeader() throws IOException
	{
		if (xpr==null) throw new FileNotFoundException(); //err= Readseq.eFileNotFound;
		//checksumTotal= 0; 	
		nseq= 0;	
		level= 0;
		
			// load some common tags 
		tagBioseqSet= XmlDoc.getXMLFieldName( BioseqDoc.kBioseqSet);
		tagBioseq		= XmlDoc.getXMLFieldName( BioseqDoc.kBioseq);
		tagSeqdata  = XmlDoc.getXMLFieldName( BioseqDoc.kSeqdata);
		tagName			= XmlDoc.getXMLFieldName( BioseqDoc.kName);
		tagDescription= XmlDoc.getXMLFieldName( BioseqDoc.kDescription);
		tagChecksum	= XmlDoc.getXMLFieldName( BioseqDoc.kChecksum);
		tagSeqlen		= XmlDoc.getXMLFieldName( BioseqDoc.kSeqlen);
		tagSeqkind	= XmlDoc.getXMLFieldName( BioseqDoc.kSeqkind);
	
		xpr.header();  

		if (includeDTD) {
			xpr.print("<!DOCTYPE ");
			xpr.print( tagBioseqSet);
			String sdtd= getDTD( tagBioseq + ".dtd");
			if (sdtd!=null) {
				xpr.println(" ["); 
				xpr.print( sdtd );
				xpr.print("]");
				}
			//? do SYSTEM filename.dtd ?
			else if (dtdUrl!=null) { 
			 //	 String dtdurl= Environ.gEnv.get("APP_SOURCE_URL") + tagBioseq + ".dtd";
				xpr.print(" PUBLIC \""); 
				xpr.print( dtdUrl); xpr.print( tagBioseq); xpr.print(".dtd");
				xpr.print("\"");
				}
			xpr.println(">");
			}
		xpr.println();
				
		if (exfeatures!=null) {
			xpr.commentStart();
			xpr.print("Sequence ");
			//try {
			//boolean notwant= "false".equals( exfeatures.elements().nextElement());
			//if (notwant) xpr.print("NOT");
			//} catch (Exception e) {} // NoSuchElementException
			xpr.println(" extracted for these features:");
			Enumeration en= exfeatures.keys();
			while (en.hasMoreElements()) {
				String key= (String)en.nextElement();
				String val= (String)exfeatures.get(key);
				xpr.println( key + "=" + val);
				}
			xpr.commentEnd();
			}

		xpr.writeStartElement( tagBioseqSet, level);
		level++;
	}
	
	public void writeTrailer() {
		level--;
		xpr.writeEndElement( tagBioseqSet, level);
		xpr.flush(); 
		}


			/** per sequence */

	public boolean setSeq( SeqFileInfo si) {
		if (si.ismask) return false;
		//? pull checksum from si if there? or from seq.?
		return setSeq( si.seq, si.offset, si.seqlen, si.seqid, si.seqdoc, si.atseq, Bioseq.baseOnly);
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
		
	public boolean setSeq( Object seqob, int offset, int length, String seqname,
						 Object seqdoc, int atseq, int basepart) 
	{
		if ((length<1 || seqob==null) && seqdoc!=null) {
			//? use if no seq - now need dummy seqob
			seqob= new byte[] {(byte)'N'};
			length= 1;
			}
		this.seqlen= length;
		this.checksum= 0;  //?
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
			setSeqName( seqname);
			setSeqPart( basepart);
			
			if (exfeatures!=null && seqdoc instanceof BioseqDocImpl) {
				BioseqDocImpl bdi= (BioseqDocImpl)seqdoc;
				bdi.setWantedFeatures(exfeatures);
				SeqRange featsr= bdi.getFeatureRanges( offset, seqlen);

				//if (featSubrange!=null && !featSubrange.isEmpty()) 
				//	featsr= featsr.subrange(featSubrange);
				
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
		return (this.seqlen>0);
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
	
		// same as BioseqRecord.extractBases() !!? -- now it is
	/****
	protected boolean extractBases( SeqRange range)
	{
				// do reverse-complement here if desired? (if range == complement)
				// ? this should be Bioseq method ?
		//? if (range==null) throw new SeqRangeException("Null SeqRange");  
		if (range==null) return false;  

		int totlen= 0;
		boolean mainrevcomp= range.isComplement() ;   // test each .next ?

		int seqstart= range.start();
		int seqstop = range.max();
		int seqbases= seqstop - seqstart + 1;

		for (SeqRange sr= range; sr!=null; sr= sr.next())  {
			//xr//if (sr.isRemote()) continue;
			totlen += sr.nbases();
			}
		if (totlen > 0) {
			int bat= 0;
			byte[] ba= new byte[totlen];
			byte[] bases= null; //= bioseq.toBytes(); // always? or check bioseq.isBytes() ?
					//! handle case of this.offset > 0 !

			if (bioseq.isBytes()) {
				seqstart= 0;
				bases= bioseq.toBytes();
				}
			else {
				bases= bioseq.toBytes(seqstart, seqbases, 0); // only get needed bytes?
				}
					
			int seqkind= bioseq.getSeqtype();
			boolean isamino= (seqkind == Bioseq.kAmino);
			boolean isrna= (seqkind == Bioseq.kRNA);
			for (SeqRange sr= range; sr!=null; sr= sr.next())  {
				//xr//if (sr.isRemote()) continue;
				
				int start= sr.start(); // is this correct for this.offset>0 ?
				start -= seqstart;
				int len= sr.nbases();
				
				boolean revcomp;
				if (mainrevcomp) revcomp= mainrevcomp;
				else revcomp= sr.isComplement(); 
				
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
					//int baend= bat + len - 1;
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
  ***********/

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
  }

	
	public void writeSeqRecord() throws IOException  // -- does all below methods, given seqSeq()
	{
	  if (testbaseKind == kUseTester) testbase.outSeqChar( -1); // reset
		writeRecordStart();  
		writeDoc();  
		writeSeq();   
		writeRecordEnd();
	}
			
		/**  write sequence data, when all seq is known from setSeq()  */
	public void writeSeq() 
	{
		xpr.writeStartElement( tagSeqdata, false, level);
		int indent= 2*level + tagSeqdata.length()+2;
		if (bioseq.isBytes() && testbaseKind != kUseTester) {
			byte[] ba= bioseq.toBytes();
			for (int i= 0, len = kSeqwidth - indent;
				i<seqlen;
				i += len, len= kSeqwidth) {
					if (len+i>seqlen) len= seqlen-i;
					//? xpr.tab(level*2);
					xpr.writeCharacters( ba, offset+i, len);
					//if (i+len<seqlen) 
					if (seqlen>=kSeqwidth) xpr.println();  
					}
			}
		else {
			for (int i= 0, iwid= 0, len = kSeqwidth - indent; 
				i < seqlen; 
				i ++) {
		   	//if (xpr.atNewline()) xpr.tab(2*level); 
			   	char bc= bioseq.base(offset+i,fBasePart);
		      if (testbaseKind == kUseTester) bc= (char) testbase.outSeqChar( bc);
		      if (bc>0) { 
						xpr.printEncoded( bc);  iwid++;
						if (iwid >= len) { xpr.println(); iwid= 0; len= kSeqwidth; }
						}
				}
			}
		xpr.writeEndElement( tagSeqdata, false, level);
	}  
	
	
		/**  end of seq before newline or end of record  */
	public void writeSeqEnd() {
		}  

	
		/** start of sequence record, initialize per seq, subclasses customize as needed */
	public void writeRecordStart() {
		//atseq++;  - setseq sets this?
		nseq++; // which do we use?
		//linesout = 0; //? per seq
		checksum = 0; //? reader may have checksum...

		xpr.writeStartElement( tagBioseq, level);
		level++;
		if (dochecksum && checksum==0) checksum= calculateChecksum();
		}  
		
		/**  end of seq record  */
	public void writeRecordEnd() {  
		level--;
		xpr.writeEndElement( tagBioseq, level);
		xpr.println();	
		}  


		// for BioseqWriter -- may as well do all here...
	protected void writeID() {
		xpr.writeTag( tagName, idword, level);
		}

	protected void writeStats() {
		xpr.writeTag( tagSeqlen, String.valueOf(seqlen), level);
		int sqtype= bioseq.getSeqtype();
		String skind= SeqInfo.getKindLabel( sqtype);
		xpr.writeTag( tagSeqkind, skind, level);
		String cks= checksumString();
		if (cks.length()>0) xpr.writeTag( tagChecksum, cks, level); 
		}
				
		/**  write documentation for record, form where all doc is known from setSeq() */
	public void writeDoc()
	{
		//String doctag=  tagBioseqDoc;// this is an unneeded wrapper tag
		//writeStartElement(doctag); level++;
		
		if (seqdoc instanceof BioseqDoc) {
			XmlDoc xdoc= new XmlDoc((BioseqDoc)seqdoc); //? new or use self?
			//xdoc= this; // was this
			//xdoc.setSourceDoc((BioseqDoc)seqdoc);
			xdoc.setIndent(level);
			if (xdoc.getID()==null) { writeID(); writeStats(); }
			xdoc.writeTo( xpr, true);
			}
		else {
			writeID();
			xpr.writeTag( tagDescription, seqid, level);
			writeStats(); // always??
			}
			
		//level--; writeEndElement(doctag);
 	}
		
	
	
	protected java.util.zip.Checksum summer;
	
	protected long calculateChecksum() {
		if (BioseqWriter.gJavaChecksum && summer==null) summer= new java.util.zip.Adler32();
		return BioseqWriter.calculateChecksum( bioseq, offset, seqlen, summer);
		}
	
	protected String checksumString() {
		if (checksum==0) return ""; else 
		return  Long.toHexString(checksum).toUpperCase(); //Fmt.fmt( checksum, 0, Fmt.HX)
		}


}
