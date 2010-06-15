package iubio.readseq;

import java.text.SimpleDateFormat;
import java.util.Date;





//public
class GcgSeqWriter  extends BioseqWriter
{
	protected String datestr;

	protected long calculateChecksum()
	{
		return GCGchecksum( bioseq, offset, seqlen);
	}

	public void writeRecordStart()
	{
		super.writeRecordStart();
		setChecksum(true);
		//testbase= new TestGcgBase(); testbaseKind= kUseTester;
		this.setOutputTranslation( new GcgOutBase( this.getOutputTranslation()));

		/*
		//dupSeqForOutput= true; // due to bioseq.replace()
		if (dupSeqForOutput) {
			Biobase[] bb= ((Bioseq) seqob).dup();
			this.bioseq= new Bioseq( bb);
			}
  if (BaseKind.indelHard != '.' && bioseq.indexOf( BaseKind.indelHard)>=0) {
  	bioseq= bioseq.clone();
  	String oldb= new String( { indelHard, indelSoft, indelEdge } );
  	String newb= new String( "..." );
  	bioseq.replace(seqlen, oldb, newb);  
  	//bioseq.replace(seqlen, BaseKind.indelHard, '.');   
			} */
 	opts.spacer = 10;
  opts.numleft = true;
	}
		
	//protected void writeRecordEnd() { writeln(); }
	
	
	public void writeDoc()
	{
		writeln( seqid );
		if (seqdoc instanceof BioseqDoc) {
			String title=  ((BioseqDoc)seqdoc).getTitle();
			if (title!=null) writeln( title );  
			}
  if (datestr==null) {
		SimpleDateFormat sdf= new SimpleDateFormat("MMM dd, yyyy  HH:mm"); //August 28, 1991  02:07
		datestr= sdf.format(new Date());
		}
		writeString( "    " + idword + "  Length: " + seqlen );
		writeln( "  " + datestr + "  Check: " + checksum + "  ..");
	}
		
};