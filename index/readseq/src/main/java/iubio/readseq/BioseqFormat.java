// BioseqFormat.java
// d.g.gilbert, 1990-1999

	
package iubio.readseq;

import java.io.*;

import flybase.OpenString;
import flybase.FastProperties;
import flybase.AppResources;
import Acme.Fmt;

	
public class BioseqFormat
	implements BioseqIoIface
	//implements BioseqFormatIface
{
	protected int formatId;
	
	public int formatID() { return formatId; }
	public void setFormatID(int id) { formatId= id; } //? Readseq sets id?
	public String formatName() { return "no-format"; }
	public String formatSuffix() { return ".seq"; }
	public String contentType() { return "biosequence/*"; }

	public String formatDocName() { 
		String cname= this.getClass().getName();
  	cname = cname.replace('.', '/');
  	cname = cname + ".html"; //docSuffix; //?
		return cname; 
		}

	public boolean canread() { return false; }
	public BioseqReaderIface newReader() { return null; }

	public boolean canwrite() { return false; }
	public BioseqWriterIface newWriter() { return null; }

	public boolean interleaved() { return false; }
	public boolean needsamelength() { return false; }
	public boolean hasdoc() { return false; }
	public boolean hasseq() { return true; } // new may01 for FlatFeat/GFF/ feature files

	public void setVariant(String varname) { }

		// format info, if available
	public InputStream getDocument() 
	{
		String cname= AppResources.global.findPath(formatDocName());
		if (cname==null) return null;
		return AppResources.global.getStream(cname);
	}
		
		// format testing =============================	
	protected int formatLikelihood, recordStartline;
	public void formatTestInit() { formatLikelihood= 0; recordStartline= 0; }
	public boolean formatTestLine( OpenString line, int atline, int skiplines) { return false; }
	public int  formatTestLikelihood() { return formatLikelihood; }
	public int  recordStartLine() { return recordStartline; }
}


/**
 * Bioseq data format registry 

// 20may01 - moved to own .java file -- thank you Sun javac for this big waste of my time
// public class BioseqFormats {};
// 21may01 - moved back - thank you to me for writing split2javac.pl 

 */




/*
public interface BioseqFormatIface
	extends BioseqIoIface
{
	public String formatName();
	public String formatSuffix();
	public String contentType();

	public boolean canread();
	public BioseqReaderIface newReader(); 

	public boolean canwrite();
	public BioseqWriterIface newWriter(); 

	public boolean interleaved();
	public boolean needsamelength();
	public boolean hasdoc(); //? has more than name, seq, other odd info
	//? public boolean canwritemany(); // gcg holds only one seq/file 

		// format testing =============================	
	public void formatTestInit();
	public boolean formatTestLine( OpenString line, int atline, int skiplines);
		//? add formatTestChunk() ?
	public int  formatTestLikelihood(); // 0..100
	public int  recordStartLine(); //?? start line of data in file, from formatTest()
}
*/
	
