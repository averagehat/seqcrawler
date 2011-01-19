//iubio/readseq/PhylipSeqFormat.java
//split4javac// iubio/readseq/InterleavedSeqReader.java date=13-Jun-2003

// InterleavedSeqReader.java -- was seqread2.java
// low level readers & writers : interleaved formats
// d.g.gilbert, 1990-1999


package iubio.readseq;


import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;

import Acme.Fmt;
import flybase.Debug;
import flybase.OpenString;
//import flybase.Native;

import iubio.bioseq.BaseKind;
import iubio.bioseq.SeqInfo;
import iubio.bioseq.Bioseq;

// PaupSeqFormat reader needs work/test
// PrettySeqFormat not readable
// OlsenSeqFormat not readable yet

 

//split4javac// iubio/readseq/InterleavedSeqReader.java line=291
public class PhylipSeqFormat extends BioseqFormat
{
	protected SeqInfo seqkind;
	protected boolean formatDetermined;
	protected boolean interleaved= true; // assume yes?
	protected int isleaf, isseq;

	protected NumSppBases nsppb;	
	//protected boolean gotSppLen;
	//protected int nospp, baselen;
	
	public PhylipSeqFormat() {
		//seqkind= new SeqKind( 99999, false, false);
		seqkind= SeqInfo.getSeqInfo( 99999, false, false);
		nsppb= new NumSppBases();
		}

		//fileSuffix= ".phylip3";
		//mimeType= "biosequence/phylip3";
		
	public String formatName() { 
		getsubformat();
		if (interleaved) return "Phylip|Phylip4"; 
		else return "Phylip3.2|Phylip2"; 
		}  
	public String formatSuffix() { return ".phylip"; } 
	public String contentType() { return "biosequence/phylip"; } 
	public boolean canread() { return true; }
	public boolean canwrite() { return true; }

	public boolean interleaved() { getsubformat(); return interleaved; }
	public boolean needsamelength() { return true; }

	public BioseqWriterIface newWriter() { 
		getsubformat();
		PhylipSeqWriter c= new PhylipSeqWriter(); 
		c.setinterleaved(interleaved);
		return c; 
		}
		
	public BioseqReaderIface newReader() {
		getsubformat();
		PhylipSeqReader c;
		if (interleaved) c= new PhylipSeqReader(); 
		else c= new Phylip2SeqReader();
		//? set some flags in c ?
		return c; 
	}
	
	protected void getsubformat() {
		if (!formatDetermined) {
			if (isleaf > isseq) interleaved= true;  
			else if (isleaf < isseq) interleaved= false;  
			else interleaved= true; 
			formatDetermined= true;
			}
		}	

		// format testing =============================	

	public void formatTestInit() { 
		super.formatTestInit(); 
		isleaf= isseq= 0;
		interleaved= true;
		formatDetermined= false;
		//nospp= 0; baselen= 0; gotSppLen= false; 
		nsppb.init();
		}

	public boolean formatTestLine( OpenString sp, int atline, int skiplines) 
	{
		atline -= skiplines;
		if (atline == 1)  {
  		nsppb= readSpeciesLength(sp);
  		if (nsppb.good) formatLikelihood += 15;
  		}
  		
		else if (atline == 2 && nsppb.good && sp.length()>10) {
      //int tseq= Bioseq.getSeqtype(sp, 10, sp.length()-10);
			seqkind.add( sp.getValue(), sp.getOffset()+10, sp.length()-10);	
			int tseq= seqkind.getKind();
      if (Character.isLetter(sp.charAt(0))   // 1st letter in 2nd sp must be of a name 
       && (tseq != Bioseq.kOtherSeq)) {			 // sequence section must be okay 
	     	formatLikelihood += 80; // 90 causes checker to stop
	     	//return true; //? or not to keep reading && test subformat
	     	//formatId= Readseq.kPhylipUndetermined;
	     	formatDetermined= false;
	     	}
      }
      
    else if (atline > 2 && nsppb.good) {
			int j, tseq, tname;
      //tname= Bioseq.getSeqtype(sp, 0, 10);
      //tseq=  Bioseq.getSeqtype(sp, 10, sp.length()-10);
      	// can we assume leading whitespace is preserved and format indicator?
      for (j=0; Character.isWhitespace(sp.charAt(j)) && j<10; j++)  ;
      if (atline - 1 <= speciesCount()) {
      	if (j<9) isleaf++; else isseq++;
      	}
      else {
      	if (j>=9) isleaf++; else isseq++;
      	}
    	}
		return false;
	}

			// these are used also by PhylipSeqReader !?!
	public final int speciesCount() { return nsppb.nospp; }
	public final int sequenceLength() { return nsppb.baselen; }
	
	public static NumSppBases readSpeciesLength( OpenString sp) 
	{
    //sscanf( sp, "%d%d", &nospp, &baselen);
    // this is kind of messy w/o a sscanf !
		int nospp= 0; 
		int baselen= 0;
    int i, j, n= sp.length();
    for (i=0; i<n && Character.isWhitespace(sp.charAt(i)); i++) ;
    for (j=i; j<n && Character.isDigit(sp.charAt(j)); j++) ;
    try { if (i<n) nospp= Integer.parseInt(sp.substring(i,j).toString()); }
    catch (NumberFormatException ex) {}
    if (nospp>0) {
      for (i=j+1; i<n && Character.isWhitespace(sp.charAt(i)); i++) ;
      for (j=i; j<n && Character.isDigit(sp.charAt(j)); j++) ;
      try { if (i<n) baselen= Integer.parseInt(sp.substring(i,j).toString()); }
      catch (NumberFormatException ex) {}
    	}
		if (nospp > 0 && baselen > 0)
			Debug.println("phylip nspp="+nospp+", nbase="+baselen);
    //return (nospp > 0 && baselen > 0);
    return new NumSppBases(nospp,baselen);
	}

	
}

class NumSppBases {
	boolean good;
	int nospp, baselen;
	NumSppBases() { this(0,0); }
	NumSppBases(int nospp, int baselen) { 
		this.nospp= nospp; this.baselen= baselen;
		good= (nospp>0 && baselen>0);
		}
	void init() { nospp= baselen= 0; good= false; }
}


//public
class Phylip2SeqReader  extends PhylipSeqReader
{
	public Phylip2SeqReader() {
		super();
		interleaved= false;
		}
		
	public boolean endOfSequence() {
		return endSequential();
		}
	protected void read() throws IOException {
		readSequential(); 
		}

	/*public Object clone() {
		Phylip2SeqReader c= (Phylip2SeqReader) super.clone();
		//? doesn't clone() copy vals of method data ?
    return c;
 		}*/

	protected boolean endSequential()
	{
		ungetend= false;
		countseq(getreadchars(), getreadcharofs()+margin, nWaiting); //countseq(sWaiting);
		boolean done= ( seqlencount >= sequenceLength());
	 	addend= !done;
		return done;
	}

	protected void readSequential() throws IOException
	{
		if (sequenceLength()==0 || speciesCount() == 0) {
	    NumSppBases nsppb= PhylipSeqFormat.readSpeciesLength(sWaiting); //!? must have already read these?
	    nospp= nsppb.nospp;
	    baselen= nsppb.baselen;
			Debug.println("format: phylip-sequential, nspp="+speciesCount()+", nbase="+sequenceLength());
			getline();
			}
		setNseq(speciesCount());
	  while (!allDone) {
	    seqlencount= 0;
	    seqid= sWaiting.substring(0,10).toString();
	    sWaiting= sWaiting.substring(10);
	    nWaiting= sWaiting.length();

	    margin= 0;
	    addfirst= true;
	    readLoop();
	    if (endOfFile()) allDone = true;
	  	}
	}

}


//public
class PhylipSeqReader  extends InterleavedSeqReader //InterleavedSeqreader
{
	final static int kNameWidth= 10; 
	protected int nospp= 0, baselen= 0;
	//protected boolean formatDetermined;
	protected boolean interleaved;
	
	public PhylipSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//formatId= 12;
		interleaved= true;
		//formatDetermined= false;
		}
 	
	/*public Object clone() {
		PhylipSeqReader c= (PhylipSeqReader) super.clone();
		//? doesn't clone() copy vals of method data ?
		c.nospp= nospp;
		c.baselen= baselen;
    return c;
 		}*/

	public boolean endOfSequence() {
		return true;
	}

	protected void read() throws IOException
	{
		readInterleaved();
	}

	public final int speciesCount() { return nospp; }
	public final int sequenceLength() { return baselen; }

	protected void readInterleaved() throws IOException
	{
	  boolean done, first = true;
	  int     iline= 0;

		addit = (choice > 0);
	  if (addit) seqlen = seqlencount= 0;
		//? already have read?
		if (sequenceLength()==0 || speciesCount() == 0) {
	    NumSppBases nsppb= PhylipSeqFormat.readSpeciesLength(sWaiting); //!? must have already read these?
	    nospp= nsppb.nospp;
	    baselen= nsppb.baselen;
			Debug.println("format: phylip-interleaved, nspp="+speciesCount()+", nbase="+sequenceLength());
			}
			
		setNseq(speciesCount());
	  do {
			getline();
	 		done = endOfFile();
	    if (done && nWaiting==0) break;
	    OpenString si= sWaiting.trim();
	    if (si.length()>0) {

	      if (first) {  
	      	// collect seqq names + seqq, as fprintf(outf,"%-10s  ",seqname); 
	        //! nseq++; //setNset() did this !
	        atseq++; //??
	        if (atseq >= speciesCount()) first= false; //?
	        if (choice == kListSequences) 
	          addinfo( sWaiting.substring(0,kNameWidth).trim().toString());
	          
	        else if ( atseq == choice) {
	          addseq( getreadchars(), getreadcharofs()+kNameWidth, nWaiting-kNameWidth); // sWaiting.substring(10);
	          seqid=  sWaiting.substring(0,kNameWidth).trim().toString();
	          }
	        }
	      else if ( iline % atseq == choice - 1 ) 
	        addseq( getreadchars(), getreadcharofs()+kNameWidth, nWaiting-kNameWidth);
	        
	  		iline++;
	    	}
	  } while (!done);
		allDone = true;
	}

};


//public
class PhylipSeqWriter extends InterleavedSeqWriter
{
	BufferedWriter tempOs;
	//int lastlen;
	//String lenerr;
	
	public void writeHeader() throws IOException { 
		super.writeHeader();
		if (!interleaved()) {
			// interleaveHeader(); //! need to count nseq, seqlen first !
			tempFile= Readseq.tempFile();
			tempOs= new BufferedWriter( new FileWriter(tempFile));
			douts= tempOs;
			}
		}
	
	public void writeRecordStart() {
		super.writeRecordStart();
   	opts.spacer = 10;
    opts.tab = 12; 
    l1  = -1; //??
		}

	public void writeDoc() {
		super.writeDoc();
			//!! must we TRuncate idword? -- probably, but causes hassles! e.g. #Mask cut
		writeString( Fmt.fmt( idword, 10, Fmt.TR + Fmt.LJ) + "  ");
    //linesout += 0; // no newline !
 		}

	protected void interleaf(int leaf) {
		writeln(); // is newline legal for phylip here?
		}
		
	protected void interleaveHeader() {
   	writeString(" " + nseq);  // these are 0 if not interleaved !?
    writeString(" " + seqlen);
  	if (interleaved()) 
     	writeln(); //sprintf( line, " %d %d\n", nseqs, nbases);  // " %d %d F\n"
   	else 
     	writeln(" I "); //sprintf( line, " %d %d I \n", nseqs, nbases);  // " %d %d FI \n"
	}

//	public boolean setSeq( Object seqob, int offset, int length, String seqname,
//						 Object seqdoc, int atseq, int basepart) 
//	{
//		if (lastlen > 0 && length != lastlen) {
//			if (lenerr==null) lenerr= String.valueOf(lastlen) + " != ";
//			lenerr += String.valueOf(length) + ", ";
//			length= lastlen; // can we pad/trunc to first length?
//			}
//		else lastlen= length;
//		return super.setSeq(seqob,offset,length,seqname,seqdoc,atseq, basepart);
//	}
	
	public void writeTrailer()  // per file 
	{ 
//		if (lenerr!=null) {
//			BioseqReader.message("Warning: this format requires equal sequence lengths.");
//			BioseqReader.message("       : lengths are padded/truncated to "+lastlen);
//			BioseqReader.message("       : " + lenerr);
//			}
			
		if (!interleaved()) {
			try { douts.close(); } catch (IOException ex) {} // == tempOs
					// reset output from temp to final stream
			setOutput(outs);

			interleaveHeader();  
			sequentialOutput();
			}
		super.writeTrailer();
	}
	
	
	protected void sequentialOutput()
	{
		try {
			Reader tempis= new FileReader(tempFile);
			char[] buf = new char[2048];
			int nread;
			while ( (nread= tempis.read(buf)) >= 0)  
				douts.write(buf, 0, nread);
			tempis.close();
			}
		catch (Exception ex) { ex.printStackTrace(); }
		//if (!(Debug.isOn)) 
		{ tempFile.delete(); tempFile= null; }
	}

	
};



