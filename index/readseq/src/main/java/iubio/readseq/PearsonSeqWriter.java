package iubio.readseq;



//public
class PearsonSeqWriter  extends BioseqWriter
{
	final static int seqwidth= 60; // up from default 50, jul'99
	
	public void writeRecordStart() {
		super.writeRecordStart();
 	opts.seqwidth = seqwidth; 
		}

	public void writeRecordEnd() { } // no extra newline!

	public void writeSeq() {  
		// writeLoop(); // ? replace w/ simpler one for this format? just dump seqq, 60/line
		int i, nout= 0;
		boolean newline= true;
		if (bioseq.isBytes() && testbaseKind != kUseTester) {
			byte[] ba= bioseq.toBytes();
	
			/*for (i= 0; i<seqlen; ) {
				int len= seqwidth;
				if (len+i>seqlen) len= seqlen-i;
				writeByteArray( ba, offset+i, len); // can't do with Writer - is really writeCharArray()
				i += len;
				writeln(); newline= true;
				}*/
			
			for (i= 0; i < seqlen; i++) {
				writeByte( (char)ba[ offset+i]); newline= false; 
				if (i % seqwidth == seqwidth-1) { 
					writeln(); newline= true; 
					}
				}
			
			}
		else {
			for (i= 0; i < seqlen; i++) {
		   	//char bc= (char) testbase.outSeqChar( bioseq.base(offset+i,fBasePart));
		   	char bc= bioseq.base(offset+i,fBasePart);
	      if (testbaseKind == kUseTester) bc= (char) testbase.outSeqChar( bc);
				if (bc>0) {
					writeByte( bc); newline= false; nout++;
					if ( (nout-1) % seqwidth == seqwidth-1) { 
						writeln(); newline= true; 
						}
					}
				}
			}
		if (!newline) writeln();
	}
		
		
	public void writeDoc() {
 	writeString(">");
 	writeString(seqid);
 	// add some other doc here if available - EMBL/GB def line
		if (seqdoc instanceof BioseqDoc) {
			String title=  ((BioseqDoc)seqdoc).getTitle();
			if (title!=null) {
				title= title.replace('\n',' ').replace('\r',' '); // make sure no newlines
			 	writeString(" "); writeString( title ); 
			 	}
			}
 	writeString(" ");
 	writeString( String.valueOf(seqlen));
 	writeString(" bp ");
 	writeln(checksumString());
		//linesout += 1;
		}
};

