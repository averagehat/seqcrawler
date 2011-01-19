//iubio/readseq/BioseqFormats.java
//split4javac// iubio/readseq/BioseqFormat.java date=06-Jun-2001

// BioseqFormat.java
// d.g.gilbert, 1990-1999

	
package iubio.readseq;

import java.io.*;

import flybase.OpenString;
import flybase.FastProperties;
import flybase.AppResources;
import Acme.Fmt;

	
//split4javac// iubio/readseq/BioseqFormat.java line=73
public class BioseqFormats
{
	public static int 
		kNoformat= -1, // format not tested 
		kUnknown= 0,	 // format not determinable  
		kMaxFormat = 30, // initial size

			// jun01 -- keep these numbers to preserve classic commandline -f=# option
		kIG = 1, 
		kGenBank = 2, 
		kNBRF = 3, 
		kEMBL = 4, 
		kGCG = 5, 
		kStrider = 6, 
		kFitch = 7, 
		kPearson = 8, 
		kZuker = 9, 
		kOlsen = 10, 
		kPhylip2 = 11, 
		kPhylip4 = 12, kPhylip = kPhylip4, 
		kPlain = 13, 
		kPIR = 14, 
		kMSF = 15, 
		kASN1 = 16, 
		kPAUP = 17, 
		kPretty = 18  
  	;

	public static String propname= "BioseqFormats"; 
		
	static { 
  	loadClasses(propname);//BioseqFormats.class.getName()
		}
 
	static int nforms;
	static BioseqFormat[] formats;
	
	public static int nFormats() { return nforms; }
	
  public static int register( BioseqFormat formatinfo) 
  { 
		if (formats==null) {
			formats= new BioseqFormat[ kMaxFormat];
			nforms= 0;
			formats[0]= new BioseqFormat(); //? null format?
			}
  	if (formats.length+1 <= nforms) {
  		int maxforms= nforms * 2;
  		BioseqFormat[] nf= new BioseqFormat[maxforms];
  		System.arraycopy( formats, 0, nf, 0, nforms);
  		formats= nf;
  		}
  	formats[++nforms]= formatinfo; //! starts at 1 not 0
  	formatinfo.setFormatID( nforms); //?
  	return nforms;
	}
	
	public static int indexFromFormat(int format) {
		return format;
		}
	
	public static BioseqFormat bioseqFormat( int format) { 
  	if (format >=0 && format <= nforms) return formats[format]; 
		else return null;
		}

		// 
		// Testing formats
		//
	public static void formatTestInit() {
		for (int i=0; i<= nforms; i++) formats[i].formatTestInit(); 	}
		
	public static int recordStartLine(int format) {
		return formats[format].recordStartLine(); }
		
	public static int formatTestLikelihood(int format) {
		return formats[format].formatTestLikelihood(); }
		
	public static boolean formatTestLine( int format, OpenString line, int atline, int skiplines) {
		return formats[format].formatTestLine( line, atline, skiplines); }
		
	public static boolean formatTestLine( int format, String line, int atline, int skiplines) {
		return formats[format].formatTestLine( new OpenString(line), atline, skiplines); }


  public static String formatName(int format) { 
		try { return formats[format].formatName(); }
		catch (Exception e) { return ""; }
  	}
  			
		// shouldn't this be same as formatName() ?
  public static String formatNameFromIndex(int format) 
  { 
  	if (format >=0 && format <= nforms) {
  		String fn= formats[format].formatName();
  		boolean noread= !formats[format].canread();
  		boolean nowrite= !formats[format].canwrite();
  		if (noread && nowrite) fn += " [no read/write]";
  		else if (noread) fn += " [write only]";
   		else if (nowrite) fn += " [read only]";
   		
   		return fn;
 			}
  	else return "";
  }

	final static String spc(boolean spaced, String val, int wid, int flag) {
		if (spaced) return val= Fmt.fmt(val, wid, flag);
		else return val;
		}
		
	final static String isyes(boolean b) { if (b) return "yes "; else return "-- "; }
	
	public static String tablestr= "<TABLE bgcolor=\"white\" border=0 CELLSPACING=0 CELLPADDING=4>";

	public static String getInfo(int format, String style)
	{
 		String delim= "\t";
 		int w= 0;
 		boolean dohtml= false, dospace= false, dotoc= false, dohref= false, doshort= false, doread= true, dowrite= true;
 		if (style.indexOf("tab")>=0) delim= "\t";
 		else if (style.indexOf("space")>=0) { dospace= true; delim= "  "; }
 		else if (style.indexOf("html")>=0) { dohtml= true; delim= "</TD><TD>"; }
 		else if (style.indexOf("short")>=0) { doshort= true; dospace= true; delim= "  "; }
 		if (style.indexOf("noread")>=0) { doread= false; }
 		if (style.indexOf("nowrite")>=0) { dowrite= false; }
 		if (style.indexOf("toc")>=0) { dotoc= true; }
 		if (style.indexOf("href")>=0) { dohref= true; }

		StringBuffer sb= new StringBuffer();
		if (style.indexOf("header")>=0) {
			if (dohtml) { 
				if (style.indexOf("header1")<0) sb.append(tablestr); 
				sb.append("<TR bgcolor=\"#99CCFF\"><TH align=left>");  
				delim= "</TH><TH align=left>"; 
				}
			sb.append(spc(dospace,"ID", 3, 0)); sb.append(delim); 
			sb.append(spc(dospace,"Name", 14, Fmt.LJ)); sb.append(delim); 
			if (doread) { sb.append(spc(dospace,"Read", 5, 0)); sb.append(delim); }
			if (dowrite) { sb.append(spc(dospace,"Write", 5, 0)); sb.append(delim); }
			if (!doshort) {
			sb.append(spc(dospace,"Int'leaf", 8, 0)); sb.append(delim); 
			}
			sb.append(spc(dospace,"Features", 8, 0)); sb.append(delim); 
			sb.append(spc(dospace,"Sequence", 8, 0)); sb.append(delim); 
			if (!doshort) {
			sb.append(spc(dospace,"Suffix", 6, Fmt.LJ));  sb.append(delim); 
			sb.append(spc(false,"Content-type", 19, Fmt.LJ)); 
			}
			if (dohtml) { sb.append("</TH></TR>"); }
			}
		else if (style.indexOf("footer")>=0) {
			if (dohtml) { sb.append("</TABLE>"); }
			}
		else {
	 		BioseqFormat fmt= bioseqFormat( format);
	 		if (fmt != null) {
	 			if (!dowrite && doread && !fmt.canread()) ; // break ENDFMT;
	 			else if (!doread && dowrite && !fmt.canwrite()) ; // break ENDFMT;
	 			else {
					String sid= String.valueOf(fmt.formatID());
					if (dohtml) { sb.append("<TR><TD>");
						if (dotoc) sb.append("<A href=\"#fmt"+sid+"\">"); 
						else if (dohref) sb.append("<A name=\"fmt"+sid+"\">"); 
					 	}
					sb.append(spc(dospace, sid, 3, 0)); 
					if (dotoc||dohref) sb.append("</A>"); 
					sb.append(delim); 
					sb.append(spc(dospace, fmt.formatName(), 14, Fmt.LJ)); sb.append(delim); 
					if (doread) { sb.append(spc(dospace, isyes(fmt.canread()), 5, 0)); sb.append(delim); }
					if (dowrite) { sb.append(spc(dospace, isyes(fmt.canwrite()), 5, 0)); sb.append(delim); }
					if (!doshort) {
					sb.append(spc(dospace, isyes(fmt.interleaved()), 8, 0)); sb.append(delim); 
					}
					sb.append(spc(dospace, isyes(fmt.hasdoc()), 8, 0)); sb.append(delim); 
					sb.append(spc(dospace, isyes(fmt.hasseq()), 8, 0)); sb.append(delim); 
					if (!doshort) {
					sb.append(spc(dospace, fmt.formatSuffix(), 6, Fmt.LJ));  sb.append(delim); 
					sb.append(spc(false, fmt.contentType(), 19, Fmt.LJ)); 
					}
					if (dohtml) { sb.append("</TD></TR>"); }
					}
				}
			}
		//if (dohtml) { sb.append("</TABLE>"); }
 		return sb.toString();
	}

  public static String formatSuffixFromIndex(int index)  { 
  	return formatSuffix(index);
  	}

  public static String formatSuffix(int format) { 
  	if (format >=0 && format <= nforms) return formats[format].formatSuffix();
  	else return "";
  	}


  public static String contentTypeFromIndex(int index) { 
  	return contentType(index);
  	}

  public static String contentType(int format) { 
  	if (format >=0 && format <= nforms) return formats[format].contentType();
  	else return "biosequence/*"; //??
  	}

	public static int formatFromIndex(int format) {
  	if (format >=0 && format <= nforms) return formats[format].formatID();
  	else return kUnknown;
		}
	
	public static int getFormatId(String format) 
	{
		int fid= kUnknown;
		if (format==null || format.length()<1) return fid;
		int at= format.indexOf('|'); 
		if (at>0) format= format.substring(0,at);
		format= format.trim();
		if ( Character.isDigit( format.charAt(0))) 
			try { fid= Integer.parseInt(format); } 
			catch (Exception e) { fid= kUnknown; }
		if (fid == kUnknown) fid= formatFromContentType(format);
		if (fid == kUnknown) fid= formatFromName(format);
		return fid;
	}

  public static int formatFromName(String name) 
  {
		if (name!=null && name.length()>1) 
		 for (int i=1; i<=nforms; i++) {
			String form2= null, form1= new String(formats[i].formatName());
			int at;
			do {
		 	  at= form1.indexOf('|'); 
		 	  if (at>0) {
		 		  form2= form1.substring(at+1).trim(); 
		 		  form1= form1.substring(0,at).trim();
		 		  }
		 		if (name.equalsIgnoreCase(form1)) return formats[i].formatID();
		 		form1= form2;
		 	} while (at > 0);
			}
			// try suffix if not found?
		for (int i=0; i<=nforms; i++) {
			String sufx= formats[i].formatSuffix(); 
			if (sufx.startsWith(".")) sufx= sufx.substring(1);
			if (name.equalsIgnoreCase(sufx)) return formats[i].formatID();
			}
		return kUnknown;
  }
  
  public static int formatFromContentType(String content) 
  {
		if (content!=null && content.length()>1) 
			for (int i=1; i<=nforms; i++) {
				String ct= formats[i].contentType();
				if (content.equalsIgnoreCase(ct)) return formats[i].formatID();
				}
		return kUnknown;
  }

 	public static boolean canread( int format) {
		try { return formats[format].canread(); }
		catch (Exception e) { return false; }
 		}
 	
 	public static boolean canwrite( int format) {
		try { return formats[format].canwrite(); }
		catch (Exception e) { return false; }
 		}
 	
	public final static BioseqWriterIface newWriter( int format) { // dang, do we need to know #seqs?
		return newWriter( format, 2); }
		
	public static BioseqWriterIface newWriter( int format, int nseqs)
	{
		try { 
			if (!formats[format].canwrite()) return null;
			BioseqWriterIface wtr= formats[format].newWriter(); 
			wtr.setFormatID(format);
			return wtr;
			}
		catch (Exception e) { return null; }
	}
	
	public static BioseqReaderIface newReader( int format)
	{
		try { 
			if (!formats[format].canread()) return null;
			BioseqReaderIface rdr= formats[format].newReader(); 
			rdr.setFormatID(format);
			return rdr;
			}
		catch (Exception e) { return null; }
	}    
	     
	     
	     
//============== inits ======================
	
  
  protected static void loadClasses(String propname)
  {
  	String pname= System.getProperty( propname, propname);
  	String listname= System.getProperty( "formats", "formats");
		FastProperties props = new FastProperties(); //default props?
		props.loadProperties(pname);

		String c;
		String cs= props.getProperty(listname);
		//System.err.println("formats="+cs); // Debug
		if (cs == null) return; //?
		int at0= 0;
		while (at0>=0) {
			int at= cs.indexOf(',',at0);
			if (at>at0) { c= cs.substring(at0,at); at0= at+1; }
			else { c= cs.substring(at0); at0= -1; }
			c= c.trim();
			if (c.length()>0) try {
				Class rc= Class.forName(c);
				register( (BioseqFormat)rc.newInstance());
				}
			catch (Exception e) {		
				System.err.println("Error loading class: '"+c+"'");
				//e.printStackTrace();
				}
			}
	}
	
};



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
	

