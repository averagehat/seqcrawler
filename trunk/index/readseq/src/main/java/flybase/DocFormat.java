// flybase/DocFormat.java
// d.g.gilbert 


package flybase;

import java.io.*;
import java.net.*;


public class DocFormat 
{
	// ! revise this sometime 	
	protected int format;
	protected String mimetype;
	
	public final static int 
			TEXT = 1230,
			DATA = 1231,
			PICT = 1232,
			POSTSCRIPT = 1233,
			JPEG = 1234,
			GIF = 1235,
			HTML = 1236,
			PRINTJOB = 1237,
			PDF= 1238
			;

	public DocFormat() {}
	
	public DocFormat(int format) { 
		this.format= format; 
		this.mimetype= mycontentType(format);
		}
		
	public DocFormat(String mimetype) { 
		this.mimetype= mimetype;
		format= myformatFromContentType(mimetype); 
		}
		
	public DocFormat(File f) { 
		mimetype= guessContentType(f);
		format= myformatFromContentType(mimetype); 
		}
		
	public DocFormat(InputStream ins) { 
		mimetype= guessContentType(ins);
		format= myformatFromContentType(mimetype); 
		}
		
	public int format() { return format; }
			
	public String contentType() {
	 	if (mimetype==null) mimetype= mycontentType(format); 
	 	return mimetype;
	 	}
	 	
	public String mycontentType(int format) {
	 	return contentType(format); 
	 	}
	public static String contentType(int format) {
		// pseudo MIME type
		switch (format) {
			case TEXT: return "text/plain";
			case HTML: return "text/html";
			case DATA: return "*/*";
			case PICT: return "image/pict";
			case PDF: return "application/pdf"; //?
			case POSTSCRIPT: return "application/postscript"; 
			case JPEG: return "image/jpeg";
			case GIF : return "image/gif";
			case PRINTJOB : return "application/printer";
			default  : return "*/*";
			}
		}

	public int myformatFromContentType(String mime) {
		return formatFromContentType(mime); 
		}
	public static int formatFromContentType(String mime) {
		if (mime==null || mime.length()==0) return 0; // or DATA ?
		mime= mime.toLowerCase();
		if (mime.equals("application/printer")) return PRINTJOB;
		if (mime.startsWith("text/html")) return HTML;
		if (mime.startsWith("text")) return TEXT;
		if (mime.endsWith("pict")) return PICT;
		if (mime.endsWith("jpeg")) return JPEG;
		if (mime.endsWith("jpg")) return JPEG;
		if (mime.endsWith("gif")) return GIF;
		if (mime.endsWith("pdf")) return PDF;
		if (mime.endsWith("postscript")) return POSTSCRIPT;
		if (mime.endsWith("/ps")) return POSTSCRIPT;
		if (mime.endsWith("printer")) return PRINTJOB;
		return 0; // or DATA ?
		}
	
	public static int formatFromSuffix(String suf) {
		if (suf==null || suf.length()==0) return 0; // or DATA ?
		suf= suf.toLowerCase();
		if (suf.endsWith("html")) return HTML;
		if (suf.endsWith("text")) return TEXT;
		if (suf.endsWith("txt")) return TEXT;
		if (suf.endsWith("pict")) return PICT;
		if (suf.endsWith("jpeg")) return JPEG;
		if (suf.endsWith("jpg")) return JPEG;
		if (suf.endsWith("gif")) return GIF;
		if (suf.endsWith("pdf")) return PDF;
		if (suf.endsWith("postscript")) return POSTSCRIPT;
		if (suf.endsWith("ps")) return POSTSCRIPT;
		return 0; // or DATA ?
		}
	
	public String suffix() { return mysuffix(format); }
	public String mysuffix(int format) { return suffix(format); }
	
	public static String suffix(int format) {
		switch (format) {
			case TEXT: return ".txt";
			case HTML: return ".html";
			case DATA: return ".data";
			case PICT: return ".pict";
			case PDF: return ".pdf";
			case POSTSCRIPT: return ".ps";
			case JPEG: return ".jpeg";
			case GIF: return ".gif";
			default: return "";
			}
		}
	
	public String macType() { return mymacType(format); }
	public String mymacType(int format) { return macType(format); }
	public static String macType(int format) {
		switch (format) {
			case TEXT: return "TEXT";
			case HTML: return "TEXT";
			case PICT: return "PICT";
			case PDF: return "PDF ";
			case POSTSCRIPT: return "POST"; // or TEXT
			case JPEG: return "JPEG";
			case GIF : return "GIFf";
			case DATA: // use app's default type
			default  : return null; // use this app's deftype! // "TEXT"; //??  
			}
		}

	public String macSire() { return mymacSire(format); }
	public String mymacSire(int format) { return macSire(format); }
	public static String macSire(int format) {
		// need some user options here
		switch (format) {
			case TEXT: return "R*ch"; // BBEdit
			case HTML: return "MOSS"; // Netscape
			case PICT: return "ttxt"; // TeachText
			case PDF: return "CARO";
			case POSTSCRIPT: return "????";  
			case JPEG: return "JVWR"; // JpegViewer
			case GIF : return "JVWR";
			case DATA: //  "????";
			default  : return null; // use app's defsire! // "ttxt";  
			}
		}
				
 	public String myguessContentType(File f)  
	{
		String mimetype= "*/*";
		try {
			FileInputStream ins= new FileInputStream(f);
			mimetype= myguessContentType(ins);
			ins.close();
			}
		catch (Exception e) {}
		return mimetype;
	}
 	public static String guessContentType(File f) 
	{
		String mimetype= "*/*";
		try {
			FileInputStream ins= new FileInputStream(f);
			mimetype= guessContentType(ins);
			ins.close();
			}
		catch (Exception e) {}
		return mimetype;
	}
	
 	public String myguessContentType(InputStream ins) { return guessContentType(ins); }
 	public static String guessContentType(InputStream ins) 
	{
		String mimetype= "*/*";
		try {
			if (!(ins instanceof BufferedInputStream)) ins = new BufferedInputStream( ins);
			mimetype= URLConnection.guessContentTypeFromStream( ins); // takes care to reset ins to start
			Debug.println(" guessContentType =" + mimetype);
			} 
		catch (Exception e) {
			Debug.println(" guessContentType failed: " + e.getMessage());
			}
		return mimetype;
	}
	
};
