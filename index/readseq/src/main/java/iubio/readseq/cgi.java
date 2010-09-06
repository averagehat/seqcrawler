//iubio/readseq/cgi.java
//split4javac// iubio/readseq/readseqcgi.java date=05-Jun-2001

// readseqcgi.java


package iubio.readseq;


import java.io.*;
import java.net.URL;

import iubio.readseq.*;

import flybase.Args;
import flybase.Environ;
import flybase.Debug;
import flybase.AppResources;
import flybase.FastVector;

/**
  Readseq HTTP server child application (common-gateway-inteface) <p>
  iubio.readseq.cgi => HTTP server cgi 
	@author  Don Gilbert
	@version 	July 1999
*/

//split4javac// iubio/readseq/readseqcgi.java line=25
public class cgi extends run 
{	
	public static void main(String[] args) {
		new cgi(args);
		}

	final static int kHelpdoc= 0, kConvertForm = 1, kMoreHelpdoc= 2;
	public static String formpath= "rez", kCgiForm= "cgiform.html";
	public static String varkey= "%%";
	public static String docbaseUrlKey = "docbaseURL";

	final static int kNofeature= 0, kSelfeature= 1, kRemfeature= 2;
	int featsel= kNofeature;
	int printdocKind= kConvertForm;
	boolean bIsHttpCall= true, wantHelp, moreHelp, haveData;
	PrintStream pr;

	protected void message(String s) {
    System.out.println(s);
		//override run// BioseqReader.message(s); == System.err.println(s);
		}

	public cgi( String[] args) 
	{
		//System.err.println("  *** not ready yet ***");
		super();
		pr= System.out;
		getargs( args);  
		
		// FIXME: set env: ReadseqForm.ACTION=/cgi-bin/readseq.cgi
		// to current calling script: REQUEST_URI = /.bin/tools/printenv.cgi
		if (Environ.gEnv.isdefined("REQUEST_URI")) {
			//?? take any old req uri?
			Environ.gEnv.set("ReadseqForm.ACTION", Environ.gEnv.get("REQUEST_URI"));
			}
		
		if (haveInputData()) {
			verbose= false;  
			outname= null; // force use of System.out
			dopipe= true;
			switch (featsel) {
				default:
				case kNofeature: featlist= null; nofeatlist= null; break;
				case kSelfeature: break; // featlist should be set by getOneArg()
				case kRemfeature: nofeatlist= featlist; featlist= null; break;
				}			
			
			int outid= BioseqFormats.getFormatId(outformat);
			
			if (true) {
				// can we trick browser into using filename to save, instead of 'readseq.cgi' !?
			String fn= Environ.gEnv.get("CONTENT_FILENAME");
			if (fn.length()==0) fn= "readseq-output";
			fn += BioseqFormats.formatSuffix(outid);
			pr.println("Window-target: "+ fn);  // this makes a new window at least - so old form remains
			pr.println("Title: "+ fn);   // not effective
			//String uri= "/readseq/output/" + fn;
			//pr.println("Location: "+ uri); // -- can't find, tries to redirect...
			//pr.println("URI: <" + uri + ">");
			}
			
			String ct= Environ.gEnv.get("APP_CONTENT_TYPE");
			if (ct.indexOf('/')<0) // bypass bioseq/type only if have "text/plain" style ct
				ct= BioseqFormats.contentType(outid);
			
			pr.println("Content-Type: "+ ct);
			pr.println();
			run();
			}
		else {
			printhtmldoc(printdocKind);
			//if (wantHelp) printhtmldoc(kHelpdoc);
			//else if (moreHelp) printhtmldoc(kMoreHelpdoc);
			//else printhtmldoc(kConvertForm);
			}
	}


	public static void cgiusage() { cgiusage(System.out); }
 
	public static void cgiusage(PrintStream out) {
		out.println();
    out.println("  HTTP server common gateway interface to Readseq");
		//out.println("  Usage: jre -cp readseq.jar " + cgi.class.getName());
		out.println("  Usage (example as cgi on unix system):");
		out.println("  Install shell script like this as '/cgi-bin/readseq.cgi' for your web server");
		out.println("    #!/bin/sh");
		out.println("    envtemp=/tmp/rseq$$.env");
		out.println("    env > ${envtemp}");
		out.println("    /usr/java/bin/jre -cp readseq.jar " + cgi.class.getName() + " env=${envtemp}");
		out.println("    /bin/rm ${envtemp}");
		out.println();
    out.println("  " + Readseq.version);
    out.println("  See also " + run.class.getName() + ".usage()");
		out.println();
	}

	public void printReadseqCgiUsage()
	{
    String helps= AppResources.global.getData( kReadseqcHelp);
    if (helps!=null) {
    	String lf = System.getProperty("line.separator");
			int e= helps.indexOf("</HTML");
  		if (e>0) e= helps.lastIndexOf(lf, e);
  		if (e<0) e= helps.length();
			pr.println( helps.substring(0,e));
    	pr.println("</PRE>");
    	}
    	
		pr.println("<HR>");
		pr.println("<P><I><B>" + Readseq.version + "</B></I></P>");
		String url= Environ.gEnv.get("APP_SOURCE_URL");
		if (url.length()>0) {
			pr.println("<P>Home of this package is ");
			pr.println("<A href=\""+url+"\">" + url + "</A></P>");
			}
		url= Environ.gEnv.get("APP_CGI_URL");
		if (url.length()>0) {
			pr.println("<P>An instance of the Web form for this is ");
			pr.println("<A href=\""+url+"\">" + url + "</A></P>");
			}
		url= Environ.gEnv.get("FEATURE_TABLE_URL");
		if (url.length()>0) {
			pr.println("<P>For further information on feature tables, see ");
			pr.println("<A href=\""+url+"\">" + url + "</A></P>");
			}

				// use help class instead help.formatHelp()
		pr.println("<HR>");
		pr.println("<B>Known biosequence formats:</B><BR>");
		pr.println( BioseqFormats.getInfo( 0, "html-header"));
		for (int i=1; i<=BioseqFormats.nFormats(); i++) {
			pr.println( BioseqFormats.getInfo(i, "html-command-line"));
			}
		pr.println( BioseqFormats.getInfo( 0, "html-footer"));
		pr.println("&nbsp;&nbsp; (Int'leaf = interleaved format; Features = documentation/features are parsed)");

		pr.println("<HR>");
		pr.println("<P>Also available in this package:</P>");
		pr.println("<P>For the current help document, use <BR>jre -cp readseq.jar help</P>");
  	pr.println("<PRE>");
		app.appusage(pr);
		pr.println("<HR>");
		cgi.cgiusage(pr);
  	pr.println("</PRE>");
		pr.println("</BODY></HTML>");
	}
	
	public void getargs(String[] sargs) 
	{	
		doclassic= false; // no single-letter args
		argv= new Args(sargs, bIsHttpCall);  
		getargs();
	}
	

	protected void getOneArg() 
	{	
		String argstr= argv.arg();
		String key= argv.argKey();
		String val= argv.argValue();
		boolean hasval=  argv.hasValue();
		boolean boolval= (argv.isBoolean()) ? argv.booleanValue() : true;			
		
		if ((key.equals("mimetype") || key.equals("mime")) )  {
			if ( hasval ) {
				Environ.gEnv.set("format", val); 
				outformat= val;
				}
			}
			
 		else if (key.equals("server"))  {
 			if ( hasval ) Environ.gEnv.set("SERVER_PATH", val); 
			}
		else if ((key.equalsIgnoreCase("APP_CONTENT_TYPE") || key.equalsIgnoreCase("contenttype")) )  {
			if ( hasval ) Environ.gEnv.set("APP_CONTENT_TYPE", val); 
			}
			
			// data from HTTP Post/multipart-forms -- read from string
		else if ( argstr.startsWith(kInputStringKey) ) {
			innames.addElement( argstr);
			}
		else if ( key.equals("FILE_DATA") ) {
			// Args should also collect filename 
			// Content-Disposition: form-data; name="FILE_DATA"; filename="5srna.gb"  
			if (hasval && val.trim().length()>0) innames.addElement( kInputStringKey + val);
			}
		else if ( key.equals("STRING_DATA") ) {
			if (hasval && val.trim().length()>0) innames.addElement( kInputStringKey + val);
			}

		else if (key.equals("case")) {
			if ("lc".equals(val)) dolowercase= true;
			else if ("UC".equals(val)) douppercase= true;
			else if ("nc".equals(val)) { douppercase= false; dolowercase= false; }
			}
		
		else if ( key.equals("featlist") ) { 
			if (hasval) { featlist= appends(featlist,val); } // need to swap to nofeatlist if featsel=remove 
			}
		else if ( key.equals("featsel") ) { 
			featsel= argv.intValue();
			//if ("none".equals(val)) featsel= 0;
			//else if ("extract".equals(val)) featsel= 1;
			//else if ("remove".equals(val)) featsel= 0;
			}

		else if (key.equals("doall")) { }
		//else if (key.equals("itemlist")) { } -- run() can handle this
		//else if (key.equals("translates")) { } //  now run() will handle
		//else if ( key.startsWith("format")) { if ( hasval) outformat= val; }// format=			
		
		else if ("morehelp".equals(key) ) 
			printdocKind= kMoreHelpdoc; 
		else if ("help".equals(key) || "h".equals(key)) 
			printdocKind= kHelpdoc; //wantHelp= true;

    // cgi uses true as hasval;  cant send to super.getOneArg
		else if ( key.startsWith("degap") ) { 
		  degap= boolval; //if (hasval) degapc= val.charAt(0); 
		  }
		else if ( key.startsWith("gapch") ) {  // format -gapcount
		  if (hasval) degapc= val.charAt(0); 
		  }
						
		else
			super.getOneArg();
	}
	
	protected void handleUnknownArg()
	{
			// might be a useful env var, or might be data file, e.g. unix call 'treeprint *.tree'
 		Environ.gEnv.set( argv.argKey(), argv.argValue() );
		//System.err.println("Unknown argument: " + argv.argKey() + "=" + argv.argValue());
	}
	
	String getFormatChoice(String param) 
	{
		boolean isout= (!param.startsWith("in"));
		StringBuffer sb= new StringBuffer();
		sb.append("<SELECT name=\""+param+"\">\n");
		String selname= BioseqFormats.formatName( BioseqFormats.getFormatId(this.outformat));
		int n= BioseqFormats.nFormats();
		for (int i= 1; i<=n; i++) {
			boolean  cando;
			if (isout) cando= BioseqFormats.canwrite(i);
			else cando= BioseqFormats.canread(i);
		 	if (cando) {
			 	String ct= BioseqFormats.contentType(i);
				String nm= BioseqFormats.formatName(i);
				String sel= ( selname.equals(nm) ) ? "SELECTED " : "";
				sb.append( "<OPTION "+sel+"value=\""+ct+"\">" + nm);
				sb.append('\n');
				}
			}
		sb.append("</SELECT>\n");
		return sb.toString();
	}
	
	String getFeaturesChoice(String param) 
	{
		StringBuffer sb= new StringBuffer();
		sb.append("<SELECT name=\""+param+"\" size=10 multiple>\n");
		String[] flist= BioseqDocImpl.getStandardFeatureList();
		if (flist==null) flist= new String[] { "exon", "intron", "CDS" }; // ? error
		int n= flist.length;
		for (int i= 0; i<n; i++) {
		 	String nm= flist[i];
			sb.append( "<OPTION>" + nm);
			sb.append('\n');
			}
		sb.append("</SELECT>\n");
		return sb.toString();
	}

		
	public URL getHttpServerUrl() // move somewhere else - AppBase ?
	{
		URL su= null;
		try {
			String host= Environ.gEnv.get("SERVER_NAME");
			int port= Environ.gEnv.getInt("SERVER_PORT", 80);
			if (port==80 || port==0) su= new URL( "http", host, "");
			else su= new URL( "http", host, port, "");
			}
	 	catch (Exception e) { if (Debug.isOn) e.printStackTrace(); }
		Debug.println("getHttpServerUrl=" + su);
		return su;
	}

	
	void printhtmldoc(int whichdoc)
	{
		pr.println("Content-Type: text/html");
		pr.println();
	
		switch (whichdoc) {
		
			default:
			case kHelpdoc:
				printReadseqCgiUsage();	
				break;
			case kMoreHelpdoc:
				help h= new help(false,true,pr);
				h.extraHelp();
				break;	
			case kConvertForm:
				String form= AppResources.global.getData(formpath, kCgiForm); 
				if (form!=null) {
					Environ.gEnv.set("ReadseqForm.Outformat", getFormatChoice("format"));
					Environ.gEnv.set("ReadseqForm.Features", getFeaturesChoice("featlist"));
					printWithEnvVars(pr, form);
					}
				else {
					pr.println("Error: missing form data <BR>");
					}
				break;
			}
	}

	public final void printWithEnvVars(PrintStream pr, String val) {
		printWithEnvVars(pr,val,null); 
		}
	public void printWithEnvVars(PrintStream pr, String val, String data)
	{
		int lastend= 0, at= 0;
		int keylen= varkey.length();
		while (at>=0) {
			int at0= at;
			at= val.indexOf(varkey, at0);
			if (at>=0) {
				int e= val.indexOf(varkey, at+keylen);
				if (e>at) {
					pr.print(val.substring(at0,at));  
					String key2= val.substring(at+keylen, e);
					String val2;
					/*if (docbaseUrlKey.equals(key2))  
						val2= docbaseUrlValue; 
					else if (data!=null && sampleDataKey.equals(key2))  
						val2= data; 
					else*/
						val2= Environ.gEnv.get(key2);
					pr.print(val2);  
					e += keylen;
					lastend= e;
					}
				at= e;
				}
			}
		pr.print( val.substring(lastend));  
	}
	

}

