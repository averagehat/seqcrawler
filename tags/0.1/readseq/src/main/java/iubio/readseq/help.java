//iubio/readseq/help.java
//split4javac// iubio/readseq/readseqrun.java date=28-Jun-2002

// iubio.readseq readseqrun.java

/*
main calls
	done: iubio.readseq.run => classic readseq commandline options
	done: iubio.readseq.app => window/gui interface (swing-based)
	done: iubio.readseq.cgi => HTTP server cgi interface 
	
	use IBM trick - put real main in package and wrapper main w/o package ?
	other uses:  rs= new iubio.readseq.run(); rs.getargs(args); rs.run(); -- support Runnable iface?
*/


package iubio.readseq;


import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Properties;

import iubio.bioseq.SeqRange;
import iubio.bioseq.SeqRangeException;
import iubio.readseq.*;

import flybase.Args;
import flybase.Debug;
import flybase.Utils;
import flybase.AppResources;
import flybase.FastVector;
import flybase.Environ;


//split4javac// iubio/readseq/readseqrun.java line=36
public class help
{
	public static String kReadseq2Help = "rez/Readseq2-help.html";
	
	public static void main(String[] args)  { 
		help h= new help(args); 
		}
		
	public boolean dohtml;
	public PrintStream out;

	public help() { help(null, System.out); }
	public help( String[] args) { help(args, System.out); }
	public help( PrintStream out) { help(null, out); }
	public help( String[] args, PrintStream out) { help(args, out); }
	
	public help( boolean noaction, boolean dohtml, PrintStream out) {
		this.dohtml= dohtml;
		this.out= out;
		 }
		
	public void help(String[] args, PrintStream out) {
		if (out==null) out= System.out;
		this.out= out;
		boolean needhelp= false, morehelp= false, formathelp= false;
		if (args!=null) for (int i=0; i<args.length; i++) {
			String arg= args[i].toLowerCase();
			if (arg.startsWith("-")) arg= arg.substring(1);
			if (arg.indexOf("html")>=0) { dohtml= true; }
			// use "o" to set output file?
			else if (arg.startsWith("h")) { needhelp= true; }
			else if (arg.startsWith("m")) { morehelp= true; }
			else if (arg.startsWith("f")) { formathelp= true; }
			}
			
		if (needhelp || !(morehelp||formathelp)) mainHelp();
		if (morehelp) extraHelp();
		if (formathelp) formatHelp();
		}
		
	public void mainHelp() {
		out.println(" For basic help     : java -cp readseq.jar help ");
		out.println(" For more details   : java -cp readseq.jar help more ");
		out.println(" For format details : java -cp readseq.jar help formats ");
		String homeurl= Environ.gEnv.get("APP_SOURCE_URL");
		if (homeurl.length()>0) {
			out.println("  Home of this package");
			if (dohtml) out.println("    <a href=\""+homeurl+"\">"+homeurl+"</a>");
			else out.println("    " + homeurl);
			}
		out.println("Programs available in this package");
		rule();
		app.appusage(out);
		rule();
		cgi.cgiusage(out);
		rule();
		test.testusage(out);
		rule();
		run.usage(out);
		rule();
	}
	

	public void extraHelp() {  
    String helps= AppResources.global.getData( kReadseq2Help);
    if (helps==null) out.println("No extra help found in "+kReadseq2Help);
 		else if (dohtml) out.println( helps);
 		else out.println( Utils.htmlToText(helps));
		}
		
	boolean  didfmthead= false;
	
	public void formatHelp() 
	{
		if (dohtml) {
			out.println("<HTML><TITLE>Readseq: Formats</TITLE><BODY>");
			out.println("<B>Known biosequence formats:</B><BR>");

			// do toc
			out.println( BioseqFormats.getInfo( 0, "html-header")); //didfmthead= true;
			for (int i=1; i<=BioseqFormats.nFormats(); i++) {
				String s= BioseqFormats.getInfo(i, "html-command-line-toc");
				out.println(s);
				}
			out.println( BioseqFormats.getInfo( 0, "html-footer"));
			rule(); out.println("<P>");
				
			for (int i=1; i<=BioseqFormats.nFormats(); i++) {
				//out.println( BioseqFormats.getInfo(i, "html-command-line"));
				formatDoc(i);
				}
			//out.println( BioseqFormats.getInfo( 0, "html-footer"));
			out.println("&nbsp;&nbsp; (Int'leaf = interleaved format; Features = documentation/features are parsed)");
			}
		else {
			out.println("Known biosequence formats:");
			//out.println( BioseqFormats.getInfo( 0, "space-header")); didfmthead= true;
			for (int i=1, n= BioseqFormats.nFormats(); i<=n; i++) {
				//out.println( BioseqFormats.getInfo(i, "space-command-line"));
				formatDoc(i);
				}
			out.println( BioseqFormats.getInfo( 0, "space-footer"));
			out.println("   (Int'leaf = interleaved format; Features = documentation/features are parsed)");
			}	
		//rule();
	}

	public void formatDocEnd() {
		out.println( BioseqFormats.getInfo( 0, (dohtml) ?  "html-footer" : "space-footer"));
 		}
 		
	public void formatDoc(int ifmt) {
		String s;
		if (!didfmthead) {
			s= BioseqFormats.getInfo( 0, (dohtml) ?  "html-header" : "space-header") ; //didfmthead= true;
			if (dohtml) s= "<A name=\"fmt"+ifmt+"\">&nbsp</a>" + s;  //-href
			out.println(s);
			}
		else {
			s= BioseqFormats.getInfo( 0, (dohtml) ?  "html-header1" : "space-header1") ; 
			out.println(s);
 			}
			
		s= BioseqFormats.getInfo(ifmt, (dohtml) ?  "html-command-line" : "space-command-line" );//-href
		out.println(s);
		if (dohtml)
				out.println( BioseqFormats.getInfo( 0, "html-footer")); //didfmthead= false;
				//out.println("<tr><td colspan=9>");
		else out.println();
		
		BioseqFormat fmt= BioseqFormats.bioseqFormat(ifmt);
	  InputStream ins= fmt.getDocument(); // all html ?
		if (ins==null)  out.println("No format details ");
		else try {
				// either read by-line  or do newline translation on string
    	String lf = System.getProperty("line.separator");
    	StringBuffer sb= new StringBuffer();
			DataInputStream dis= new DataInputStream(ins);
			while (( s= dis.readLine())!=null) { sb.append(s); sb.append(lf); }
			s= sb.toString();
			/*
			int len= ins.available();
			byte[] buf= new byte[len];
			len= ins.read(buf);
			s= new String(buf, 0, len);
			*/
			int it= s.indexOf("<BODY>"); if (it>0) s= s.substring(it+"<BODY>".length());
			it= s.indexOf("</BODY>"); if (it>0) s= s.substring(0, it);
			
			if (dohtml) out.println(s);
			else out.println( Utils.htmlToText(s) );
			
			//DataInputStream dis= new DataInputStream(ins);
			//while (( s= dis.readLine())!=null) out.println(s);
			ins.close();
			} catch (IOException e) {}
		rule();
		//if (dohtml) out.println("</td></tr>");
	}
		
				
				
	public void rule() {	
		if (dohtml) out.print("<HR width=\"50%\" align=left>"); else for (int i= 0; i<30; i++) out.print('-'); 
		out.println(); 
		}
	
}


/**
  Readseq command-line driver program <p>
  iubio.readseq.run => classic readseq commandline options
  
 <pre>
  Minimal programming calls for input to output conversion
 
import java.io.*;
import iubio.readseq.*;
public class testrseq {
	public static main( String[] args) {
		try {
		int outformat= BioseqFormats.formatFromName("fasta");
		BioseqWriterIface seqwriter= BioseqFormats.newWriter(outformat);  
		seqwriter.setOutput( System.out);
		seqwriter.writeHeader();  
		Readseq	rd= new Readseq();
		for (int i=0; i &lt; args.length; i++) {
			rd.setInputObject( args[i] );
			if ( rd.isKnownFormat() && rd.readInit() )  
				rd.readTo( seqwriter); 
			}
		seqwriter.writeTrailer();
		}
		catch (Exception ex) { ex.printStackTrace(); }
	}
}
	</pre>

	@author  Don Gilbert
	@version 	December 1999
*/

