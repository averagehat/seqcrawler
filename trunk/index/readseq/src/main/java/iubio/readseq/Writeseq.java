// Writeseq.java
// d.g.gilbert, 1990-1999


package iubio.readseq;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

//import iubio.bioseq.Bioseq;

import flybase.OpenString;

 


/**
 * Writeseq is public wrapper for classes of BioseqWriter ancestry <p>
 * collect and write a batch of sequences to output stream
 *
 * @author  Don Gilbert
 * @version 	Jul 1999
 * @see iubio.readseq.Readseq
 */

public class Writeseq
	implements Enumeration //?
{
	protected Vector seqfilevec= new Vector();
	protected int fAt;
	protected int format;
	protected int minbases;
	protected boolean sizesDiffer = false;
	protected boolean fWriteMask= SeqFileInfo.gWriteMask;

  protected BioseqWriterIface writer;
	protected BioseqFormat former;  
	

	public Writeseq() { 
		this( BioseqFormats.kNoformat);
		}
		
	public Writeseq(int format) { 
		setFormat(format);
		}
		
	public final int getFormat() { return format; }
	public void setFormat(int format) {
		this.format= format;  
		former= BioseqFormats.bioseqFormat(format);  
		if (writer!=null && writer.formatID() != format) writer= null;
		}
		
	public BioseqWriterIface getWriter() { return writer; }
	public BioseqFormat getBioseqFormat() { return former; }
		
	public boolean canwrite() {
		if (writer!=null) return true;  
		else if (former!=null) return former.canwrite();
		else return false;
		}

		//
		// Data accessors -- want all these? -- same as for Readseq
		//

	public SeqFileInfo nextSeq() { 
		if (moreresults()) return (SeqFileInfo) seqfilevec.elementAt(fAt++); 
		else return null; 
		}
	public Object result() { if (moreresults()) return seqfilevec.elementAt(fAt++); else return null; }
	public boolean moreresults() 	{ return (fAt < seqfilevec.size()); }
	public int atresult() 				{ return fAt;  }
	public int nresults() 				{ return seqfilevec.size();  }
	public Vector allresults() 		{ return seqfilevec;  }
	public void restartresults() 	{ fAt= 0; }
	public void removeresults()   { fAt= 0; seqfilevec.removeAllElements(); }

	 	//
		// Enumeration iface
		//
		
	public boolean hasMoreElements() { 
		if (moreresults()) return true;
		else return false; //canReadMore(); 
		}
		
	public Object nextElement() { 
		if (moreresults()) return this.result(); 
		//if (readNext()) return this.result(); 
		return null;
		}

	 	//
		// add data to write
		//
		
	public void addSeq( SeqFileInfo si) 
	{
		if (minbases<=0) minbases= si.seqlen; // ?? temp fix problem w/ empty seq
		else {
		 	if (si.seqlen!=minbases) sizesDiffer= true;
		 	minbases= Math.min( si.seqlen, minbases);
		 	}
		seqfilevec.addElement( si);
	}

	public void addSeq( Object aSeq, String id, int start, int nbases, 
										  Object seqdoc, boolean hasmask) 
	{
		SeqFileInfo sa= new SeqFileInfo(); //(SeqFileInfo) this.si.clone();
		sa.seq= aSeq;
		sa.seqdoc= seqdoc;
		sa.offset= start;
		sa.seqlen= nbases;
		sa.seqid= id;
		sa.hasmask= hasmask;
		addSeq( sa);
	}
	 
	public void setWriteMask(boolean turnon) { fWriteMask= turnon; }

	public void close() throws IOException {
		if (writer!=null) { writer.close(); } // writer= null; ??
		}

		// was writeInit()
	public final void open( File f, int outFormat, int nSeqs) throws IOException {
		open( new FileWriter( f), outFormat, nSeqs);
		}

	/*public void open( OutputStream outs, int outFormat)  throws IOException {
		open( new OutputStreamWriter( outs), outFormat, -1);
		}*/

	public void open( Writer outs, int outFormat, int nSeqs) throws IOException 
	{
		if (outFormat <= BioseqFormats.kUnknown) {
			//return false;
			throw new ReadseqException("Unknown BioseqWriter format: "+outFormat);
			}
		setFormat( outFormat); //if (outFormat != this.format) writer= null;
		if (writer==null) writer= BioseqFormats.newWriter(outFormat, nSeqs); 
				//writer= BioseqFormats.newWriter( );
		if (writer==null) throw new ReadseqException("Null BioseqWriter"); //return false;

		writer.setOutput( outs);
		minbases= 0;
		sizesDiffer= false;
		removeresults();
	}
		
		// was writeLoop
	public boolean write() throws IOException //?
	{
		restartresults();
		if (moreresults()) {
			if (writer==null) throw new ReadseqException("Null BioseqWriter"); //return false;
			writer.setNseq( nresults());
			if (sizesDiffer && former.needsamelength() )  //former.interleaved() || 
				setSameSize();
			writer.writeHeader();
			while (moreresults()) {
				SeqFileInfo si= nextSeq();
				if (writer.setSeq( si)) writer.writeSeqRecord();
				if (fWriteMask && writer.setMask(si, si.gMaskName)) writer.writeSeqRecord();
				//if (writer.error()!=0) return false;
				}
			writer.writeTrailer();
			return true;
			}
		return false;
	}

	protected void setSameSize()
	{
		restartresults();
		while (moreresults()) {
			SeqFileInfo si= nextSeq();
			si.seqlen= minbases;
			}
		restartresults();
	}

} // Writeseq
