//iubio/readseq/XmlSeqReader.java
//split4javac// iubio/readseq/XmlSeqReader.java date=01-Mar-2002

// iubio/readseq/XmlSeqReader.java

package iubio.readseq;


import java.io.*;
import java.util.*;

import flybase.*;

import iubio.bioseq.Bioseq;
import iubio.readseq.BioseqReader;
//import iubio.readseq.ReadseqException;

/*
Various free XML parsers can be used here (the SAX versions will do)
See IBM's excellent XML4J at http://www.alphaworks.ibm.com/
See Sun's XML parser at http://java.sun.com/xml
*/

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.Parser;
import org.xml.sax.helpers.ParserFactory;
//import org.xml.sax.HandlerBase; // below interfaces make up HandlerBase
import org.xml.sax.EntityResolver;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.AttributeList;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;


//split4javac// iubio/readseq/XmlSeqReader.java line=35
public class XmlSeqReader 
	implements BioseqReaderIface
		, BioseqDocVals
  	, EntityResolver, DTDHandler, DocumentHandler, ErrorHandler // == SAX HandlerBase
{
	public boolean showErrors;
	public static int readChunkSize= 2048; //8192;

	public XmlSeqReader() {
		showErrors= Debug.isOn;
		verbose= BioseqReader.verbose;
		}


	protected int formatId;
	protected int err;
	protected int seqlen, offset, nseq, atseq;
	protected int seqoffset, seqlencount, maxseqlen, choice;
	protected String seqid= SeqFileInfo.gBlankSeqid;
	protected String idword= SeqFileInfo.gBlankSeqid;
	protected boolean skipdocs, verbose;
	protected byte[]  seqbytes;
	private XmlDoc  xmlseqdoc;
	private FastVector seqvec;
	private boolean fEof= false;
	private Reader fIns;

	
	protected final static int kUseTester= 1, kAnyChar= 2, kAlphaChar= 3;
	//protected TestBiobase	testbase= new TestBiobase();
	protected int testbaseKind= kAlphaChar;
		
	private final int testbase(int c) {
		switch (testbaseKind) {
  		default:
  		case kAlphaChar	: if (c<=' ' || (c >= '0' && c <= '9')) return 0; else return c;
  		case kAnyChar		: if (c<' ') return 0; else return c;
  		//case kUseTester	: return testbase.isSeqChar(c); 
  		}
		}

	protected void message(String s) {
		// need opt to print to app text component
		BioseqReader.message(s);
		//System.err.println(s);
		}


	
		//
		// BioseqReaderIface
		//

	public int formatID() { return formatId; }
	public void setFormatID(int id) { formatId= id; } //? Readseq sets id?

	public int seqLen() { return seqlen; }
	public int getNseq() { return nseq; }

	public void setChoice(int seqchoice) { choice= seqchoice; }

	public void setInput(Reader ins) {  
		this.fIns= ins; 
		fEof= false; 
		setReaderBuf(fIns);
		}

		// read one sequence, choice == seq index
	public void doRead() throws IOException { 
		if (fIns==null) throw new FileNotFoundException(); //err= Readseq.eFileNotFound;
		
		//
		// FIXME:  choice == atseq, addit, adddoc -- need to respect read only requested records
		//
		
		if ( seqvec == null || seqvec.isEmpty() ) {
			// call sax parser here, store all seqs in seqVec
			doSaxRead();
			}
		else {
			// skip on; caller will fetch data from copyto() call
			}
		}

	
		// from Readseq.readTo() -- more efficient to do here
	public void readTo( BioseqWriterIface writer, int skipHeaderLines)  throws IOException 
	{
		//BioseqFormat wrformat= BioseqFormats.bioseqFormat( writer.formatID()); 
		//skipdocs= !wrformat.hasdoc();
	 	skipdocs= ! writer.wantsDocument();

		setSaxWriteTo( writer);
		//!!writer.writeHeader(); // or do via sax.startDocument() ?// do in caller!

		doSaxRead();
		
		//!! writer.writeTrailer(); // or do via sax.endDocument() ? // do in caller!
		setSaxWriteTo( null);
	}
				 
	public SeqFileInfo readOne(  int whichEntry) throws IOException
	{
		this.resetSeq();
		this.setChoice( whichEntry); // endOfFile depends on whichEntry == choice
    if ( this.endOfFile() ) return null; 
		this.doRead(); 
		
		SeqFileInfo si= new SeqFileInfo();
		this.copyto( si);
		si.nseq= this.getNseq(); // or is this atseq?
		si.checkSeqID();
  	return si;	
	}

	public boolean endOfFile() { 
		if (seqvec!=null) {
			return (choice > seqvec.size());
			}
		return fEof; 
		}

	private void setReaderBuf(Reader ins) {
		// need to reset SAX reader !?
  	//? saxhandler.setCharacterStream (fIns);
  	if (seqvec != null) seqvec.removeAllElements();
		}			
		
	public void reset() 
	{
		seqlen= seqlencount= err= atseq= 0;
		//ungetline= allDone= false;
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

	public void skipPastHeader(int skiplines) 
	{
		// not for xml parser
	}

	public void copyto( SeqFileInfo si) 
	{
		if (si==null) return;
		try {
			SeqFileInfo sat= (SeqFileInfo) seqvec.elementAt(choice-1);
			sat.copyto(si);
			//? remove element/storage after copyto() ??
			} 
		catch (Exception e) { si.err= -2; } //?
	}

	protected void storeSeq() throws IOException
	{
		// for SAX parser, save all seqs as parsed
		if (seqlen>0) {  
			atseq++; nseq++; //?
			Bioseq bseq= new Bioseq( seqbytes, 0, seqlen);
			SeqFileInfo si= new SeqFileInfo( bseq, 0, seqlen );
			si.atseq= atseq; //??<< counted to current sequence in file
			si.nseq= nseq;
			si.seqid= seqid;
			si.err= err;
			si.seqdoc= xmlseqdoc;
	 		if (Debug.isOn || verbose) 
			 message("saxread "+si.nseq+" id=" + si.seqid + " seqlen=" + si.seqlen);
			
			if (seqvec==null) seqvec= new FastVector();
			if (bioseqwriter!=null)  {				
				if (bioseqwriter.setSeq( si)) bioseqwriter.writeSeqRecord();

					//? add dummy si to seqvec for counting?
				SeqFileInfo sidum= new SeqFileInfo();
				sidum.seqid= si.seqid;
				sidum.atseq= si.atseq;
				seqvec.addElement( sidum);
				}
			else {
				seqvec.addElement( si);
				}
			}
	}


	public void setSeqName( String name) { 
			//? need this still?
		int i;
		seqid= name;
		seqid= seqid.trim();
		i= seqid.indexOf(' ');
		if (i<=0) i= seqid.length();
		if (i>30) i= 30;
		idword= seqid.substring(0,i).toString();
		}


		//
		// SAX reader parts
		//

	/* interface BioseqDocVals
	protected final static int 
		kBeforeFeatures = 0,  kAtFeatureHeader= 1, kInFeatures = 2, kAfterFeatures= 3,
		kField = 1, kSubfield = 2, kContinue = 3, 
		kFeatField= 4, kFeatCont = 5, kFeatWrap= 6;
	*/
	
	protected int inElVal, inFeatures, parselevel, docfieldlevel;
	protected Integer inEl;
	protected String inTagName;
	protected boolean inElAppend;
	protected FastStack inElStack= new FastStack();
	protected StringBuffer inFeatureKey, inFeatureVal; //, inFeatureLoc;
	protected int featlevel;

	class Pair {
		Integer val; String name;
		Pair(String nm, Integer v) { name= nm; val= v; }
		}
		
		
	//public static String saxParserClass = "com.ibm.xml.parsers.SAXParser";  
	boolean continueOnError= true;

	protected void doSaxRead() throws IOException 
	{
		try {
		//String className = XmlSeqFormat.saxParserClass; 
			//System.getProperty ("org.xml.sax.parser", saxParserClass);
	 	Parser parser;
	 	try { parser= ParserFactory.makeParser( XmlSeqFormat.saxParserClass); }
	 	catch (Exception ep) { parser= ParserFactory.makeParser(); }
	 	if (parser==null) {
	 		throw new IOException("Can't instantiate an XML parser"); 
	 		}
    parser.setDocumentHandler(this);
    parser.setDTDHandler(this);
    parser.setErrorHandler(this);
		//if (parser instanceof XMLParser) 
		//	((XMLParser)parser).setContinueAfterFatalError(continueOnError);
   	InputSource inputSource= new InputSource( fIns);
    parser.parse( inputSource );
    }
    catch (Exception e) { 
    	e.printStackTrace();
    	throw new IOException(e.getMessage()); 
    	}
	}

	protected BioseqWriterIface bioseqwriter;
	
	protected void setSaxWriteTo(BioseqWriterIface writer)
	{
		this.bioseqwriter= writer;
	}
		
	protected void initSaxDoc() {
		atseq= nseq= 0;
		inFeatures= kBeforeFeatures;
		inElVal= BioseqDocVals.kUnknown;
		inEl= null;
		inTagName= null;
		inElAppend= false;
		inElStack.removeAllElements();
		inFeatureKey= new StringBuffer();
		inFeatureVal= new StringBuffer();  
		resetSaxSeq();
		}
		
	protected void resetSaxSeq() {
		resetSeq();
		xmlseqdoc= new XmlDoc(); // new one of me
		xmlseqdoc.setSkipDocs( skipdocs);
		}
		
	public void resetSeq() {
		seqlen= seqlencount= 0; 
		//allDone= false;
		idword= SeqFileInfo.gBlankSeqid;
		seqid= SeqFileInfo.gBlankSeqid;
		//maxseqlen= 0; seq= null; //? leave storage for next and copy prior?
		}

	protected void addseq( char[] b, int offset, int nb) 
	{
	  //if (!addit) return;
	  nb += offset;
	  for (int i=offset; i<nb; i++) { 
	  	int c= testbase( b[i]);
	    if (c > 0) {
	      if (seqlen >= maxseqlen) if (!expand()) return;
		   	seqbytes[seqlen++]= (byte) c;  
	      }
	    }
	}
	
	protected boolean expand() 
	{
  	maxseqlen += readChunkSize;
		if (seqbytes==null) seqbytes= new byte[maxseqlen];
		else {
			byte[] tb= new byte[maxseqlen];
			System.arraycopy( seqbytes, 0, tb, 0, seqlen);
			seqbytes= tb;
			}
  	return (seqbytes!=null);
	}



	  //
	  // SAX HandlerBase reader interface
	  //
	  

	public void error (SAXParseException e) throws SAXException
  { errorOut("error: ", e);  }

  public void fatalError (SAXParseException e) throws SAXException
  { errorOut("fatalError: ",e); }
  
  public void warning (SAXParseException e) throws SAXException
  { errorOut("warning: ",e); }

	protected void errorOut (String kind, SAXParseException e) {
		//String id= e.getPublicId(); // which ?
		err++;
		if (showErrors) {
			String id= e.getSystemId();
			if (id!=null) {
				int at= id.lastIndexOf('/');
				if (at>0) id= id.substring(at+1);
				}
			message( id + ":" + e.getLineNumber() + ":"+ e.getColumnNumber() + " ");
			message( kind + e.getMessage());
			}
  }


	public void startDocument() throws SAXException {
		try {
		if (xmlseqdoc==null || nseq>0) initSaxDoc();
		//! if (bioseqwriter!=null) bioseqwriter.writeHeader(); // do in caller!
		} catch (Exception e) { throw new SAXException(e); }
    }  

  public void endDocument()  throws SAXException {
		fEof= true;
		//try {
		//! if (bioseqwriter!=null) bioseqwriter.writeTrailer();// do in caller!
		//} catch (Exception e) { throw new SAXException(e); }
  	}

	
  public void startElement(String name, AttributeList atts)   throws SAXException 
  {
  	try {
  	int lastel= inElVal;
  	if (inEl!=null) inElStack.push( new Pair(inTagName, inEl));
  	inTagName= name;
  	inElAppend= false;
		parselevel++;
  	
		inEl= xmlseqdoc.getBiodocInteger(name); 
		if (inEl!=null) inElVal= inEl.intValue();
		else inElVal= BioseqDocVals.kUnknown;

		switch (inElVal) {
			//case BioseqDocVals.kUnknown:
			//case BioseqDocVals.kBlank:
			case BioseqDocVals.kBioseqSet: 
			case BioseqDocVals.kBioseq: 
			case BioseqDocVals.kBioseqDoc: 
				xmlseqdoc.addDocField( inTagName, "", inElVal, kField, false);  
				docfieldlevel= parselevel+1;
				break; // these should have no char data

			case BioseqDocVals.kFeatureTable: 
				inFeatures= kInFeatures; //kAtFeatureHeader;
				inFeatureVal.setLength(0);  inFeatureKey.setLength(0); 
				xmlseqdoc.addDocField( inTagName, "", inElVal, kField, false);  
				break;
				
			case BioseqDocVals.kFeatureItem: 
				//? startFeature ? - need key, val from chars()
				break;

			case BioseqDocVals.kFeatureNote: 
					//  will get notes before closing kFeatureItem - are we sure it is kFeatureItem?
				if (inFeatureKey.length()>0 && lastel == BioseqDocVals.kFeatureItem) {
					xmlseqdoc.startFeature( BioseqDocVals.kFeatureItem, inFeatureKey.toString(), inFeatureVal.toString());
					inFeatureVal.setLength(0);  inFeatureKey.setLength(0); 
					}
				break;

			case BioseqDocVals.kFeatureLocation: 
			case BioseqDocVals.kFeatureValue: 
				if (inFeatureVal.length()>0) ; //? error?
				inFeatureVal.setLength(0);
				break;

			}
    } catch (Exception e) { throw new SAXException(e); }
	} 


 public void endElement(String name) throws SAXException {
		try {
		// handle any finishing for this named tag
		// at kBioseq end need to store current seqFileInfo and reset for more...
		
		switch (inElVal) {
			case BioseqDocVals.kBioseq: 
				storeSeq(); resetSaxSeq(); 
				break;
				
			case BioseqDocVals.kFeatureTable: 
				inFeatures= kAfterFeatures;
				break;
			
			case BioseqDocVals.kFeatureItem: 
				//if (inFeatureKey.length()>0) {
					xmlseqdoc.endFeature( BioseqDocVals.kFeatureItem, inFeatureKey.toString(), inFeatureVal.toString());
					inFeatureVal.setLength(0);  inFeatureKey.setLength(0); 
					//}
				break;

			case BioseqDocVals.kFeatureNote: 
				//if (inFeatureKey.length()>0) {
					xmlseqdoc.endFeature( BioseqDocVals.kFeatureNote, inFeatureKey.toString(), inFeatureVal.toString()); 
					inFeatureVal.setLength(0);  inFeatureKey.setLength(0); 
					//}
				break;

			case BioseqDocVals.kFeatureLocation: 
			case BioseqDocVals.kFeatureValue: 
				break;
		}

		parselevel--;
  	if (!inElStack.empty()) {
  		Pair p= (Pair) inElStack.pop();
  		inEl= p.val; inTagName= p.name;
			if (inEl!=null) inElVal= inEl.intValue();
			else inElVal= BioseqDocVals.kUnknown;
  		//inTagName= XmlDoc.getXMLFieldName( inEl); // need for chars - save in stack?
  		inElAppend= true;
			}
			
 		} catch (Exception e) { throw new SAXException(e); }
	} 
	


	protected String charsToString(char[] val, int offset, int length) 
	{
		while (length>0 && val[offset+length-1] <= ' ') length--;
		int i= 0; 
		while (i<length && val[offset+i] <= ' ') i++;
		if (i>0) { offset += i; length -= i; }
		if (length <= 0) return null;
		String sval= new String(val, offset, length);
		sval= sval.replace('\n',' '); //? .replace('\r',' '); //?
		//sval= sval.trim(); // getting whitespace - drop?   
		//if (sval.length()==0) return null; else //! must return null for whitespace for inFeature strings
		return sval;
		}

	protected StringBuffer charsToString( StringBuffer oldval, char[] val, int offset, int length) {
		String snew= charsToString( val, offset, length);
		if (oldval!=null) {
			if (snew==null) return oldval;
			//if (oldval.length()>0) oldval.append(' ');  
			oldval.append( snew);
			return oldval;  
			}
		else 
			return new StringBuffer(snew);
		}
		
	
  public void characters( char ch[], int start, int length)  throws SAXException
  {
			// for reader iface, see also writeCharacters
		try {
		if (! xmlseqdoc.keepField(inElVal) ) return; //?
		switch (inElVal) {
	
			case BioseqDocVals.kSeqdata: 
				addseq( ch, start, length); 
				break;
			
			case BioseqDocVals.kName:    
				{
				docfieldlevel= parselevel;
				String sval= charsToString(ch, start, length); //new String(val, start, length).trim();
				if ( sval!=null ) {
					setSeqName(sval); //seqid= sval;
					xmlseqdoc.addDocField( inTagName, sval, inElVal, kField, inElAppend); 
					inElAppend= true;  
					}
				break;
				}
				
			//case BioseqDocVals.kUnknown: //? save this anyway?
			case BioseqDocVals.kBioseqSet: 
			case BioseqDocVals.kBioseq: 
			case BioseqDocVals.kBioseqDoc: 
				// do in startel //xmlseqdoc.addDocField( inTagName, "", inElVal, kField, false);  
				docfieldlevel= parselevel+1;
			case BioseqDocVals.kBlank:
			case BioseqDocVals.kFeatureTable: 
				break; // these should have no char data

			default: //? is this safe?
				String sval= charsToString(ch, start, length); //new String(val, start, length).trim();
				if (sval!=null || !inElAppend) {
					// do we want to store any fields w/ null values?
					int fldlevel= (parselevel>docfieldlevel) ? kSubfield : kField;
					xmlseqdoc.addDocField( inTagName, sval, inElVal, fldlevel, inElAppend);  
					inElAppend= true;  
					}
				break;

					// add these new ft tags for xml
			/*case kFeatureKey: // ft tag -- move into chars of kFeatureItem,kFeatureNote
				inFeatureKey=  charsToString(inFeatureKey,ch, start, length);  
				break;*/
				
			case BioseqDocVals.kFeatureValue:	// value
			case BioseqDocVals.kFeatureLocation: // can use inFeatureVal?
				inFeatureVal= charsToString( inFeatureVal, ch, start, length);   
				break;

			/*case BioseqDocVals.kFeatureElement: //!? kFeatureElement == kFeatureNote
				break;*/
								
			case BioseqDocVals.kFeatureItem:  
				inFeatureKey=  charsToString( inFeatureKey, ch, start, length);  
				//xmlseqdoc.addFeature( inTagName, charsToString(ch, start, length), kFeatField, inElAppend);
				//inElAppend= true; //? need this esp - "&lt;" are sent sep from rest
				break;
				
			case BioseqDocVals.kFeatureNote:	 
				inFeatureKey=  charsToString( inFeatureKey, ch, start, length);  
				//xmlseqdoc.addFeature( inTagName, charsToString(ch, start, length), kFeatCont, inElAppend);
				//inElAppend= true; 
				break;

			}
    } catch (Exception e) { throw new SAXException(e); }
	}  



  public void ignorableWhitespace(char ch[], int start, int length) throws SAXException 
 		{ }  //? characters(ch, start, length);


			// unused HandlerBase parts

	public void processingInstruction(String target, String data)  throws SAXException 
  { } 
 	public InputSource resolveEntity (String publicId, String systemId)  throws SAXException
  { return null; }
  public void notationDecl (String name, String publicId, String systemId)
  { }
  public void unparsedEntityDecl (String name, String publicId, String systemId, String notationName)
  { }
  public void setDocumentLocator (Locator locator)
  { }


}


