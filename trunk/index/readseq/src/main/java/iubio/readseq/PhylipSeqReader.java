package iubio.readseq;

import flybase.Debug;
import flybase.OpenString;

import java.io.IOException;


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