package iubio.readseq;

import flybase.Debug;
import iubio.bioseq.SeqInfo;
import Acme.Fmt;




//public
class EmblSeqWriter  extends BioseqWriter
{
	final static int seqwidth= 60, ktab= 5, kspacer= 10, knumwidth= 8, knumflags= 0;  
	protected int seqkind;
	
	public void writeRecordStart()
	{
		super.writeRecordStart();
 	opts.tab = ktab;    
  opts.spacer = kspacer;  
 	opts.seqwidth = seqwidth;
  opts.numright = true;  
  opts.numwidth = knumwidth;  
  seqkind= bioseq.getSeqtype();
  if ( seqkind == SeqInfo.kAmino) {
			//BioseqWriter.gJavaChecksum= false;  // FIXME !
			//BioseqWriter.gShortChecksum= false;
  	setChecksum(true);
			}
	}
		
	public void writeRecordEnd() {  
		//BioseqWriter.gJavaChecksum= true;  // FIXME !
		writeln("//"); 
		}

	public void writeSeq() // per sequence
	{
		// writeLoop();
		int i, j, nout= 0;
		boolean newline= true;
		if (bioseq.isBytes() && testbaseKind != kUseTester) {
			byte[] ba= bioseq.toBytes();
			for (i= 0; i < seqlen; i++) {
				if (newline) {
					for (j=0; j<ktab; j++) writeByte(' '); 
					newline= false;
					}

				writeByte( ba[offset+i]); 
				if (i % seqwidth == seqwidth-1) { 
		    	writeString( "  " + Fmt.fmt( i+opts.origin, knumwidth, knumflags));
					writeln(); newline= true; 
					}
				else if (i % kspacer == kspacer-1) writeByte(' ');
				}
			}
		else {
			for (i= 0; i < seqlen; i++) {
				if (newline) {
					for (j=0; j<ktab; j++) writeByte(' '); 
					newline= false;
					}

		   	char bc= bioseq.base(offset+i,fBasePart);
	      if (testbaseKind == kUseTester) bc= (char) testbase.outSeqChar( bc);
				if (bc>0) {
					writeByte( bc); 
					if ( (nout) % seqwidth == seqwidth-1) { 
			    	writeString( "  " + Fmt.fmt( nout+opts.origin, knumwidth, knumflags));
						writeln(); newline= true; 
						}
					else if ((nout) % kspacer == kspacer-1) 
						writeByte(' ');
					nout++;
					
					}
				}
			}
			
		if (!newline) {
			int tab= seqlen % seqwidth; 
			if (tab>0) {
				tab= seqwidth - tab;
				tab += (tab-1) / kspacer;
				}
			for (j=0; j<tab; j++) writeByte(' ');  // need to add missing spacers!
			writeString( "  " + Fmt.fmt( seqlen-1+opts.origin, knumwidth, knumflags));
			writeln();
			}
}

//ID   DMEST6A    standard; DNA; INV; 1754 BP.
//ID   EMBL       standard; DNA; UNC; 100 BP.
//ID   FASTA          STANDARD;      PRT;   100 AA.  << swissprot/amino
	protected void writeID() {
		writeString("ID   ");
		if (seqkind==SeqInfo.kAmino) {
			writeString( Fmt.fmt( idword, 15, Fmt.LJ));
			writeString(" STANDARD;      PRT; ");
	   	writeString( String.valueOf(seqlen));
			writeln(" AA.");
			}
		else {
			writeString( Fmt.fmt( idword, 11, Fmt.LJ));
			writeString(" standard; ");
			writeString( SeqInfo.getKindLabel(seqkind));  
			if (Debug.isOn) writeString("; debug");
			writeString("; UNC; ");
	   	writeString( String.valueOf(seqlen));
			writeln(" BP.");
			}
		}

	protected final void writeTitle() { writeString( "DE   "); writeln( seqid); }

//SQ   SEQUENCE   100 AA;  11907 MW;  5DB8D5B8 CRC32;    << swissprot/amino
//SQ   Sequence 400 BP; 104 A; 83 C; 130 G; 83 T; 0 other;

	protected void writeSeqStats(String cks) {	
		if (seqkind==SeqInfo.kAmino) {
			writeString( "SQ   SEQUENCE "); 
			writeString( Fmt.fmt( seqlen, 5, 0)); writeString(" AA;"); 
			writeString( Fmt.fmt( cks, 10, 0)); writeString(" CRC32;"); 
			writeln();
			}
		else {
			if (cks.length()>0) { writeString("CC   "); writeString(cks); writeln(" CRC32;"); }
			writeString( "SQ   Sequence "); writeString( String.valueOf(seqlen)); writeln(" BP;"); 
			}
		}

	public void writeDoc()
	{
		String cks= checksumString();
		int at= cks.indexOf(" checksum"); if (at>0) cks= cks.substring(0,at);
		if (seqdoc instanceof BioseqDoc) {
			EmblDoc doc;
			if (seqkind==SeqInfo.kAmino) doc= new SwissDoc((BioseqDoc)seqdoc);
			else doc= new EmblDoc((BioseqDoc)seqdoc);
			boolean doid= true;
			String docid= doc.getID();
			if (docid==null || 
				(! idword.startsWith( SeqFileInfo.gBlankSeqid) && ! docid.equals(idword) )  )
					{ writeID(); doid= false; }
			if (!doid && doc.getTitle()==null) writeTitle(); // should have, but not before id
			doc.replaceDocField( doc.kSeqlen, String.valueOf(seqlen) );
			if (cks.length()>0 ) doc.replaceDocField(doc.kChecksum, cks); //! Urk this ends up AFTER SQ line !
			 
			linesout += doc.writeTo(douts, doid);
				//^^ EmblDoc() now writes "SQ" line with stats - usually
			if (doc.getDocField(BioseqDoc.kSeqstats)==null) {
				if (seqkind!=SeqInfo.kAmino && doc.getDocField(BioseqDoc.kChecksum)!=null) cks= "";
				writeSeqStats(cks); // must have
				}
			}
		else {
			writeID();
			writeTitle();
			writeSeqStats(cks);
			}
	}
 
};