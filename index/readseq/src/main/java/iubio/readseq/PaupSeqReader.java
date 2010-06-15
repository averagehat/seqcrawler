package iubio.readseq;

import flybase.OpenString;

import java.io.IOException;




//public
class PaupSeqReader  extends  InterleavedSeqReader //InterleavedSeqreader
{
	char matchchar= 0; 
	int topseqlen, topnseq;
	
	public PaupSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//testbaseKind= kUseTester;
		//formatId= 17;
		}

	public boolean endOfSequence() {
		return false;
		}

	protected void read() throws IOException
	{
  boolean done= false;
  boolean interleaved= false;
  int i, j, n;
  while (!done && !endOfFile()) {
    getline();
    OpenString os= new OpenString(sWaiting);
    os.lowerCase(); //? why os here when UC on write???
    n= nWaiting;
    if (os.indexOf("matrix")>=0) done= true;
    if (os.indexOf("interleav")>=0) interleaved= true;
    if ((i= os.indexOf("ntax="))>=0)  {
    	i += 5;
    	for (j=i+1; j<n && Character.isDigit(os.charAt(j)); j++) ;
    	topnseq= Integer.parseInt(os.substring(i,j).toString());
    	}
    if ((i= os.indexOf("nchar="))>=0) {
    	i += 6;
    	for (j=i+1; j<n && Character.isDigit(os.charAt(j)); j++) ;
    	topseqlen= Integer.parseInt(os.substring(i,j).toString());
    	}
    if ((i= os.indexOf("matchchar="))>=0)  {
    	i += 10;
    	if (os.charAt(i) == '\'' || os.charAt(i) == '"') i++;
				matchchar= os.charAt(i);
    	}
    }
		setNseq(topnseq);
		//Debug.println("nexus-ileaf, nspp="+topnseq+", nbase="+topseqlen);
 	readLoop(interleaved);
	}


	protected boolean first, atname, domatch, done, indata; 
	protected OpenString saveseq;
	protected String sid, sid1;
	protected int iline;
	
//	protected StringBuffer savebuf;
//	protected void savebuf(int IGNOREseqindex, char[] swait, int offset, int len) {
//		// ignore seqindex !
//		if (savebuf==null) savebuf= new StringBuffer();
//		Debug.println("savebuf at="+savebuf.length()+" b="+swait[offset]);
//		String savec= new String(swait,offset,len);
//		savebuf.append(  savec);
//		}
		
	protected void matchsaved(int seqindex, int offset, int len) {
		len= Math.min(len, saveseq.length()); // should be same
	  for (int i=0; i < len; i++)  
	    if (getreadbuf(offset+i) == matchchar) {
	   		char mc= saveseq.charAt(i); //Character.toUpperCase(saveseq.charAt(i)); 
	    	setreadbuf(offset+i, mc);  
	    	}
		//Debug.println("matchsaved at="+seqindex+" b="+saveseq.charAt(0)); //savebuf.charAt(seqindex));
		}
		
	protected final int skipName(int seqat, OpenString si) {
		for ( ; seqat < nWaiting && Character.isLetterOrDigit(si.charAt(seqat)); seqat++) ;
		for ( ; seqat < nWaiting && si.charAt(seqat) <=  ' '; seqat++) ;
  return seqat;
	}
	
	protected void readIndata()
	{
  // [         1                    1                    1         ] 
  // human     aagcttcaccggcgcagtca ttctcataatcgcccacggR cttacatcct 
  // chimp     ................a.t. .c.................a .......... 
  // !! need to correct for matchchar 

	int seqat, offset= 0;
  OpenString si= sWaiting; // ==  getreadchars(), getreadcharofs()+seqat, nWaiting - seqat
  while (offset<nWaiting && si.charAt(offset) <= ' ') offset++;
		if (si.indexOf(';')>0) indata= false;
		
			// this isLetOrDig skips '[   1  2 3 ... ]' headers !?
  if (offset<nWaiting && Character.isLetterOrDigit(si.charAt(offset)) )  {
     	// valid data line starts w/ a left-justified seqq name 
   	seqat= skipName(offset, si);
  
    if (first) {
	    	if (firstpass) nseq++; /// this is bad - need to stop incrementing 1st time thru
      atseq++; 
      if (atseq >= topnseq) first= false;

      if (choice == kListSequences)  
        addinfo( si.substring(0,seqat).trim().toString());
        
      else if (atseq == choice) {
	        seqid= si.substring( 0, seqat).trim().toString();
        sid= seqid;
        if (domatch) {
          if (atseq == 1) {
          	sid1= sid;
          	saveseq= si.substring(seqat); // NOT USED
  	  			//saveseq= saveseq.replace('c','w');//DEBUG -- NOT SEEN
    				//saveseq= new OpenString( saveseq.toUpperCase()); //DEBUG -- this is the bad saveseq
          	}
          else  
          	matchsaved(seqlen, seqat, nWaiting - seqat);            	 
          }
	        addseq( getreadchars(), getreadcharofs()+seqat, nWaiting - seqat);
        }
      else { 
        if (atseq == 1 && domatch) {
	     			saveseq= si.substring(seqat);  // used
  	  		//saveseq= saveseq.replace('a','z');//DEBUG *** 1st group
    			//saveseq= new OpenString( saveseq.toUpperCase()); //DEBUG -- this is the bad saveseq
						}
        }
      }

    else if ( si.indexOf(sid) == offset ) {
      if (domatch) {
      	if (sid.equals( sid1)) {  
      		saveseq= si.substring(seqat); // NOT USED
      		//saveseq= saveseq.replace('t','x'); //DEBUG  -- NOT SEEN
    			//saveseq= new OpenString( saveseq.toUpperCase()); //DEBUG -- this is the bad saveseq
      		}
        else 
       		matchsaved(seqlen, seqat, nWaiting - seqat);
        }
	    	addseq(getreadchars(), getreadcharofs()+seqat, nWaiting - seqat);
      }

    else if (domatch && (si.indexOf(sid1) == offset) ) {
	     	saveseq= si.substring(seqat); // used
  	  //saveseq= saveseq.replace('g','y');//DEBUG *** 2nd group
    	//saveseq= new OpenString( saveseq.toUpperCase()); //DEBUG -- this is the bad saveseq
      }
    else {
    
      }

		iline++;
			}
	}
	
	
	protected void readSeqData()
	{
    // [         1                    1                    1         ] 
    // human     aagcttcaccggcgcagtca ttctcataatcgcccacggR cttacatcct 
    //           aagcttcaccggcgcagtca ttctcataatcgcccacggR cttacatcct 
    // chimp     ................a.t. .c.................a .......... 
    //           ................a.t. .c.................a .......... 

	int seqat, offset= 0;
  OpenString si= sWaiting;
  while (offset<nWaiting && si.charAt(offset) <= ' ') offset++;
		if (si.indexOf(';')>0) indata= false;
		
  if ( offset<nWaiting && Character.isLetterOrDigit(si.charAt(offset)) )  {
    // valid data line starts w/ a left-justified seqq name 
    if (atname) {
	   		if (firstpass) nseq++; /// this is bad - need to stop incrementing 1st time thru
      atseq++; //??
      seqlencount = 0;
      atname= false;
   		seqat= skipName(offset, si);

      if (choice == kListSequences) {
        	// ! we must count bases to know when topseqlen is reached ! 
	        countseq(getreadchars(), getreadcharofs()+seqat, nWaiting - seqat);
        if (seqlencount >= topseqlen) atname= true;
        addinfo( si.substring(0,seqat).trim().toString());
        }
        
      else if (atseq == choice) {
	        seqid= si.substring( 0, seqat).trim().toString();

						//FIXME - domatch for sequential - need to save all of 1st seq
        //if (domatch) {
        //  if (atseq == 1) sid1= seqid;
        //  else matchsaved(seqlen, seqat, nWaiting - seqat);            	 
        //  }

	    		addseq(getreadchars(), getreadcharofs()+seqat, nWaiting - seqat);
        seqlencount= seqlen;
        if (seqlencount >= topseqlen) atname= true;
        }
        
      else {
        //if (atseq == 1 && domatch) saveseq= si.substring(seqat);  
        countseq(getreadchars(), getreadcharofs()+seqat, nWaiting - seqat);
        if (seqlencount >= topseqlen) atname= true;
        }
      }

    else if (atseq == choice) {
      //if (domatch && !seqid.equals( sid1)) matchsaved(seqlen, seqat, nWaiting - seqat);
      addseq( getreadchars(), getreadcharofs(), nWaiting);
      seqlencount= seqlen;
      if (seqlencount >= topseqlen) atname= true;
      }
    else {
    	//if (domatch && (si.indexOf(sid1) == offset) )  saveseq= si.substring(seqat); // used
      countseq(getreadchars(), getreadcharofs(), nWaiting);
      if (seqlencount >= topseqlen) atname= true;
      }
    }
	}
	
	protected void readLoop(boolean interleaved) throws IOException
	{
		indata= first = atname= true;
	 	domatch= (matchchar > 0);
	 	addit = (choice > 0);
	  if (addit) seqlen = 0;
	  seqlencount = iline= 0;
	  do {
	    getline();
	    done = endOfFile();
	    if (done && nWaiting==0) break;
	   	else if (indata) {
	   		if (interleaved) readIndata();
	   		else readSeqData();
	   		}
	    else {
    	String s= sWaiting.toLowerCase();
    	if (s.indexOf("matrix")>=0) {
	      	indata= atname= true;
	      	iline= 0;
	      	if (choice == kListSequences) done = true;
	      	}
	      }
	  } while (!done);
		allDone = true;
	}

};