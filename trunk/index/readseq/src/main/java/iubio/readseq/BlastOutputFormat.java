// iubio.readseq.BlastOutputFormat.java -- was blastread.java
// d.g.gilbert, 1999


package iubio.readseq;


import java.io.*;

import flybase.Debug;
import flybase.OpenString;
import flybase.FastHashtable;

import iubio.bioseq.BaseKind;


/** 
	* BLAST multalign output parser - read only - similar to PAUP ileave
	*
	*/

public class BlastOutputFormat extends BioseqFormat
{		
	public String formatName() { return "BLAST";  }  
	public String formatSuffix() { return ".blast"; }  
	public String contentType() { return "biosequence/blast"; }  

	public boolean canread() { return true; }    
	public boolean canwrite() { return false; }  // no desire to try to write - read only seqdata
	public boolean interleaved() { return true; }  
	public boolean needsamelength() { return true; }  

	public BioseqReaderIface newReader() { return new BlastOutputReader(); }
	//public BioseqWriterIface newWriter() { return new BlastOutputWriter(); }		

	protected boolean hasblast, hasquery, hasdb;
	public void formatTestInit() { super.formatTestInit(); hasblast= hasdb= hasquery= false; }
	
	public static String blastKey= "blast", queryKey= "query=", dbKey= "database:";
	
	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		String s= line.toLowerCase();
		if (s.startsWith(blastKey)) {
 			hasblast= true;
      formatLikelihood= 40; //? not all have this line?
 			recordStartline= atline;
      return true; //(formatLikelihood>60);
      }
    else if (s.startsWith(queryKey)) {
  		hasquery= true;
      formatLikelihood += 35;  // both blast & query= lines would make this fairly certain?
  		recordStartline= atline;
      return true; //(formatLikelihood>60);
      }
    else if (s.startsWith(dbKey)) {
  		hasdb= true;
 			if (hasquery && hasblast) formatLikelihood= 95;
 			else if (hasquery) formatLikelihood= 90; //? 80? any conflicts?
  		recordStartline= atline;
 			// do we need to scan for alignment data?
      //formatLikelihood += 20; // this also should clinch format
      return true; //(formatLikelihood>60);
      }
    else
    	return false;
	}

}


/**
	* @see iubio.readseq.PaupSeqReader
	*/
	
//public 
class BlastOutputReader  extends  BioseqReader  
{
	protected char matchchar= 0; 
	protected final int kMinSeqline = 30; //?
	protected boolean firstSeqline, atname, domatch, done; 
	protected OpenString saveseq;
	protected String sid1;
	protected int atbase, alignmentLine, iline, seqindent, seqendindent;
	protected FastHashtable idhash= new FastHashtable();
	
	public BlastOutputReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		}

	protected void read() throws IOException
	{
    if (alignmentLine==0) alignmentLine= findAlignments();
    if (alignmentLine<0) return; //?
    readAlignments();
	}
	
	public void skipPastHeader(int skiplines) 
	{
   	super.skipPastHeader( skiplines);
   	try {
	   	if (alignmentLine>0) // is this count after super.skip? 
	   		for (int i= alignmentLine-1; i>0; i--) getline(); //? -1 or not?
    } catch (IOException e) {}
	}
 		
    	// 1st find start of align seq data, if there
//tmpseq_1 37     tcggcttttcctgaattccaaaagggaagagggtata--atgagggatg-tcccaccctc 93
//AF124730 59465    ..............................a....ta......c.ct-..tg...... 59521
// or ...
//QUERY      1   MVTENPQRLTVLRLATNKGPLAQIWLASNMSN-IPRGSVIQTHIAESAKEIAKASGCDDE 59
	
	protected int findAlignments() throws IOException
	{
 		//Debug.print( "BlastOutputReader.findAlignments=");
 			// this should set skiplines so we can skip repeated calls here!
  	int aline= 0;
  	while (!endOfFile()) {
      getline();
      aline++;
      int at= sWaiting.indexOf(' ');
      if (at>0) {
	      int len= nWaiting;
    		boolean haveSeqChar= false, maybeSeqLine= true;
      	while (!haveSeqChar && maybeSeqLine) {
 	      	at++;
      		if (at>=len) maybeSeqLine= false;
      		else {
	       		char c= sWaiting.charAt(at);
	     			if (c <= ' ') ;
	     			else if (BaseKind.isAlphaseq(c)) haveSeqChar= true;
	      		else if (!(BaseKind.isIndel(c) || (c >= '0' && c <= '9')))  
	      			maybeSeqLine= false;
	      		}
      		}
      		
				if (haveSeqChar) {
	     		int e;
	      	for (e= at; e<len; e++) {
	      		char c= sWaiting.charAt(e);
	      		if ( c <= ' ') break;
	      		else if (! (BaseKind.isAlphaseq(c) || BaseKind.isIndel(c)) ) {
	      			 maybeSeqLine= false; 
	      			 break;
	      			 }
						}
					if (maybeSeqLine && e > at + kMinSeqline) {
						seqindent= at; // always start counting seq here even if blanks
						seqendindent= e; // is this always valid?
						//Debug.println( "line "+aline);
						return aline; //? any more?
						}
	      	}
    	 	}
      	
			}
		//Debug.println("-1");
		return -1;
	}


//end of data - non blank line that doesn't start with seqname	
//  Database: Non-redundant GenBank+EMBL+DDBJ+PDB sequences
//    Posted date:  Aug 1, 1999  2:25 PM
//  Number of letters in database: 1,237,149,220
//  Number of sequences in database:  453,800
/*
	have to handle this sort of insertion info -- leading space is okay, but skip line
AC004616 118202               .............g.....c..g......c......-.g.t..... 118157
                                                     \                      
                                                     |
                                                    ta                      
*/

	final static int kEndOfSequence= -1, kSkipLine= 0, kSequenceLine= 1;

	public boolean endOfSequence() {
		return (isSequenceLine() == kEndOfSequence);
		}
	
	protected int isSequenceLine() // -1 == end of data, 0 = false, 1 = true
	{
		if (nWaiting == 0) return kSkipLine; // blank line?
		char c= sWaiting.charAt(0);
		if (Character.isLetterOrDigit(c)) {  // start with alpha(?digit) name
			if (nWaiting > seqindent) return kSequenceLine; //? more checks
			else return kEndOfSequence; //?
			}
		else if (c == ' ') {
			int at= 0; 
			while (at<nWaiting && sWaiting.charAt(at) <= ' ') at++;
			if (at == nWaiting) return kSkipLine; // blank line?
			else if (at < seqindent) return kEndOfSequence;  
			else return kSkipLine; //? a line with insert info?
			}
		else return kSkipLine; //? kEndOfSequence; //?
	}
	
	class SeqIndex {
		int	atseq, atbase;
		SeqIndex( int nseq, int nbase) { atseq= nseq; atbase= nbase; }
		}
			
	protected void readAlignments() throws IOException
	{
		//Debug.println( "BlastOutputReader.readAlignments");
		firstSeqline = atname= true;
	 	domatch= true; // don't know matchchar till we see 2nd+ seq //(matchchar > 0);
	 	addit = (choice > 0);
	  if (addit) seqlen = 0;
	  seqoffset= atbase= seqlencount = iline= 0;

				// at call, sWaiting has 1st seq line
	 	readIndata();
	  do {
	    getline();
	    done = endOfFile();
	    if (done && nWaiting==0) break;
	    //if (endOfSequence()) done= true;
	    switch (isSequenceLine()) {
	    	case kSkipLine: break; //firstSeqline= false;
	    	case kEndOfSequence: done= true; break;
	    	case kSequenceLine: readIndata(); break;
	    	}
	  } while (!done);
	  
	  nseq= idhash.size(); //??
		setNseq(nseq);//?
		allDone = true;
	}

	protected void fixmatchchar(int offset)
	{
		if (matchchar == 0) {
			// may be '.' or '-', any others? ' ' is leading indel
		  int dots= 0, dashes= 0;
		  int len= nWaiting - offset;
		  for (int i=0; i < len; i++) {
		  	int c= getreadbuf(offset+i);
				if (c == '.') dots++;
				else if (c == '-') dashes++;
				}
			if (dashes > dots) matchchar= '-'; else matchchar= '.';
			}
	  for (int i=0; i < saveseq.length(); i++) {
	  	int c= getreadbuf(offset+i);
	  	if (c == ' ') setreadbuf(offset+i, BaseKind.indelEdge); else
	  		//^^? instead maybe adjust seqoffset in idhash()
	  		//? add opt in Bioseq for seqoffset offset?
	  	if (c == matchchar) setreadbuf(offset+i, saveseq.charAt(i));
	    }
	}
	
	
//tmpseq_1 149    aagaggagaggtccact-cagatggttgggggacttgagaattttatttttggtttatat 207
//AF124730 59578  ........t...t..t.-..........aa..g...-....................    59632
//AL030999 32080                                       ....................    32099
//Z95126   169903                    ..............ac.t....................    169940
    // !! need to correct for matchchar 
	//! data always has query seq, but others may or may not be in an alignment block
	//  some seqs show up first after several blocks
	
	
	protected void readIndata()
	{
    OpenString si= sWaiting;
  	//int seqat, offset= 0;
    //while (offset<nWaiting && si.charAt(offset) <= ' ') offset++;
		//if (si.indexOf(';')>0) indata= false;
		
    // if (offset<nWaiting && Character.isLetterOrDigit(si.charAt(offset)) )  
    if (nWaiting>0)
    {
      	// valid data line always starts w/ a left-justified seqq name 
			int endname= si.indexOf(' ');
			if (endname<=0) return; //?
	  	String sid= si.substring( 0, endname).trim().toString();
	  	
			if (idhash.get(sid) == null) {
				// first pass thru file with atseq==1, query seq, will get all ids
				nseq++;
				idhash.put( sid, new SeqIndex( nseq, seqoffset));
				// need also save current base index for start of seq in total alignment!
        if (choice == kListSequences) addinfo( sid);
				}
   		SeqIndex sind= (SeqIndex)idhash.get( sid);
     	atseq= sind.atseq; //atseq++; //??
      if (atseq == choice) seqid= sid; 
      if (atseq == 1) sid1= sid;
          
			int seqat= seqindent; // constant tab to seq data?
			if (seqat >= nWaiting) return; //?
			int endseq= seqendindent; // is this always accurate? maybe not
			if (endseq >= nWaiting) endseq= nWaiting;
			for (boolean badc= true; badc && (endseq>seqat); ) {
				char c= si.charAt(endseq-1);
				if (c > ' ' && (BaseKind.isAlphaseq(c) || BaseKind.isIndel(c))) 
					badc= false;
				else endseq--;
				} 

    	if (sid.equals(seqid)) { // (atseq == choice)
        if (domatch) {
          if (sid.equals(sid1)) saveseq= si.substring(seqat, endseq);
          else fixmatchchar( seqat);
          }
        if (firstSeqline && atseq != 1 && seqoffset>0) {
        	setSeqoffset(seqoffset);
        	}
        addseq( getreadchars(), getreadcharofs()+seqat, endseq - seqat);
	    	if (atseq == 1) { seqoffset= atbase; atbase= seqlen; }
				//if (firstSeqline) Debug.println("id="+sid+", seq="+si.substring(seqat, endseq));
				firstSeqline= false; 
        }
        
			else if (sid.equals(sid1)) { // (atseq == 1)
       		// always count atbase in query alignment we are
        if (domatch) saveseq= si.substring(seqat, endseq);
        	//? or bases= endseq - seqat; 
        seqoffset= atbase;
        int bases= countseqline( getreadchars(), getreadcharofs()+seqat, endseq - seqat);
       	atbase += bases;
      	}
  		iline++;
 			}
	}
	

};


/*
BlastOutputFormat.html
<HTML><BODY><PRE>
 Read aligned sequences from output of BLAST.
 August 1999

 In particular, read the multiple alignments, rather than pairwise alignments.
 At NCBI's BLAST service now these are called
		master-slave with identities
		master-slave without identities
		flat master-slave with identities     -- best to parse
		flat master-slave without identities	-- best to parse
 
 Various blast programs have different headers, but Query= and Database: seem constant
 
BLASTN 2.0.9 [May-07-1999]
^^^^^ key string

Query= djs74_1180.s1 CHROMAT_FILE: djs74_1180.s1 PHD_FILE:
^^^^^^ key string

Database: Non-redundant GenBank+EMBL+DDBJ+PDB sequences
^^^^^^^^^ key string

may or may not have Scores before alignments
                                                                   Score     E
Sequences producing significant alignments:                        (bits)  Value

gb|AF124730|AF124730  Homo sapiens PAC clone N2184 chromosom...   123  7e-26
emb|AL030999.2|HS593F22  Human DNA sequence from clone 593F2...   111  3e-22
...

align formats vary some - look for line >= 40 chars of bases, no spaces 


tmpseq_1 37     tcggcttttcctgaattccaaaagggaagagggtata--atgagggatg-tcccaccctc 93
AF124730 59465    ..............................a....ta......c.ct-..tg...... 59521
AL030999 31934      .......................g.....c...--......c...-...a...... 31986
...

QUERY      1   MVTENPQRLTVLRLATNKGPLAQIWLASNMSN-IPRGSVIQTHIAESAKEIAKASGCDDE 59
S50979     1   MVTENPQRLTVLRLATNKGPLAQIWLASNMSN-IPRGSVIQTHIAESAKEIAKASGCDDE 59
CAA88356   1   MVTENPQRLTVLRLATNKGPLAQIWLASNMSN-IPRGSVIQTHIAESAKEIAKASGCDDE 59

</PRE></BODY></HTML>
*/
