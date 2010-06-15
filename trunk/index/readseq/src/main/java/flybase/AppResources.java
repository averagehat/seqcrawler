// flybase/AppResources.java

package flybase; //.util ?


import java.io.*;
import java.util.*;
import java.net.URL;

import flybase.*;


public class AppResources
{
  public static AppResources global; 
	public static String defaultRezpath = "rez/"; // same as FastProperties
	String lineSeparator;

  static {
		global= new AppResources(); 
  	}

	public static void main(String args[])
	{
 		// special entry to allow others to pull rez files from the jar
 		Vector data= new Vector();
 		String out= null;
 		boolean append= false;
 		String rezpath= defaultRezpath;
 		if (args.length==0) args= new String[] { "help" };
		for (int i=0; i<args.length; i++) {
			String arg= args[i];
			int eq= arg.indexOf('=');
			if (eq>0) {
				String key= arg.substring(0,eq).toLowerCase();
				String val= arg.substring(eq+1);
				if (key.startsWith("out")) out= val;
				else if (key.startsWith("path")) rezpath= val;
				else if (key.startsWith("append")) append= (val.equals("false") ? false : true);
				else if (key.startsWith("data")) data.addElement(val);
				}
			else if (arg.equals("help")||arg.startsWith("-h")||arg.startsWith("--h")) {
				System.out.println("Usage: java -cp app.jar flybase.AppResources [option=val] rezfile");
				System.out.println("options:");
				System.out.println("out=filename - output file");
				System.out.println("append=true - append to output");
				System.out.println("data=filename - resource file");
				System.out.println("path=/jar/path/ - resource path in jar, default="+rezpath);
				System.exit(0);
				}
			else {
				data.addElement(arg);
				}
			}
		try {
 			AppResources ar= new AppResources();
			Writer wtr;
			if (out == null) wtr= new OutputStreamWriter(System.out);
			else wtr= new FileWriter(out,append);  
			for (int i=0; i<data.size(); i++) {
				String datas= ar.getData( rezpath, (String) data.elementAt(i));
				if (datas!=null) wtr.write(datas);
				}
			wtr.close();
			}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	
	public AppResources() {
		lineSeparator = System.getProperty("line.separator");
		}
		
	public final InputStream getStream(String rezpath, String filename) {
		return getStream( rezPath(rezpath,filename));
		}
		
	public InputStream getStream(String rezpath) {
		try { return ClassLoader.getSystemResourceAsStream( rezpath); } // j1.1 doesn't throw any Exception
		catch (Exception ex) { return null; } // java 1.2 can puke out if missing rez 
		}

	public final String getData(String rezpath, String filename) {
		return getData( rezPath(rezpath,filename));
		}
		
	public String getData(String rezpath) {
		try {
			InputStream ins= this.getStream(rezpath);
			if (ins==null) return null;  
			BufferedReader rdr= new BufferedReader( new InputStreamReader(ins));
			StringWriter wtr= new StringWriter();
			String s;
			while ((s= rdr.readLine()) != null) {
				wtr.write( s); wtr.write(lineSeparator); 
				}  
			ins.close();
			wtr.close();  
			return wtr.toString();
  		}
  	catch (Exception e) { 
  		System.err.println("Error loading " + rezpath + ": " + e.getMessage()); 
  		return null;
  		}
		}
	
	public final byte[] getBytes(String rezpath, String filename) {
		return getBytes( rezPath(rezpath,filename));
		}
	public byte[] getBytes(String rezpath) {
		try {
			InputStream ins= this.getStream(rezpath);
			if (ins==null) return null;
			if (true) {
				ByteArrayOutputStream baos= new ByteArrayOutputStream();
				int b;
				while ((b= ins.read())>=0) baos.write((byte)b);
				return baos.toByteArray();
				}
			else {
				int nb= ins.available();  //? bad?
				byte[] ba= new byte[nb];
				nb= ins.read(ba);
				ins.close();
				return ba;
				}
  		}
  	catch (Exception e) { return null; }
		}

	/*
			// drop these unless really want to pull in AWT stuff
	public final Image getImage(String rezpath, String filename, Component waitforComp) {
		return getImage( rezPath(rezpath,filename), waitforComp);
		}
		
	public Image getImage(String rezpath, Component waitforComp)
	{
		try {
  		URL url = ClassLoader.getSystemResource( rezpath);
  		if (url==null) return null;
    	//Debug.println("getImage url="+url);
			Image img= Toolkit.getDefaultToolkit().getImage(url);
			if (waitforComp!=null) {
				MediaTracker tracker = new MediaTracker(waitforComp);  
		  	tracker.addImage(img, 0);
		  	tracker.waitForID(0);
		  	}
			return img;
  		}
  	catch (Exception e) { 
  		System.err.println("Error loading " + rezpath + ": " + e.getMessage()); 
  		return null;
  		}
	}
	*/
	
	public String findPath(String cname)
	{
		if (! hasResource(cname) ) {
			int at= cname.lastIndexOf('/');
			if (at>0) { 
				cname= cname.substring(at+1); 
				if (hasResource(cname)) return cname;
				}
			cname= defaultRezpath + cname;
			if (! hasResource(cname) ) return null;
			}
		return cname;
	}
	
	public final boolean hasResource(String rezpath, String filename) { 
		return getUrl(rezpath,filename)!=null; }
		
	public final boolean hasResource(String rezpath) { return getUrl(rezpath)!=null; }

	public final URL getUrl(String rezpath, String filename) {
		return getUrl( rezPath(rezpath,filename));
		}

	public URL getUrl(String rezpath) {
		try { return ClassLoader.getSystemResource( rezpath); } // j1.1 doesn't throw any Exception
		catch (Exception ex) { return null; } // java 1.2 can puke out if missing rez 
		}
	
	public String rezPath(String path, String filename) {
		if (path==null || path.length()==0) return filename;
		else if (filename==null) return path;
		else {
     	path = path.replace('.', '/'); //? in case path == 'something.package.classname' 
     	if (path.charAt( path.length()-1) != '/') path += "/";
			return path + filename;		
			}
		}
	
} 
