package iubio.readseq;

import flybase.Debug;
import flybase.OpenString;
import iubio.bioseq.Bioseq;
import iubio.bioseq.SeqInfo;

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
		if (atline == 1)  
  		nsppb= readSpeciesLength(sp);
  		
		else if (atline == 2 && nsppb.good && sp.length()>10) {
      //int tseq= Bioseq.getSeqtype(sp, 10, sp.length()-10);
			seqkind.add( sp.getValue(), sp.getOffset()+10, sp.length()-10);	
			int tseq= seqkind.getKind();
      if (Character.isLetter(sp.charAt(0))   // 1st letter in 2nd sp must be of a name 
       && (tseq != Bioseq.kOtherSeq)) {			 // sequence section must be okay 
	     	formatLikelihood = 85; // 90 causes checker to stop
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