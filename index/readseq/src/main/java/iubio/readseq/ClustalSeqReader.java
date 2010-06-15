package iubio.readseq;

import java.io.IOException;

import flybase.OpenString;


/*
CLUSTAL W (1.8) multiple sequence alignment


HBB_HUMAN       --------VHLTPEEKSAVTALWGKVN--VDEVGGEALGRLLVVYPWTQRFFESFGDLST
HBB_HORSE       --------VQLSGEEKAAVLALWDKVN--EEEVGGEALGRLLVVYPWTQRFFDSFGDLSN
                          *:  :   :   *  .           :  .:   * :   *  :   .

HBB_HUMAN       PDAVMGNPKVKAHGKKVLGAFSDGLAHLDN-----LKGTFATLSELHCDKLHVDPENFRL
HBB_HORSE       PGAVMGNPKVKAHGKKVLHSFGEGVHHLDN-----LKGTFAALSELHCDKLHVDPENFRL
*/

//public
class ClustalSeqReader  extends InterleavedSeqReader  
{
	final static int kNameWidth= 15;  
	
	public ClustalSeqReader() {
		margin	=  0; //? or kNameWidth
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		}

	public boolean endOfSequence() {
		return false;
	}

	protected void read() throws IOException
	{
  	int  iline= 0;
	 	addit = (choice > 0);
	  if (addit) seqlen = seqlencount= 0;
	  boolean  done, first= true;
	  do {
	    getline();
	    done = endOfFile();
	    if (done && nWaiting==0) break;

			OpenString sid= null;
			if (nWaiting>kNameWidth)
	   		sid= sWaiting.substring(0,kNameWidth).trim();
	   		
	    if (sid==null || sid.length()==0) { // includes blanks and conserved line after seqs
	    	if (atseq>0) first= false;
	    	}
	    else {
   	 		if (first) {  
	        if (firstpass) nseq++; /// this is bad - need to stop incrementing 1st time thru
	        atseq++;  
	        if (choice == kListSequences)  addinfo(sid.toString());
	        else if ( atseq == choice ) {
	          addseq( getreadchars(), getreadcharofs()+kNameWidth, nWaiting-kNameWidth); 
	       		seqid= sid.toString();
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