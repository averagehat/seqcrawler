// Readseq.java
// d.g.gilbert, 1990-1999+
/*

			            PUBLIC DOMAIN NOTICE: 
This software is freely available to the public for use. The
author, Don Gilbert, of Readseq and the Java package
'iubio.readseq' does not place any restriction on its use or
reproduction. Developers are encourged to incorporate parts in
their programs. I would appreciate being cited in any work or
product based on this material. This software is provided without
warranty of any kind.

*/


package iubio.readseq;


import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import java.net.URL;

import flybase.Debug;
import flybase.OpenString;
import flybase.FastVector;
import flybase.FastProperties;
import flybase.Native;
import flybase.AppResources;
import flybase.Environ;

import iubio.bioseq.Bioseq;
import iubio.bioseq.SeqRange;
import iubio.bioseq.SeqInfo;

		 
	
/**
 * Readseq is public wrapper for
 * classes of BioseqReader & BioseqWriter ancestry	
 * @author  Don Gilbert
 * @version 	July 1999

 <pre>
Sample program for input to output conversion
 
import java.io.*;
import iubio.readseq.*;
public class testrseq {
	public static main( String[] args) {
		try {
		int outformat= BioseqFormats.formatFromName("fasta");
		BioseqWriterIface seqwriter= BioseqFormats.newWriter(outformat);  
		seqwriter.setOutput( System.out);
		seqwriter.writeHeader();  
		Readseq	rd= new Readseq();
		for (int i=0; i &lt; args.length; i++) {
			rd.setInputObject( args[i] );
			if ( rd.isKnownFormat() && rd.readInit() )  
				rd.readTo( seqwriter); 
			}
		seqwriter.writeTrailer();
		}
		catch (Exception ex) { ex.printStackTrace(); }
	}
}
	</pre>
 */
 
public class Readseq 
	implements Enumeration
	//, ReadseqIntf
{
	public static String version = "Readseq version 2.1.8 (25 June 2002)";
	/** special flag for readlist() */
	public static String kInputStringKey = "indata="; 

	public boolean verbose, verboseClassic;
	
	public Readseq() { 
		this( BioseqFormats.kNoformat);
		}
		
	public Readseq(int format) { 
		setFormat(format);
		verbose= (Debug.isOn || BioseqReader.verbose);
		}
			
	public Reader getInput()  { 
		return fIns; //? or reader.getInput() 
		}

		/** input setters - @see readlist( FastVector inlist)  */
	public void setInput( File f) throws IOException { close(); fIns= fRdIns= new RsInputFile( f); 	}
	public void setInput( Reader in)  throws IOException { close(); fIns= fRdIns=  new RsInputReader( in); }
	public void setInput( InputStream in)  throws IOException { close(); fIns=  fRdIns= new RsInputStream( in); }
	public void setInput( URL url) throws IOException { close(); fIns= fRdIns= new RsInputUrl( url); 	}
	public void setInput( String s)  throws IOException { close(); fIns= fRdIns= new RsInputString( s); }
	public void setInput( OpenString s)  throws IOException { close(); fIns= fRdIns= new RsInputOpenString( s); }
	public void setInput( byte[] b) throws IOException  { close(); fIns= fRdIns= new RsInputBytes( b); } 
	public void setInput( char[] c)  throws IOException { close(); fIns= fRdIns= new RsInputChars( c); } 
		//? can we do: 
	//public void setInput(Object inob) { setInputObject(inob); }
	
	public String setInputObject( Object inob)  throws IOException 
	{ 
		if (inob instanceof String) inob= checkInString( (String)inob);
		return setInputObjectChecked( inob);
	}
	
	public String setInputObjectChecked( Object inob)  throws IOException 
	{
		     if (inob instanceof File) { setInput( (File) inob);  return inob.toString(); 	}
		else if (inob instanceof URL) { setInput( (URL) inob);  return inob.toString(); }
		else if (inob instanceof InputStream) { setInput( (InputStream) inob);  return "InputStream"; 	}
		else if (inob instanceof Reader) { setInput( (Reader) inob); return "Reader"; }
		else if (inob instanceof String) { setInput( (String) inob); return "String"; }
		else if (inob instanceof OpenString) { setInput( (OpenString) inob); 	return "OpenString"; }
		else if (inob instanceof char[]) { setInput( (char[]) inob);  return "char[]"; }
		else if (inob instanceof byte[]) { setInput( (byte[]) inob); return "byte[]"; }
		else if (inob instanceof Enumeration) { 
			readlist((Enumeration)inob); //? tricky - recursive call okay?
			return null; 
			}
		else if (inob instanceof Integer) { // my null object
			return null; 
			}
		else if (inob != null) {
			message("Unreadable input object " + inob.getClass().getName());
			return null; 
			}
		else return null;
	}

	// some util wrappers
	
	public static String tempFolder() {
		return Environ.gEnv.get("tempdir", Native.tempFolder());
		}
	public static String tempFilename() {
		return Native.tempFilename("readseq-",".tmp");// + Integer.toHexString( new Random().nextInt()) + ".temp");
		}
  public static File tempFile() { 
		return new File(tempFolder(), tempFilename());
		}
		
	protected void message(String s) {
		BioseqReader.message(s);
		//System.err.println(s);
		}
		
	public void close() throws IOException { 
		if (fIns!=null) fIns.close(); 
		// finialize fRdIns to delete temp file?
		if (fRdIns!=null) try { fRdIns.finalize(); } catch (Throwable th) {}
		fIns= null;  
		}
	
	public void setFormatTestor(Testseq testor) { formatTestor= testor; }

  public final boolean isMydata() { return isKnownFormat(); }

	protected int forceFormatId;
	public void setInputFormat(int formatID) {
		forceFormatId= formatID;
		}
		
	public boolean isKnownFormat() 
	{
		si= new SeqFileInfo(); //?
		int fmt= BioseqFormats.kUnknown;
		if (forceFormatId > 0) {
			fmt= forceFormatId;
			si.format= fmt;
			si.skiplines= 0;	
			forceFormatId= 0; // zero it for next call ?
			}
		else {	
			if (formatTestor==null) formatTestor= new Testseq(); //this
			fmt= formatTestor.testFormat( fIns, si);
			fIns= formatTestor.getPossibleNewInputReader();
			//fFiltIns= formatTestor.getFilterReader();
			}
			
		if (verbose)
			message("isKnownFormat format=" + fmt +":"+ BioseqFormats.formatName(fmt));
		if (fmt <= BioseqFormats.kUnknown) return false;
		else {
			setFormat( fmt);  
			return true;
			}
	}




			//? not same as isKnownFormat()
	public boolean canread() {
		if (reader!=null) return true; //reader.canread();
		else if (format>0) return BioseqFormats.canread(format);
		else return false;
		}
		
	public final SeqFileInfo getInfo() { return si; }

	public final int getFormat() { return format; }

	public void setFormat(int format) {
		this.format= format;  
		former= BioseqFormats.bioseqFormat(format);  //BioseqFormats.bioseqFormat(format)
		if (reader!=null && reader.formatID() != format) reader= null;
		}

  public String getFormatName() { 
		if (reader!=null) return BioseqFormats.formatName( reader.formatID() ); // isn't reader format always == foramt?
		else if (format>0) return BioseqFormats.formatName( getFormat());
		else return "";
		}
	
	public BioseqReaderIface getReader() { return reader; }
	public BioseqFormat  getBioseqFormat() { return former; }
	
	public boolean eof() {
		try { 
			return ( (si!=null && si.err!=0 )
				|| fIns==null 
				|| !fIns.ready() // si.ins.available()==0
			//? || (reader==null ? false : reader.endOfFile())
				); 
			} 
		catch (IOException ex) { return true; }
		}




	public URL checkUrl(String s) {
		if (s.lastIndexOf(":/", 30)>0) {
		  try {
			URL u;
			if (s.startsWith("systemresource:/") && s.indexOf("/+/")<0)  { 
				// u == systemresource:/FILE9/+/rez/embl.seq   --  /+/ is sun url key
				u= AppResources.global.getUrl( s.substring("systemresource:/".length()));
				Debug.println(" sysrez -> "+ u);
				if (u!=null) return u;
        }
			 u= new URL(s);  // throws
			 Debug.println(" url -> "+ u);
			 return u;
			 } 
			catch (Exception ue) { }
			}
		return null;
		}


	public final Object checkInString( String ins) {
		return checkInString(ins, kInputStringKey);
		}
		
	public Object checkInString( String ins, String stringDataKey)
	{
		if (Debug.isOn) Debug.print("checkInString  '" + ins.substring(0,Math.min(80,ins.length())));
		
		if (ins.startsWith( stringDataKey)) {
			ins= ins.substring( stringDataKey.length()); //? .trim()
			URL url= checkUrl(ins);
			if (url!=null) {
				Debug.println("' is url.");
				return url;  
				}
			else {
				Debug.println("' is data.");
				return ins; // the data w/o stringDataKey
				}
			}
		else {
			URL url= checkUrl(ins);
			if (url!=null) {
				Debug.println("' is url.");
				return url; 
				}
			else {
				File file;
				if (indir!=null) file= new File(indir,ins);
				else file= new File(ins); // need indir ?
				if (file.exists() && file.isFile() && file.canRead()) {
					Debug.println("' is file.");
					return file; 			
					}
				else {
					Debug.println("' is unknown/unreadable object.");
					message("Unknown or unreadable data: " + ins);
					return new Integer(0); //? or do what - don't want to treat it like String data				
					}
				}
			}
	}
				
	public void checkInList( FastVector inlist, String stringDataKey)
	{
		if (inlist!=null)
		for (int i= 0; i<inlist.size(); i++)  {
			Object el= inlist.elementAt(i);
			if (el instanceof String) {
				Object newel= checkInString( (String)el, stringDataKey);
				if (newel != null) inlist.setElementAt( newel, i);
				}
			}
	}

	protected File indir;
	public void setInDirectory(File indir) { this.indir= indir; }

			/**
			* Read a FastVector list of inputs:  File, URL, InputStream, String of (File,URL,Data)
			*  -- moved here from readseqapp for handy usage
			*/
			
	public final void readlist( FastVector inlist) {
		readlist( inlist, kInputStringKey);
		}
	public final void readlist( Enumeration inlist) {
		readlist( inlist, kInputStringKey);
		}
	public void readlist( FastVector inlist, String stringDataKey) {
		readlist( inlist.elements(), stringDataKey);
		}
		
	public void readlist( Enumeration inlist, String stringDataKey)
	{
		if (inlist==null) return;
		int forceInformat= forceFormatId; // preserve and use for all inlist ?
		
		//? no value - do per object?
		// checkInList(inlist, stringDataKey);
		
		//for (int innum= 0; innum < inlist.size(); innum++) 
				//Object inob = inlist.elementAt(innum); 
		while ( inlist.hasMoreElements() )
			try { // try inside or outside list loop?
				Object inob= inlist.nextElement();
				//String inname= setInputObjectChecked( inob);
				String inname= setInputObject( inob);
				if (inname == null) continue;		
						
				if (verbose) message("Reading from "+inname);
				
				if (forceInformat > 0)  this.setInputFormat(forceInformat);
				if (!this.isKnownFormat()) 
					message("Unknown biosequence format for input " + inname);
				else {
					if (!this.readInit())  
						message("Error initializing drawseq for input " + inname);
					else
						while (this.canReadMore()) this.readNext(); 
					}
					
				this.close();
				}
			catch (IOException e) { 
				message("Readseq.list error: "+ e.getMessage());
				e.printStackTrace(); 
				}
	}


		//
		// Data accessors -- want all these?
		//
		
	public final SeqFileInfo nextSeq() { 
		// should this seqfilevec.removeElementAt() ??
		if (moreresults()) return (SeqFileInfo) seqfilevec.elementAt(fAt++); 
		else return null; 
		}
	public SeqFileInfo[] allSeqs() { 	
		SeqFileInfo[] sfi= new SeqFileInfo[ seqfilevec.size() ];
		seqfilevec.copyInto( sfi);
		return sfi;
		}
	public final Object result() { if (moreresults()) return seqfilevec.elementAt(fAt++); else return null; }
	public final boolean moreresults() 	{ return (fAt < seqfilevec.size()); }
	public final int atresult() 				{ return fAt;  }
	public final int nresults() 				{ return seqfilevec.size();  }
	public final Vector allresults() 		{ return seqfilevec;  }
	public final void restartresults() 	{ fAt= 0; }
	public final void removeresults()   { fAt= 0; seqfilevec.removeAllElements(); }
	 
	 	//
		// Enumeration iface
		//
		
	public boolean hasMoreElements() { 
		if (moreresults()) return true;
		else return canReadMore(); 
		}
		
	public Object nextElement() { 
		if (moreresults()) return this.result(); 
		try { if (readNext()) return this.result();  }
		catch (IOException e) {}
		return null;
		}


		//
		// reading methods
		//
		
	protected boolean didinit;

	public void initIfNeeded( String defname) {
		if (!didinit) readInit(defname);
		}
		
	public final boolean readInit()  { return readInit(SeqFileInfo.gBlankSeqid); }
		
	public boolean readInit( String defname) //? open(name)
	{
		if (reader==null) reader= BioseqFormats.newReader(format);  
		if (reader==null) return false; //? throw IOException ?
		if (former.interleaved()) fRdIns.makeRewindable();
		
		reader.setInput(fIns); 
		reader.reset(); //rewind();
		
		if (reader instanceof PlainSeqReader) 
			((PlainSeqReader)reader).setInputFile( fRdIns.getFile() ); // just for VeryRaw? or others?

		if (reader instanceof BioseqReader) {
			// only useful if just 1 long seq in file -- only Plain/Raw format input? unless we count input entries first
			int  clen= ((BioseqReader)reader).getReadChunkSize();
			long flen= fRdIns.guessLength();
			if ( (flen - clen) > 50000) {
				if (former instanceof PlainSeqFormat) clen= (int) Math.min(500000,flen);
				else clen= (int) Math.max(clen, Math.min(500000, flen / 10));
				((BioseqReader)reader).setReadChunkSize(clen);
				}
			}

		if (defname==null) defname= SeqFileInfo.gBlankSeqid;
		seqDefname= SeqFileInfo.cleanSeqID(defname);
		saveskip= si.skiplines;
		whichEntry= 0;
		didinit= true;
		//if (!canread()) return false; else 
		return true;
	}
	

	
 	void verboseClassic(SeqFileInfo si) 
 	{
	 //   fprintf( stderr, "Sequence %d, length= %d, checksum= %X, format= %s, id= %s\n",
   //        whichSeq, seqlen, checksum, formatstr(format), seqidptr);
   	StringBuffer sb= new StringBuffer();
   	sb.append("Sequence ");		sb.append( whichEntry);
   	sb.append(", length= "); 	sb.append( si.seqlen);
   	
   	long checksum= 0;
if (true) try {
   	//!? need to do this just for verboseClassic ? - store checksum somewhere for writer to use
		BioseqRecord bs= new BioseqRecord(si);
		SeqInfo sst= bs.getseq().getSeqStats();
		checksum= sst.getChecksum();
			// BioseqWriter.CRC32checksum( si.seq, si.offset,  si.seqlen);
} catch (Exception ex) {} // XML returns si.seqlen == 0 ?? mar02 -- FIXME
		
   	sb.append(", checksum= "); 	
   		sb.append(  Long.toHexString(checksum).toUpperCase());
   			// Fmt.fmt( checksum, 0, Fmt.HX).toUpperCase() 
   	sb.append(", format= "); 	
   		sb.append( former.formatID()); sb.append(". "); 
   		sb.append( former.formatName());
   	sb.append(", id= "); 	sb.append(si.seqid);
   	message( sb.toString() );
	}


	public boolean canReadMore() {
		if (reader==null) return false; else 
		if (former.interleaved()) return ((whichEntry==0) ? true : (whichEntry < si.nseq));  else //? 
		if (!reader.endOfFile()) return true; //? need for saxreader --> fIns sets eof()
		return !eof();   
		}
	
	public SeqFileInfo readAt(int atEntry) throws IOException
	{
		if (reader==null) throw new ReadseqException("Null BioseqReader"); 
		
		//if (former.interleaved()) { reader.reset(); si.skiplines= saveskip; } //??? yes or no - reset rewinds Input
			//^^ InterleavedSeqReader does the reset/skiplines fix
			
		si= readSeq( si, atEntry); 
		if (si != null ) {
			if ( !si.hasid()) si.seqid= seqDefname;
			
			//? patch here for extractRange ? set si.offset, si.seqlen
			// this still reads entire sequence - need to fix to read only extractRange
			
			extractOrigin= 0;
			if (extractRange!=null && !extractRange.isEmpty()) {
				int start  = extractRange.start() + extractRange.origin();
				int nbases = extractRange.nbases();
				if (si.length() >= nbases && si.offset()+si.length() > start) {
					si.setoffset(start);
					si.setlength(nbases);
					extractOrigin= start; // need to set origin for seqwriter !! should put in si.
					} 
				}
				
			}
		return si;   
	}

	public boolean readNext() throws IOException
	{
		//if (former.interleaved()) { reader.reset();  si.skiplines= saveskip; }
		//si= readSeq( si, ++whichEntry); 
   	//if (!si.hasid()) si.seqid= seqDefname;
		////if (si.err==0 && si.seqlen>0) 
		si= readAt(++whichEntry);
		if (si!=null) {
			if (si.hasseq() || si.hasdoc()) 
				seqfilevec.addElement( si); 
			}
 		return moreresults();	
	}


	protected Hashtable exfeatures;
	protected SeqRange featSubrange, extractRange;
	protected boolean wantSelectedFeats;
	protected int extractOrigin;
	
	public void setFeatureExtraction(Hashtable featurelist, SeqRange featSubrange) {
		this.exfeatures= featurelist; this.featSubrange= featSubrange;
		wantSelectedFeats= (exfeatures!=null && !exfeatures.isEmpty());
		} 

	public void setExtractRange(  SeqRange exSubrange) {
		this.extractRange= exSubrange;
		} 
 		
	protected void writeSelectedFeatureRecords( SeqFileInfo si,
				BioseqWriterIface seqwriter, BioseqWriterIface docwriter )  throws IOException 
	{
		BioseqRecord bsrec = new BioseqRecord(si); 
		BioseqRecord bsfeat= new BioseqRecord();
		String seqid= bsrec.getID();
		bsfeat.setSeqID(seqid);  
 	 	BasicBioseqDoc featdoc= new BasicBioseqDoc(seqid);
 	 	
 	 	SeqRange maxRange= new SeqRange( bsrec.offset(), bsrec.length());
 	 	int maxStart= maxRange.start();
 	 	int maxEnd= maxRange.max();
 	 	
 	 	if (Debug.isOn) Debug.println("writeSelectedFeatureRecords for "+ bsrec);
 	 	if (bsrec.hasdoc()) {
 		 	featdoc.addDocField( bsrec.getdoc().findDocItem(BioseqDoc.kAccession, 0) );
 		 	featdoc.addDocField( bsrec.getdoc().findDocItem(BioseqDoc.kDate, 0) );
 		 	featdoc.addDocField( bsrec.getdoc().findDocItem(BioseqDoc.kDescription, 0) );
 	 		}
 	 	//if (Debug.isOn) Debug.println(" featdoc: "+ featdoc.getDocumentText());
 	 	
		FeatureItem[] fits= bsrec.findFeatures( exfeatures, extractRange);
		String fn= ""; Enumeration en= exfeatures.keys(); 
		while (en.hasMoreElements()) fn += (String) en.nextElement() + ", ";
 	 	//if (Debug.isOn) Debug.println(" features: "+ fn);
		if (fits==null) {
 			featdoc.addComment("No such features found: " + fn);
			bsfeat.set( null, featdoc, null);
			if (docwriter==null) docwriter= seqwriter;
			if (docwriter!=null && docwriter.setSeq( bsfeat)) docwriter.writeSeqRecord();
			}
		else {
	 		featdoc.addComment("Extracted features: " + fn);
	 		if (featSubrange!=null) featdoc.addComment("Extracted feature subrange: " + featSubrange);
	 		FeatureItem fit= null; 
	 		int k= 0, nex= 0;
			for ( ; k < fits.length; k++) 
			try {
	 	 		fit= fits[k];
				//Bioseq bseq= bsrec.extractFeatureBases( fits[k]);
	 	 		if (Debug.isOn && k<10) Debug.println(" feature ["+k+"]="+ fit);
				SeqRange featsr= (SeqRange) fit.getLocation();
				
				//Debug.print ("featrange, orig="+featsr);
				//Debug.println(" maxStart="+maxStart+" maxEnd="+maxEnd);
				if (featSubrange!=null) featsr= featsr.subrange(featSubrange);
				//Debug.println("featrange, subrange="+featsr+", featSubrange="+featSubrange);
						//^ need to peg featsr to available data range !
				if (maxStart > featsr.start() || maxEnd < featsr.max()) {
					//featsr = featsr.intersection0( maxRange); // can return null !
					int b= Math.max( maxStart, featsr.start());
					int e= Math.min( maxEnd, featsr.max());
					featsr= new SeqRange( b, e-b+1 );
					//Debug.println("featrange, pinned="+featsr);
					}
						
				Bioseq bseq= bsrec.extractBases(featsr); // problems here featsr == 0 ??
				//Debug.print("featrange, extracted="+featsr);
				//Debug.println("  bseq, extracted len="+bseq.length());

				int featorig= 0;  
				if (featsr.isComplement())  
					featorig= - (featsr.origin() + featsr.max()); //? so display counts up from compl.start
				else
					featorig= featsr.origin() + featsr.start();  			 
		 		
		 		String featid= bsfeat.getNextBlankID(seqid);  //? or use seqid+_featorig
				featdoc.replaceDocField( featdoc.kName, featid );
				bsfeat.setSeqID(featid);  //bsfeat.set() should do this from featdoc

  			//? featdoc.addSequenceStats( bseq);
	  		featdoc.addFeature( fit);  
	  				//!? Need to adjust orign of this feat to match extr range?
	  				//! mostly need to adjust origin of sequence
	  				
				featdoc.addFeature( featdoc.sExtractionFeature, featsr);
				featdoc.addFeatureNote("/note", 
					  "Location extracted from source. Feature locations are for source, not for this extraction."); 

				bsfeat.set( bseq, featdoc, null);

				if (docwriter!=null && docwriter.setSeq( bsfeat)) docwriter.writeSeqRecord();
				if (seqwriter!=null && seqwriter.setSeq( bsfeat)) {
					if (seqwriter instanceof BioseqWriter) {
						WriteseqOpts opts=  ((BioseqWriter) seqwriter).getOpts();
						opts.origin= featorig;
						//seqwriter.setOpts( opts); // not needed now - opts isn't copy
						}
					seqwriter.writeSeqRecord();
					}
	  		featdoc.features().removeAllElements();
				}
			catch ( Exception ex ) { 
	 	 		if (Debug.isOn) {
	 	 			Debug.println("Exception with feature ["+k+"]="+ fit);
	 	 			ex.printStackTrace(); // fillIn isn't helping
	 	 			}
				if (nex++>2) {
					ReadseqException rex= new ReadseqException(ex.getMessage()); 
					throw rex; //? or keep on w/ next feature ?
					}
				featdoc.features().removeAllElements();
				}
			}
	}

	public void writeSeqTo( SeqFileInfo si, BioseqWriterIface seqwriter, BioseqWriterIface docwriter )  
			throws IOException 
	{
		if (extractOrigin > 0 && seqwriter instanceof BioseqWriter) {
			WriteseqOpts opts= ((BioseqWriter) seqwriter).getOpts();
			opts.origin= extractOrigin;  
			}
		if (wantSelectedFeats && si.hasdoc()) 
			writeSelectedFeatureRecords( si, seqwriter, docwriter);
		else {
			if (docwriter!=null && docwriter.setSeq( si)) docwriter.writeSeqRecord();
			if (seqwriter!=null && seqwriter.setSeq( si)) seqwriter.writeSeqRecord();
			}
		//if (fWriteMask && seqwriter.setMask(si, si.gMaskName)) seqwriter.writeSeqRecord();
	}
	
		
  	// these are Readseq methods, not Writeseq
 	protected boolean fWriteMask= SeqFileInfo.gWriteMask;
	public void setWriteMask(boolean turnon) { fWriteMask= turnon; }
 	
	public boolean readTo( BioseqWriterIface writer)  throws IOException {
		return readTo( writer, false); }
		
	public boolean readToOld( BioseqWriterIface writer, boolean writeWrapper)  
		throws IOException {
		if (reader==null) throw new ReadseqException("Null BioseqReader"); 
		if (writeWrapper) writer.writeHeader();
		reader.readTo( writer, si.skiplines);
		if (writeWrapper) writer.writeTrailer();
		return true;
		}
 
	public boolean readTo( BioseqWriterIface seqwriter, boolean writeWrapper)  
		throws IOException 
	{				
		//? SeqFileInfo si; // this is global; used by canReadMore; others
		if (writeWrapper) seqwriter.writeHeader();
	 	
	 	if (reader instanceof BioseqReader)
	 		((BioseqReader)reader).setSkipDocs( ! (wantSelectedFeats || seqwriter.wantsDocument())); // ! FIXME
	 	
		// while (this.readNext()) { si= this.nextSeq(); ... }
		while (canReadMore() && (si= readAt(++whichEntry)) != null) 
		{ 
			writeSeqTo( si, seqwriter, null);
			/*
			if (extractOrigin > 0 && seqwriter instanceof BioseqWriter) {
				WriteseqOpts opts= ((BioseqWriter) seqwriter).getOpts();
				opts.origin= extractOrigin;  //!? need to reset opts.origin after write?
				}
			if (wantSelectedFeats && si.hasdoc()) 
				writeSelectedFeatureRecords( si, seqwriter, null);
			else if (seqwriter.setSeq( si)) seqwriter.writeSeqRecord();
			*/
		}
		if (writeWrapper) seqwriter.writeTrailer();
		return true;
	}
 
		
	public boolean readToPair( BioseqWriterIface seqwriter, BioseqWriterIface docwriter, 
				boolean writeWrapper)  throws IOException 
	{
		//? SeqFileInfo si; // this is global; used by canReadMore; others
		if (writeWrapper) { docwriter.writeHeader(); seqwriter.writeHeader(); }

	 	if (reader instanceof BioseqReader)
		 	((BioseqReader)reader).setSkipDocs( false); // ! FIXME

		// while (this.readNext()) { si= this.nextSeq(); ... }
		while (canReadMore() &&  (si= readAt(++whichEntry)) != null) 
		{
			writeSeqTo( si, seqwriter, docwriter);
			/*
			if (extractOrigin > 0 && seqwriter instanceof BioseqWriter) {
				WriteseqOpts opts= ((BioseqWriter) seqwriter).getOpts();
				opts.origin= extractOrigin;  
				}
			if (wantSelectedFeats && si.hasdoc()) 
				writeSelectedFeatureRecords( si, seqwriter, docwriter);
			else {
				if (docwriter.setSeq( si)) docwriter.writeSeqRecord();
				if (seqwriter.setSeq( si)) seqwriter.writeSeqRecord();
				}
			//if (fWriteMask && seqwriter.setMask(si, si.gMaskName)) seqwriter.writeSeqRecord();
			//removeresults(); //? free seq?
			*/
		}
		if (writeWrapper) { docwriter.writeTrailer(); seqwriter.writeTrailer(); }
		return true;
	}


	public void list( Writer out) 
	{
		try {
			for (boolean more= true; more; ) {
				si= readSeq( si, BioseqReader.kListSequences); //? catch (EOFException eox) {}
				more= (si!=null && (si.hasseq() || si.hasdoc()));
				if (more) out.write( ((Bioseq)si.seq).toChars()); 
				} 
			}
		catch (IOException ex) {
			Debug.println("list error"); ex.printStackTrace();
			}
	}


	
		//
		// implementation =============================	
		//
		
	protected Testseq formatTestor;
	protected Vector seqfilevec= new Vector();
	protected int fAt;

  protected BioseqReaderIface reader;
	protected BioseqFormat former;  
	protected Reader fIns;
	protected RsInput fRdIns; // need separate from fIns in cases where fIns is chained t new Reader
	protected SeqFileInfo si= new SeqFileInfo();
	protected int format;
	protected int saveskip, whichEntry= 1;
	protected String seqDefname;
	

	protected SeqFileInfo readSeq( SeqFileInfo sin, int atEntry) throws IOException
	{
		reader.skipPastHeader( sin.skiplines); sin.skiplines= 0; //! bug alert, must call even if skiplines==0
		
    whichEntry= atEntry;
    
		SeqFileInfo sout;
		sout= reader.readOne( whichEntry);
		
				//? do we need a clone of sin ?
		//? if (sout==null) return null;
		//sout= (SeqFileInfo) sin.clone(); // new object for this sequence
		//reader.copyto( sout); //?!????
		//sout.nseq= reader.getNseq();
		
		if (verboseClassic && sout!=null) 
			verboseClassic(sout);
		else if (verbose && sout!=null) 
			message("read "+atEntry+", id=" + sout.seqid + " seqlen=" + sout.seqlen); 
		return sout;
	}
	
/*	 
		// this is about same as SeqFileInfo sfi= BioseqReader.readOne( atEntry);
	protected SeqFileInfo readSeqOld(SeqFileInfo sin, int atEntry) throws IOException
	{
		if (reader==null) throw new ReadseqException("Null BioseqReader");  

		reader.resetSeq();
		reader.skipPastHeader( sin.skiplines); //! bug alert, must call even if skiplines==0
		sin.skiplines= 0; 

		SeqFileInfo sout= (SeqFileInfo) sin.clone(); // new object for this sequence
    if ( reader.endOfFile() ) { sout.err= -1; return sout; }

    whichEntry= atEntry;
		reader.setChoice(whichEntry); //?? set sequence item in stream?
		reader.doRead(); //!? have to catch EOFException and store current data !?
		
		reader.copyto(sout);
		sout.nseq= reader.getNseq();
		if (verbose) message("read "+atEntry+", id=" + sout.seqid + " seqlen=" + sout.seqlen);
		return sout;
	}
*/ 

};







/**
 * Rewindable wrapper classes for input data <p>
 * Make input stream rewind/reset-able when needed
 */
abstract class RsInput extends Reader
{
	public final static long kUnknownLength = -1;
	protected Reader in;
	protected InputStream inbytes; // maybe null, but some readers (ScfReader) need!

	protected RsInput() throws IOException { super(); }
	public void makeRewindable() {} // for readseq
	public InputStream getByteStream() { return inbytes; } // for readseq
	
		// FilterReader equivalents
	public int read() throws IOException { return in.read(); }
	public long skip(long n) throws IOException { return in.skip(n); }
	public long guessLength()   { return kUnknownLength; } // don't know
	public File getFile()   { return null; } 
	public boolean ready() throws IOException { return in.ready(); }
	public boolean markSupported() { return in.markSupported(); }
	public void reset() throws IOException { in.reset(); }
	public void close() throws IOException { in.close(); }
	public int read(char cbuf[], int off, int len) throws IOException {
		return in.read(cbuf, off, len); }
	public void mark(int readAheadLimit) throws IOException {
		in.mark(readAheadLimit); }
	public void finalize() throws Throwable { super.finalize(); }
}

/*
 //  dump FilterReader .. j12 won't allow super(null)
abstract class RsInput extends FilterReader
{
	private static StringReader dumbdumb= new StringReader("");// fix for nasty java 1.2 change
	RsInput()  throws IOException { super(dumbdumb); } // java1.2 requires non-null Reader(x) constructor!
	public void makeRewindable() {}
}*/


/**
 * Rewindable wrapper for File input
 */
class RsInputFile extends RsInput
{
	protected static final int kBufsize = 8192;
	protected File fInfile;
	//protected InputStreamReader  fInfs; //FileReader
	protected long flength;
	
	RsInputFile( File f) throws IOException {
		fInfile= f; 
		openFile();
		}
		
	RsInputFile() throws IOException { }

	public long guessLength()   { return flength; } 
	public File getFile()   { return fInfile; } 
	
	public void reset()  throws IOException { 
		try { in.close(); } catch (IOException ex2) {}
		openFile();
		}

	protected void openFile() throws IOException { 
				// was this - try what FileReader does
		//fInfs = new FileReader( fInfile);  
		flength= fInfile.length();
		inbytes= new FileInputStream(fInfile);
		in= new InputStreamReader(inbytes);
		//in= fInfs; 
		//fInbuf= new BufferedReader( fInfs, kBufsize);
		//fInbuf.mark( kBufsize); 
		//in= fInbuf; 
		lock= in;
		}
}


/**
 * Rewindable wrapper for InputStream input
 */
class RsInputStream extends RsInputFile
{
	protected InputStream fInsOrig;
	//protected BufferedInputStream fInbuf;
	//protected InputStreamReader fInrd;
	
	//? do we always need to makeRewindable() before reading anything? 
	// otherwise data can be lost! if buf.reset() fails
	
	RsInputStream( InputStream ins) throws IOException {
		this.fInsOrig= ins;
		openStream();
		}
		
	RsInputStream() throws IOException { }
		
	protected void openStream()  throws IOException { 
		in= new InputStreamReader( fInsOrig);
		//fInbuf= new BufferedInputStream( fInsOrig, kBufsize);
		//fInrd= new InputStreamReader( fInbuf);
		//in= fInrd;
		lock= in;
		makeRewindable(); //?? always
		}

	public void reset()  throws IOException { 
			if (fInfile==null)  makeRewindable(); //? do we need this?
			else {
			try { in.close(); } catch (IOException ex2) {}
			openFile();
			}
		}

	public void makeRewindable() 
	{
			// called only when format requires rewinding -- e.g., interleaved
		if (fInfile==null) {
			fInfile= Readseq.tempFile();
			//try { fInbuf.reset(); } catch (IOException ex) {}
			try {
				FileWriter fout= new FileWriter(fInfile);
				char[] buf= new char[8192];
				int n;
				do {
					n= in.read(buf);
					if (n>0) fout.write(buf, 0, n);
				} while (n>=0);
				fout.close();
				openFile();
				}
			catch (IOException ex) { ex.printStackTrace(); }
			}
	}
			// should be close() !?
	public void finalize() throws Throwable {
		super.finalize();
		if (fInfile!=null) {
			fInfile.delete();
			fInfile= null;
			}
		}
}


/**
 * Rewindable wrapper for URL input
 */
class RsInputUrl extends RsInputStream
{	
	URL url;
	RsInputUrl( URL url) throws IOException {
		super();
		this.url= url;
		this.fInsOrig= url.openStream();
		openStream();
		}
}


/**
 * Rewindable wrapper for Reader input
 */
class RsInputReader extends RsInputFile
{
	protected Reader fInsOrig;
	//? do we always need to makeRewindable() before reading anything? 
	// otherwise data can be lost! if buf.reset() fails
	
	RsInputReader( Reader ins)  throws IOException {
		this.fInsOrig= ins;
		openStream();
		}
		
	protected void openStream() throws IOException { 
		in= fInsOrig;
		//fInbuf= new BufferedReader( fInsOrig, kBufsize);
		//fInbuf.mark(kBufsize);
		//in= fInbuf; 
		lock= in;
		makeRewindable(); //??
		}

	public void reset()  throws IOException { 
		//try { fInbuf.reset();  }
		//catch (IOException ex) {
			if (fInfile==null) makeRewindable(); // need to get fInfile!
			else {
				try { in.close(); } catch (IOException ex2) {}
				openFile();
				}
			//}
		}

	public void makeRewindable() 
	{
			// called only when format requires rewinding -- e.g., interleaved
		if (fInfile==null) {
			fInfile= Readseq.tempFile();
			//try { fInbuf.reset(); } catch (IOException ex) {}
			try {
				FileWriter fout= new FileWriter(fInfile);
				char[] buf= new char[8192];
				int n;
				do {
					n= in.read(buf);
					if (n>0) fout.write(buf, 0, n);
				} while (n>=0);
				fout.close();
				openFile();
				}
			catch (IOException ex) { ex.printStackTrace(); }
			}
	}
	
			// should be close() !?
	public void finalize() throws Throwable {
		super.finalize();
		if (fInfile!=null) {
			fInfile.delete();
			fInfile= null;
			}
		}
	
}


/**
 * Rewindable wrapper for byte[] input
 */
class RsInputBytes extends RsInput
{
	ByteArrayInputStream ba;
	long flength;
	
	RsInputBytes( byte[] bytes)  throws IOException {
		flength= bytes.length;
		ba= new ByteArrayInputStream(bytes);
		this.inbytes= ba;
		in= new InputStreamReader( ba);  lock= in;
		}
	public long guessLength()   { return flength; } 
	public void reset()  throws IOException { ba.reset(); }
}


/**
 * Rewindable wrapper for char[] input
 */
class RsInputChars extends RsInput
{
	long flength;
	RsInputChars( char[] inchars)  throws IOException {
		flength= inchars.length;
		in= new CharArrayReader(inchars);  lock= in;
		}
	public long guessLength()   { return flength; } 
}

/**
 * Rewindable wrapper for String input
 */
class RsInputString extends RsInput
{
	long flength;
	RsInputString( String s)  throws IOException {
		flength= s.length();
		in= new StringReader( s); lock= in;
		}
	public long guessLength()   { return flength; } 
}

/**
 * Rewindable wrapper for OpenString input
 */
class RsInputOpenString extends RsInput
{
	long flength;
	RsInputOpenString( OpenString s)  throws IOException {
		flength= s.length();
		in= new CharArrayReader( s.getValue());  lock= in;
		}
	public long guessLength()   { return flength; }  
}



/*
	// for testing variants	of Readseq	 
public interface ReadseqIntf
{
	public void setInput( File f) throws IOException; // many others ?? (Object f)
	public void close() throws IOException;
	public boolean isKnownFormat(); 
	public boolean readInit();
	public void list( Writer out);
	public boolean readTo( BioseqWriterIface writer)  throws IOException;
}
*/