//flybase/Args.java
//split4javac// flybase/Args.java date=01-Mar-2002

// flybase/Args.java
// handle environ variables from Environ.properties input
// also Args for command-line inputs -- also does HTTP ("cgi") args
// d.gilbert



package flybase;


import java.io.*;
import java.util.*;
import java.net.URL;

/*
 aug99  - added opt allow arg pattern: "fg:hI:o:"  for single let, '-x' style args
 formats
	  pattern: "abC:d"   args: "-adC bob", "-a -d -Cbob"
	  pattern: null   args "-a -d -Cbob", "a=1 d=3 C=bob", 
	  pattern: null   args "alpha=123 delta=joe Cops=bob file1 file2"
	  
	todo - add opt to read args from System.in (like for HTML post)
*/

//split4javac// flybase/Args.java line=25
public class Args 
	implements Enumeration
{
	public static String argFlag = "-";
	public static boolean checkFlag= true;
	public static final int kUnnamed = 0, kNamed= 1;
	
	protected String[] theArgs;
	protected String arg, argval, argkey, nextarg, argpat;
	protected int iarg, argtype;
	protected boolean httpcall,doStripBackslash, argpatIsSingleLetters, isMultiPartForm;
	protected Properties pargs; // args as a Hashtable of key,value pairs?  
	
	public Args(String[] args) { this(args,false); }
	
	public Args(String[] args, boolean httpcall) {
		theArgs= args;
		this.httpcall= httpcall;
		
		// preprocess(); //?
		//doStripBackslash= !(System.getProperty("file.separator").equals("\\"));
		doStripBackslash= (System.getProperty("file.separator").equals("/"));
		
		// quick check for debug
		if (args!=null) for (int i=0; i<args.length; i++)
			if (args[i].startsWith("debug")) Debug.setVal(1);
			
		checkForHtmlPost(); //? always check?
		}
		
		// Enumeration interface
  public boolean hasMoreElements() { return hasMoreArgs(); }

  public Object nextElement() { return nextArg(); }


	public void setHttpcall(boolean turnon) { httpcall= turnon; }

		/** for unix style "ab:C:def" */
	public void setArgPattern(String pattern, boolean singleLetters) { 
		argpat= pattern; 
		argpatIsSingleLetters= singleLetters;
		}
	
	
	public void reset() { 
		iarg= 0;  
		arg=  nextarg=  argkey=  argval= null; 
		pargs= null;
		}
	
	public boolean hasMoreArgs() {
  	if (nextarg != null) return true;
		else if (iarg >= theArgs.length) return false;
		else return true;
		}
	
	public String[] theArgs() { return theArgs; }

	public String arg() { return arg; }

	public String argKey() { return argkey; }
	
	public int argType() { return argtype; }
	
	public String argValue() { return argval; }
	
	public boolean hasValue() { return (argval!=null && argval.length()>0); }
	
	public int intValue() { 
		if (argval==null) return 0;
		try { return Integer.parseInt(argval); }
		catch (Exception e) { return 0; }
		}
		
	public double doubleValue() { 
		try { return Double.valueOf(argval).doubleValue(); }
		catch (Exception e) { return Double.NaN; } // 0 ?
		}
		
	public boolean booleanValue() { 
		return ("true".equalsIgnoreCase(argval) || "1".equals(argval) || "on".equalsIgnoreCase(argval));
		}
		
	public boolean isBoolean() { 
		return booleanValue() 
			|| ("false".equalsIgnoreCase(argval) || "0".equals(argval) || "off".equalsIgnoreCase(argval));
		}
	
	public Properties properties() { 
		if (pargs==null) pargs= getAsProperties();
		return pargs;
		}
		
		
	protected Properties getAsProperties()
	{
		Properties pargs= new Properties();
		int saveiarg= iarg; iarg= 0;
		String savearg= arg; arg= null;
		String saveargval= argval; argval= null;
		String savenextarg= nextarg; nextarg= null; 
		try {
			while (hasMoreArgs()) {
				nextArg();
				pargs.put(arg, (argval==null) ? "true" : argval);
				}
			}
		catch (Exception e) {}
		iarg= saveiarg; arg= savearg;
		argval= saveargval; nextarg= savenextarg;
		return pargs;
	}
	

  public final static String postMimeType = "application/x-www-form-urlencoded";
  public final static String formdataMimeType = "multipart/form-data";
  public final static String formfieldStart = "Content-Disposition: form-data";
				// CONTENT_TYPE = multipart/form-data; boundary=---------------------------32196953321522
  final static String postMethod = "POST";
  final static String getMethod = "GET";
	boolean checkedPost;

	private final String shortst(String s) {
		if (s!=null && s.length()>80) return s.substring(0,75)+"...";
		else return s;
		}
		
  protected final String getFormParam(String key, String s)
  {			
  	String val= null;
		int at= s.indexOf(key);
		int len= s.length();
		if (at>0) {
			at += key.length();
			int e= at;
			if (s.charAt(e)=='"') { at++; e= s.indexOf('"',at+1); }
			else if (s.charAt(e)=='\'') { at++; e= s.indexOf('\'',at+1); }
			else { while (e<len && s.charAt(e)>' ' ) e++; }
			val= s.substring(at,e);
			}
		return val;
	}
	
  protected boolean readMultipartForm(String contype, Vector av)
  {			
		checkedPost= true;
		boolean addedarg= false;
		int clen= Environ.gEnv.getInt("CONTENT_LENGTH");
		if (clen>0) try {
			String bnd= getFormParam("boundary=", contype);
			Debug.println("mpart-form boundary: " + bnd);
			if (bnd!=null)  {
				//! bnd string differs by number of '-' in contype and data !!!! who is the bozo?
				int i= 0, len= bnd.length();
				while (i<len && bnd.charAt(i)=='-') i++;
				bnd= bnd.substring(i);
				}
				
				// better think about spooling file data to temp file - may be huge...
			DataInputStream din= new DataInputStream(System.in);
			String argname=null, argfile=null;
			StringBuffer argbuf= new StringBuffer();
			int argline= 0;
			String s= din.readLine();
			while (s!=null) {
				if (s.startsWith("--") && s.indexOf(bnd)>=0) {
					if (argname!=null && argbuf.length()>0) {
						if (argfile!=null) Environ.gEnv.set("CONTENT_FILENAME", argfile);
						String arg= argname + "=" + argbuf.toString();
						Debug.println("mpart-form arg: " + shortst(arg));
						av.addElement(arg); addedarg= true;
						}
					argline= 0; argname= null; argfile= null; 
					argbuf.setLength(0);
					}
				else if (s.startsWith(formfieldStart)) {
					argname= getFormParam("name=", s);
					argfile= getFormParam("file=", s);
					Debug.println("mpart-form key: " + shortst(argname));
					argline= 0;
					argbuf.setLength(0); // append(argname=) ??
					}
				else {
					argline++;
					if (argline>1) { // || s.length()>0
						if (argline>2) argbuf.append('\n');
						argbuf.append(s);  
						}
					}
					
				s= din.readLine();
				}
			din.close();
			}
			
		catch (Exception e) { e.printStackTrace(); }
		return addedarg;
	}	

  protected boolean addHttpArgs(String s, Vector av)
  {
  	boolean more= false;
		int at0= 0;
		while (at0>=0) {
			String arg;
			int at= s.indexOf('&', at0);
			if (at<0) { arg= s.substring(at0); at0= -1; }
			else { arg= s.substring(at0,at); at0= at+1; }
			arg= Utils.decode(arg);	
			if (arg.length()>0) {  av.addElement(arg); more= true; }
			}
		return more;
	}
		
  protected boolean checkForHtmlPost()
  {
  	if (checkedPost) return false;
  	// also look for "QUERY_STRING" ?
		boolean ispost= postMethod.equalsIgnoreCase(Environ.gEnv.get("REQUEST_METHOD"));
		//boolean ismulti= false;
		boolean isget= false;
		
		boolean newargs= false;
		Vector av= new Vector();
		if (theArgs!=null) for (int i=0; i<theArgs.length; i++) 
			av.addElement(theArgs[i]); //? do we want cmdline args after POSTed ones?
			
		String qs= Environ.gEnv.get("QUERY_STRING"); //? check always?
		if (qs.length()>0) {
			if (addHttpArgs( qs, av)) newargs= true;
			isget= true;
			}
			
		if (ispost) {
			String ct= Environ.gEnv.get("CONTENT_TYPE");
			if ( ct.startsWith( formdataMimeType) ) { 
				isMultiPartForm= true; 
				ispost= false;  //! false? need other method to read multipart
				if (readMultipartForm(ct, av)) newargs= true;
				//return true;
				}
			else 
				ispost= postMimeType.equalsIgnoreCase(ct);
			}
			
		if (ispost) {
			checkedPost= true;
			int clen= Environ.gEnv.getInt("CONTENT_LENGTH");
			if (clen>0) try {
				byte[] buf= new byte[clen]; // can clen be too big if ismulti ?
				System.in.read(buf);

				String s= new String(buf);
				Environ.gEnv.set("CONTENT_STRING", s); // in case we need elsewhere?
				Debug.println("POSTed arguments: " + shortst(s));
				
				if (addHttpArgs( s, av)) newargs= true;
				}
			catch (Exception e) { 	e.printStackTrace(); }
			}
			
		if (newargs) {
			String[] ss= new String[av.size()];
			av.copyInto(ss);
			theArgs= ss;
			}
		return (ispost || isget || isMultiPartForm);
	}
	
	protected String checkArgQuote(String arg, char quote) {
		return checkArgQuote( arg, quote, iarg);
		}
		
	protected String checkArgQuote(String arg, char quote, int atarg) 
	{
		int at= arg.indexOf(quote);
		if (at >= 0) {
			int e= arg.indexOf(quote,at+1);
			while ( e<0 && atarg < theArgs.length ) {
				arg += " " + theArgs[atarg++];
				e= arg.indexOf(quote, at+1);
				}  
			int at1= at+1, e1= e+1;
			if (e<0) { e= arg.length(); e1= e; }
			return arg.substring(0,at) + arg.substring(at1,e) + arg.substring(e1);
			}
		else return arg;
	}


	protected boolean isCommonArg()
	{
				// process some common args
		if (arg.startsWith("debug")) {
			int val=  Math.max(1, intValue());
			Debug.setVal(val);
			return true; //if (hasMoreArgs()) return nextArg();
			}
	
		else if (arg.startsWith("env=") 
			|| arg.startsWith("environ=") 
			|| arg.startsWith("properties=")) {
			int eq= argval.indexOf('=');
 			if (eq<0) 
				Environ.gEnv.readProperties(argval); 
 			else {
 				String key= argval.substring( 0, eq);
 				String val= argval.substring( eq+1);
				Environ.gEnv.set(key, val); 
				}
			checkForHtmlPost();
			return true; //if (hasMoreArgs()) return nextArg();
			}
		
		else if (arg.startsWith("mimetype=") || arg.startsWith("mime=") ) {
			if ( postMimeType.equalsIgnoreCase(argval) ) argval= "text/html";
			}	
		return false;		
	}
	
	public String nextArg()
	{
		try {
  		if (nextarg!=null) { arg= nextarg; nextarg= null; }
			else arg= theArgs[iarg++]; // throws array out of bounds 
			argkey= arg;
			argval= null;
			argtype= kUnnamed;

 			if (arg.equalsIgnoreCase("httpcall")) {
 				httpcall= true; 
 				checkForHtmlPost();
				}
				
			if (httpcall && !isMultiPartForm) {
				//if (arg.indexOf("='")<0 && arg.indexOf("=\"")<0) { // skip quoted parts!?
					int at= arg.indexOf('&');
					if (at>=0) {
						if (at+1<arg.length()) nextarg= arg.substring(at+1);
						arg= arg.substring(0,at);
						}
					if (arg.indexOf('+')>=0 || arg.indexOf('%')>0) 
						arg= Utils.decode(arg);
					//}
				}
				
			String carg= checkArgQuote(arg,'"');
			if (carg!=arg) arg= carg;
			else arg= checkArgQuote(arg,'\'');
				// fix for apache-httpd escaping -- ONLY FOR UNIX - not for MSDOS - check system.property
			if (doStripBackslash && arg.indexOf('\\')>=0 ) 
				arg= Utils.removeChars(arg,"\\");

			int at= arg.indexOf('=');
			if (at>0) {
				argtype= kNamed;
				argval= ((at+1>=arg.length()) ? "" : arg.substring(at+1));
				int at0= 0;
				if (checkFlag && arg.startsWith(argFlag)) at0= argFlag.length();
	 			argkey= arg.substring( at0, at);
				}

			else if (checkFlag && arg.startsWith(argFlag)) {
				int len= arg.length();
	 			if (len>1) {
	 				argkey= arg.substring(1,2);
					argtype= kNamed;
					if (len>2) argval= arg.substring( 2);
	 				int ip= (argpat==null ? -1 : argpat.indexOf(argkey));
	 				if (ip>=0) {
	 					if (argpat.length()>ip && argpat.charAt(ip+1) == ':') {
	 						if (argval==null) argval= theArgs[iarg++]; //? need any more processing?
	 						}
	 					else if (argpatIsSingleLetters) {
	 						nextarg= argval; // may be null
	 						argval= ""; // null?
	 						}
			 			}				
	 				}
				}
			
			if (argkey==null) {
				argkey= arg;
				argval= arg;
				argtype= kUnnamed;
				}	

			if (isCommonArg()) {
 				if (hasMoreArgs()) return nextArg();
				}

			Debug.println("Argument: '" + shortst(arg) 
					+ "', key: " + shortst(argkey) + " = " + shortst(argval));   
			return arg;
			}
		catch (Exception e) {
			throw new NoSuchElementException("Args"); 
			}
	}

	
	/* public void readArgs() throws Exception
	{
		String s;
  	//s= System.getProperty("db"); if (s!=null) dbName= s;
		//if (System.getProperty("debug")!=null || System.getProperty("verbose")!=null) 
		//	setDebug( true);
		//if (args==null || args.length==0) usage();

		while (hasMoreArgs()) {
   		if (arg.startsWith("out=")) outname= argval;
   		else if (arg.equals("httpcall")) httpcall= true;
  		else if (arg.startsWith("debug")) setDebug(true);
  		else if (arg.startsWith("mimetype=")) mimetype= argval;
 			else if (arg.startsWith("mime=")) mimetype= argval;
   		else if (arg.startsWith("java=")) Environ.gEnv.set("java",argval);
 			else if (arg.startsWith("env=")) {
 				int at= argval.indexOf('=');
	 			if (at<0) 
					Environ.gEnv.readProperties(argval); 
	 			else {
	 				String key= argval.substring( 0, at);
	 				String val= argval.substring( at+1);
					Environ.gEnv.set(key, val); 
					}
				}
			}
	} */
		
}




