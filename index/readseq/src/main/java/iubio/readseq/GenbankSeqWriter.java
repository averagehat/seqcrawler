package iubio.readseq;

import iubio.bioseq.SeqInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

import Acme.Fmt;


//public
class GenbankSeqWriter  extends BioseqWriter
{
	final static int seqwidth= 60, ktab= 0, kspacer= 10, knumwidth= 9, knumflags= 0;  
	String datestr;
	int seqkind;
	
	public void writeRecordStart()
	{
		super.writeRecordStart();
 	//opts.tab = ktab;    
  opts.spacer = kspacer;  
 	opts.seqwidth = seqwidth;
  opts.numleft = true;
  opts.numwidth = knumwidth;  
  seqkind= bioseq.getSeqtype();
	}
	
	public void writeRecordEnd() { writeln("//"); }

//123456789
//ORIGIN      
//      1 cagcagccgc ggtaatacca gctccaatag cgtatattaa agttgttgtg gttaaaaagc
//     61 tcgtagttgg atctcagatc cggagctgcg gtccaccgcc cggtggttac tgtagcgacc
////

	public void writeSeq() // per sequence
	{
		// writeLoop();
		int i, j, nout= 0;
		int origin= opts.origin;
		boolean newline= true;
		if (bioseq.isBytes() && testbaseKind != kUseTester) {
			byte[] ba= bioseq.toBytes();
			for (i= 0; i < seqlen; i++) {
				if (newline) {
		    	writeString( Fmt.fmt( i+origin, knumwidth, knumflags));
		    	writeByte(' ');
					newline= false;
					}
				writeByte( ba[offset+i]); 
				if (i % seqwidth == seqwidth-1) { writeln(); newline= true; }
				else if (i % kspacer == kspacer-1) writeByte(' ');
				}
			}
		else {
			for (i= 0; i < seqlen; i++) {
				if (newline) {
		    	writeString( Fmt.fmt( nout+origin, knumwidth, knumflags));
		    	writeByte(' ');
					newline= false;
					}

		   	char bc= bioseq.base(offset+i,fBasePart);
	      if (testbaseKind == kUseTester) bc= (char) testbase.outSeqChar( bc);
				if (bc>0) {
					writeByte( bc); nout++;
					if ( (nout-1) % seqwidth == seqwidth-1) { 
						writeln(); newline= true; 
						}
					else if ( (nout-1) % kspacer == kspacer-1) 
						writeByte(' ');
					}
				}
			}
			
		if (!newline) {
			writeln();
			}
}


	protected void writeID() {
		//LOCUS       AF005656     1504 bp    DNA             UNA       02-JAN-1999
		writeString("LOCUS       ");
		writeString( Fmt.fmt( idword, 11, Fmt.LJ));
		writeString( Fmt.fmt( seqlen, 7, 0));
		if (seqkind==SeqInfo.kAmino)  writeString(" aa "); 
		else writeString(" bp ");
		String skind= SeqInfo.getKindLabel(seqkind);
		writeString( Fmt.fmt( skind, 6, 0));
		writeString("             UNA       ");
  if (datestr==null) {
  	SimpleDateFormat sdf= new SimpleDateFormat("dd-MMM-yyyy");
  	datestr= sdf.format(new Date());
  	}
		writeString( datestr);
		writeln();
		}
		
	protected final void writeTitle() { writeString("DEFINITION  "); writeln( seqid); }
			
	public void writeDoc()
	{
		String cks= checksumString();
		if (seqdoc instanceof BioseqDoc) {
			GenbankDoc doc= new GenbankDoc((BioseqDoc)seqdoc); 
			if (seqkind==SeqInfo.kAmino) doc.setAmino(true);
			boolean doid= true;
			String docid= doc.getID();
			if (docid==null || 
				(! idword.startsWith( SeqFileInfo.gBlankSeqid) && ! docid.equals(idword) )  )
					{ writeID(); doid= false; }
			if (!doid && doc.getTitle()==null) writeTitle(); // should have, but not before id
			doc.replaceDocField( doc.kSeqlen, String.valueOf(seqlen) );
			if (cks.length()>0 ) doc.replaceDocField(doc.kChecksum, cks); //! Urk this ends up AFTER BASE line !
			linesout += doc.writeTo(douts, doid);
			}
		else {
			writeID();
			writeTitle();
			if (cks.length()>0) { writeString("COMMENT     "); writeln(cks); }
			//writeString("BASE COUNT        0 a      0 c      0 g      0 t      0 others");
			}
		writeln( "ORIGIN      "); // always do here - never in doc
	}
	
};
