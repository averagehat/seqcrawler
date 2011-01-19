//flybase/Native.java
//split4javac// flybase/Native.java date=25-Sep-2001

// native.java
// d.gilbert, java extensions



package flybase;
//package dclap;


import java.net.*;
import java.util.StringTokenizer;
import java.io.*;
import java.util.Random;
//import java.awt.Frame;
//import java.awt.FileDialog;

import flybase.Utils;

//import com.apple.mrj.*;
//import com.apple.mrj.aei.*;


//split4javac// flybase/Native.java line=23
public class Native {
	public final static int kSysUnknown = 0;
	public final static int kSysMac = 1;
	public final static int kSysMSWin = 2;
	public final static int kSysXWin = 3;   
	public final static int kSysOS2 = 4;   
	public final static int kSysUntested = -1;
	public final static int mac = kSysMac;
	public final static int mswin = kSysMSWin;
	public final static int xwin = kSysXWin;
	public final static int os2 = kSysOS2;

	//public static boolean MRJ2BUG;
	
	protected static int systemFlavor = kSysUntested;
	protected static String lineEnd = null;
	
	static {
		SystemFlavor();
		}
		
	public static int SystemFlavor() {
		if (systemFlavor == kSysUntested) {
			String osname= System.getProperty("os.name");
			osname= osname.toLowerCase();
			if (osname.startsWith("mac")) { //both "MacOS" and "Mac OS" seen
				systemFlavor= kSysMac; 
				//String mrjvers = System.getProperty("mrj.version");
				//if (mrjvers!=null && mrjvers.startsWith("2.0")) MRJ2BUG= true; 
				}
			else if (osname.startsWith("windows")) 
				systemFlavor= kSysMSWin;
			else if (osname.startsWith("os/2")) 
				systemFlavor= kSysOS2;
			else if (System.getProperty("file.separator").equals("\\"))  
				systemFlavor= kSysMSWin;
			else if (System.getProperty("line.separator").equals("\n\r")) 
				systemFlavor= kSysMSWin;
			else 
				systemFlavor= kSysXWin; // default - too many os's to test
			}
		return systemFlavor;
		}
	
	public static String SystemFlavors() {
		if (systemFlavor == kSysUntested) 
			SystemFlavor();
		switch (systemFlavor) {
			case kSysMac: 	return new String("mac");
			case kSysMSWin: return new String("mswin");
			case kSysOS2: 	return new String("os2");
			case kSysXWin: 	return new String("xwin");
			default:	return new String("unknown");
			}
		}


	public static boolean standardLineEnd= false;
	
	public static String LineEnd() {
		if (standardLineEnd) return "\n";
		if (lineEnd==null) {
			lineEnd= System.getProperty("line.separator"); // ? is this accurate now?
			/*if (systemFlavor == kSysUntested) 
				SystemFlavor();
			switch (systemFlavor) {
				case kSysMac: lineEnd= new String("\r"); break;
				case kSysMSWin: lineEnd= new String("\r\n"); break;
				default:
				case kSysOS2: 	 
				case kSysXWin: 	lineEnd= new String("\n"); break;
				}*/
			}
		return lineEnd;
		}
		
	public static String LineEnd(int repeatCount) {
		String e= new String( LineEnd());
		while (--repeatCount>0) e += LineEnd();
		return e;
		}

	public static String filePath(String javaPath) {
		// convert Java path separators to native file system separators !
		if (javaPath==null) return null;
		char cto, cfrom= File.separatorChar;
		String top;
		if (systemFlavor == kSysUntested) 
			SystemFlavor();
		switch (systemFlavor) {
			case kSysMac: 	
				//if (System.getProperty("mrj.version")==null)
				//		return javaPath; //!? metrowerks barfs on mac native
				cto= ':'; top=""; 
				break;
			case kSysOS2:
			case kSysMSWin: cto= '\\'; top="\\"; break;  //top="C:\\";
			default:
			case kSysXWin: 	cto= '/'; top="/"; break;   // assume Unix OS
			}
			
		boolean changetop= (javaPath.charAt(0)==cfrom);
			// problems when // is converted to :: on mac -- different meanings!
		String twoSlash= File.separator + File.separator;
		int at= javaPath.indexOf(twoSlash);
		while (at>=0) {
			javaPath= javaPath.substring(0,at) + javaPath.substring(at+1);
			at= javaPath.indexOf(twoSlash);
			}
		String nativePath= javaPath.replace(cfrom,cto);
		nativePath= Utils.decode(nativePath);
		if (changetop) nativePath= top + nativePath.substring(1);
		return nativePath;
		}


	public static String tempFolder() 
	{
		if (tempFold==null) {
			// note: java 1.2/1.3 has File createTempFile(prefix,suffix,null|directory)
			//  GetPropertyAction a = new GetPropertyAction("java.io.tmpdir");
	    //  tmpdir = ((String) AccessController.doPrivileged(a));

			String fold= null;
			File ff= null;
			if (systemFlavor == kSysUntested) 
				SystemFlavor();
				
			switch (systemFlavor) {
				case kSysMac: 
					/*try {
						String mrjvers = System.getProperty("mrj.version");
						if (mrjvers!=null)  
							ff= MRJFileUtils.findFolder(MRJFileUtils.kTemporaryFolderType);
						//if (ff!=null && ff.isDirectory()) fold= ff.getPath();
						}
					catch (java.io.FileNotFoundException ex) { }
					*/
					break;
					
				case kSysXWin: 
					ff= new File("/tmp",""); // or "/usr/tmp" ?
					break;
					
				case kSysOS2: 	
				case kSysMSWin: 
					ff= new File("\\tmp","");
					if (ff==null || !ff.exists()) ff= new File("\\temp","");
					if (ff==null || !ff.exists()) ff= new File("c:\\temp","");
					break;
				}
			 
			if (ff!=null && ff.exists() && ff.isDirectory()) 
				fold= ff.getPath();
			if (fold==null || fold.length()==0)	
				fold= System.getProperty("user.dir","") + "/"; //?
			tempFold= fold;
			}
		return tempFold;
	}
		
	private static final Object tmpFileLock = new Object();
	private static int tempnum = -1;
	private static String tempFold = null;

  public static String tempFilename( String prefix, String suffix) { 
		if (prefix==null) prefix= "dclap";
		if (suffix==null) suffix= ".tmp";
		synchronized (tmpFileLock) {
			if (tempnum == -1) tempnum = new Random().nextInt() & 0xffff;
			tempnum++;
			prefix= prefix + Integer.toString(tempnum) + suffix;
			}
		return prefix;
    }

  public final static String tempFilename( String suffix) { 
		return tempFilename("dclap", suffix);
    }
    
  public final static File tempFile() { return tempFile(".tmp");  }
  public static File tempFile(String suffix) { 
  	String tfold= tempFolder();
  	if (tfold==null || tfold.length()==0) return new File(tempFilename(suffix));
		else return new File(tfold, tempFilename(suffix));
    }

		
/*****		
	public static void loadLibraries(String libraries) {
		// library name syntax is:
		//   "all_dclap;mac_native;mswin_mwincode;sunos_other"
		// where the prefixes "all_", "mac_", ... are stripped before trying to load
		
		if (libraries==null || libraries.length() < 2)  return;
		if (systemFlavor == kSysUntested) 
			SystemFlavor();
		boolean notfound= false;
		String osname= System.getProperty("os.name"); 
		osname= osname.toLowerCase();
		String osarch= System.getProperty("os.arch");
		String sysflavor= SystemFlavors();
		String missinglib = "";
		StringTokenizer st= new StringTokenizer(libraries,"; \t\n\r");
		while (st.hasMoreTokens()) {
			String lib= st.nextToken();
			if (lib.startsWith("all")) lib= lib.substring(4);  
			else if (lib.startsWith(osname)) lib= lib.substring(1+osname.length());  
			else if (lib.startsWith(osarch)) lib= lib.substring(1+osarch.length()); 
			else if (lib.startsWith(sysflavor)) lib= lib.substring(1+sysflavor.length()); 
			else lib= null; // not for this system...
			
			if (lib!=null) {
				try { System.loadLibrary(lib);  }
				catch (UnsatisfiedLinkError linkerr) {
					try { System.loadLibrary("Java_" + lib); } // JDK 1.0 runtime name
					catch (UnsatisfiedLinkError ex) { 
						missinglib += lib + " ";
						notfound= true;
						//ex.printStackTrace();
						}
					}
				}
			}
		if (notfound) {
			System.out.println("Failed to load native code library: " + missinglib);
			}
		}
*****/

/****
				// call external application  
	public static String macResultFile;
	
	protected static String[] macCommandList(String[] commandArray, String[] environ, 
					String resultFile) throws IOException
	{
		String[] newlist = null;
		if (commandArray!=null) {
			DTempFile df= new DTempFile(".commands"); // DTempFile() deletes disk file when finalized
			File f= df.getFile();
			newlist= new String[3];
			newlist[0]= commandArray[0]; // app
			newlist[1]= f.getPath();
			newlist[2]= resultFile; //NOT Native.filePath(resultFile);
				// dang -- resultFile must exist to be passed by Finder to child app
			
			PrintStream pr;
			pr= new PrintStream( new FileOutputStream(resultFile));
			pr.println("start child app");
			pr.close();
			
			pr= new PrintStream( new FileOutputStream(f));
			int i;
			for ( i= 0; i<commandArray.length; i++) {
				pr.print(commandArray[i]);
				pr.print(Native.LineEnd()); 
				}
			if (environ != null)  
				for ( i= 0; i<environ.length; i++) {
					pr.print(environ[i]);
					pr.print(Native.LineEnd());
					}
			pr.close();
			}
		return newlist;
	}
	
	
	public static Process exec(String[] commandArray, String[] environ) 
		throws IOException 
	{
		Process child= null;
		macResultFile= null;
		if (commandArray==null) return null;
		
		switch (SystemFlavor()) {
		
			case kSysMac  : 
				if (commandArray[0].equalsIgnoreCase("applescript")) {
					// from MRJ examples
					AppleScript as = new AppleScript();
					commandArray[0]= "";
					String scriptToRun= Utils.joinStrings(commandArray, " "); //Std.join(" ",commandArray);
					//String scriptToRun = "tell application \"Filemaker Pro\"" + "\n" +
					//						"return record 1 in database 1" + "\n" +
					//					  "end tell" + "\n";
										  
					System.out.println("running applescript...");
					Object result = as.javaRunScript(scriptToRun);
					
					macResultFile= "ascript-out." + System.currentTimeMillis();
					File f= new File(macResultFile);
					PrintStream pr= new PrintStream( new FileOutputStream(f));
					pr.println( result);
					pr.close();
					
					//System.out.println("result: " + result);
					System.out.println("...done. Results in " + macResultFile);
					child= null; //?! what can we return from this? result object?
					}
				else {
					// MRJ supports exec(), where cmdline is list of files for app to open
					// but doesn't yet supprt Process.waitFor() or .exitValue() !!!
					macResultFile= tempFolder();
					macResultFile +=  "app." + System.currentTimeMillis();
					Debug.println("exec macResultFile " + macResultFile);
					String[] macCmds= macCommandList(commandArray, environ, macResultFile); 
					child= Runtime.getRuntime().exec(macCmds, environ); 
					}
				break;
				
			default:
			case kSysMSWin: 
			case kSysXWin :  
			  child= Runtime.getRuntime().exec(commandArray, environ); 
 				break;
			}
		return child;
 	}
*****/	

		
				// call external application  
	//private native static void openurl(byte[] urlstring, int length);
		
/****			
	public static void openUrl(URL theUrl) {
		openUrl( theUrl.toString());
		}

	public static String locateWebBrowser(String sapp) {
		try {
				// let user find file?
			Frame fr= new Frame("Locate web browser");  
			FileDialog fd= new FileDialog( fr, "Locate web browser", FileDialog.LOAD);
			fd.setFile( sapp);
			fr.reshape(100,100,300,30); fr.show(); //?
			fd.show();
			fr.dispose();
			if (fd.getFile()!=null && fd.getDirectory()!=null)  { 
				sapp= fd.getDirectory() + File.separator + fd.getFile();  
				Prefs.getPrefs().setProperty("user.openurl", sapp);
				return sapp;
				}
			}
		catch (Exception e2) {	
			System.out.println("Failed to locate web browser, err:" + e2.getMessage());
			//e2.printStackTrace(); 
			}
		return null;
	}
	
	public static void openUrl(String surl) {
		String sapp, smore;
		Process proc;	
		switch (SystemFlavor()) {
			case kSysMac  : sapp= "MOSS"; break; // netscape;//"MSIE"; // MS Inet Expl.
			case kSysMSWin: sapp= "netscape.exe"; break;
			default:
			case kSysXWin : sapp= "netscape"; break;
			}

		// sapp= Environ.gEnv.get( "user.openurl."+SystemFlavors(), "netscape");
		sapp = Prefs.getPrefs().getProperty("user.openurl", sapp);
		smore= "";

		if (SystemFlavor() == kSysMac) {
			Debug.println("openUrl: macos-browser " + sapp + " '" + surl + "' ");
			String sig;
			if (sapp.length()<=4) { sig= sapp; sapp= null; } else { sig= "MOSS"; }
			WebBrowserInterface browser= new WebBrowserInterface(sig);
			Object processOrAEDesc= browser.openApp(sapp);
			if (processOrAEDesc==null) {
				sapp= locateWebBrowser(sapp);
				if (sapp!=null) processOrAEDesc= browser.openApp(sapp);
				}
			browser.openURL(surl);
			}
			
		else {
			try { 
				int ipar= sapp.indexOf("%1"); 
				if (ipar>=0) {
					smore= sapp.substring(ipar+2).trim();
					sapp = sapp.substring(0,ipar).trim();
					}
				String params[] = { sapp, surl, smore };
				Debug.println("openUrl: exec " + sapp + " " + surl + " " + smore);
				proc= Runtime.getRuntime().exec(params); 
				}
				
			catch (Exception ex) { 
				System.out.println("openUrl: failed to run " + sapp + " " + surl);
				try {
					sapp= locateWebBrowser(sapp);
					if (sapp!=null)  { 
						String params[] = { sapp, surl };
						proc= Runtime.getRuntime().exec(params); 
						}
					}
				catch (Exception e2) {	
					System.out.println("openUrl: failed to run " + sapp + " " + surl);
					System.out.println("Try setting the property: user.openurl=/path/to/url-opener-app");
					}
				}
			}
		}
******/

}

