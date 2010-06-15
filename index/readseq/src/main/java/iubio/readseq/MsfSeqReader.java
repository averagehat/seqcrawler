package iubio.readseq;

import flybase.OpenString;

import java.io.IOException;



// 28sep99 -failing ?
//public
class MsfSeqReader  extends InterleavedSeqReader //InterleavedSeqreader
{
public MsfSeqReader() {
	margin	=  0;
	addfirst= false;
	addend 	= false;  
	ungetend= true;
	testbase= new TestGcgBase();
	testbaseKind= kUseTester;
	//formatId= 15;
	}

public boolean endOfSequence() {
	return false;
	}	

//protected OpenString sid;

protected void read() throws IOException
{
	int  at, iline= 0;
	OpenString  sid= new OpenString("");
  boolean  done, indata= false;
 	addit = (choice > 0);
  if (addit) seqlen = 0;
  seqlencount= 0;
  do {
    getline();
    done = endOfFile();
    if (sWaiting.startsWith("!!")) continue; // GCG version 9+ comment line
		OpenString si= sWaiting;
		int offset= 0;
		
    if (done && nWaiting==0) break;
    
   	else if (indata) {
   		//?
   		offset= 0;
		while (offset<nWaiting && si.charAt(offset) <= ' ') offset++;
      if ( offset<nWaiting ) {
    	int seqat;
    	for (seqat= offset; si.charAt(seqat) > ' '; seqat++) ;
      	OpenString id= si.substring(offset,seqat).trim();
       // Debug.println("msf at id=<"+id+"> match="+id.equals(sid));
        if (id.equals(sid)) //sid.equals(id)) 
        	addseq( getreadchars(), getreadcharofs()+seqat, nWaiting-seqat);   
        iline++;
        }	   	
   		}
   	
    else if ( (at= si.indexOf("Name: ")) >= 0) {  
    	// seqq header line 
      // Name: somename      Len:   100  Check: 7009  Weight:  1.00 
      //nseq++; 
      atseq++; 
      at += 6;
      if (choice == kListSequences) 
      	addinfo( si.substring(at).trim().toString());
      else if (atseq == choice) {
        seqid= si.substring(at).trim().toString(); 
        int e;
        for (e= 0; seqid.charAt(e)> ' '; e++) ;
        sid= new OpenString( seqid.substring(0,e).trim());
        //Debug.println("msf sid=["+sid+"]");
        }
      }

    else if ( si.indexOf("//")>=0 ) {  
      indata = true;
      iline= 0;
			//if (nseq==0) {  }
			setNseq(atseq);
      if (choice == kListSequences) done = true;
      }
      
  } while (!done);
	allDone = true;
}

};
