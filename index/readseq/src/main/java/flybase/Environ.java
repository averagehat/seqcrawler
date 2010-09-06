//flybase/Environ.java
//split4javac// flybase/Environ.java date=28-Apr-2001

// flybase/Environ.java
// handle environ variables from Environ.properties input
// also Args for command-line inputs -- also does HTTP ("cgi") args
// d.gilbert



package flybase;


import java.io.*;
import java.util.*;
import java.net.URL;





//split4javac// flybase/Environ.java line=19
public class Environ { 
	public static String envPropertiesName = "Environ";  //?   
  public static Environ gEnv; // we only have one instance of this class, right?
	public static String gVarkey= "%%";
  protected FastProperties syms = new FastProperties(); //default props?

  static {
		gEnv= new Environ( true); // no exception handler in static!
  	}

	public Environ() { }

	public Environ(String propertiesName) //throws Exception
	{
  	//getResource(propertiesName);  
		syms.loadProperties(propertiesName);
	}

	protected Environ( boolean forstatic)
	{
  	String ename= System.getProperty("app.environ", envPropertiesName);
		try { syms.loadProperties(ename); }
		catch (Exception e) {}
	}

	public void getResource(String propertiesName) {
		syms.loadProperties(propertiesName);
  	}
    
  public boolean isdefined(String hsymbol) {
  	if (hsymbol==null) return false;
  	if (syms.getProperty( hsymbol) != null) return true;
  	else return false;
  	}
    
	public void save(OutputStream out, String header) {
		syms.save(out,header);
		}
   
  public void print(PrintStream aout)  {
  	syms.list( aout);
 	 }
  
	public final FastProperties gethash() { return syms; }
	
  public final String getsym(String hsymbol) {
		//return (String) syms.get(hsymbol);
		return syms.getProperty(hsymbol);
		}
	    
  public String get(String hsymbol, String defaultvalue) {
  	String val= get(hsymbol);
  	if (val == null || val.length()==0) {
  		if (defaultvalue==null) defaultvalue= ""; //? fix to ensure always "" something
  		val= defaultvalue;
  		set(hsymbol, val);
  		}
  	return val;
  }

  public String get(String hsymbol) 
  {
		if (hsymbol==null) return "";
		//String val= (String) syms.get(hsymbol);
		String val= syms.getProperty(hsymbol);
  	if (val!=null) {
  				// check value for %%ENV_NAME%% pattern and substitute other env values
  				// ? on put or on get?
  		int at= val.indexOf(gVarkey);
  		while (at>=0) {
  			int e= val.indexOf(gVarkey, at+2);
  			if (e>at) {
  				String sym2= val.substring(at+2, e);
  				String val2= get(sym2);
  				val= val.substring(0,at) + val2 + val.substring(e+2);
  				at= val.indexOf(gVarkey,at);
  				}
  			else at= -1;
  			}
  		return val;
  		}
  	else return "";
  }
	
	public String[] getList(String hsymbol)
	{
		String val= get(hsymbol);
		return Utils.splitString(val);
	}

	public String[] getList(String hsymbol, String defvalue, String delims)
	{
		String val= get(hsymbol,defvalue);
		return Utils.splitString(val, delims);
	}
	
	public final int getInt(String hsymbol) { return getInt(hsymbol,0); }

	public int getInt(String hsymbol, int defaultvalue) {
		try { return Integer.parseInt( get(hsymbol, String.valueOf(defaultvalue))); }
		catch (Exception e) { return 0; }
		}

	public final boolean getBoolean(String hsymbol) { return isTrue(hsymbol); }

	public final boolean isTrue(String hsymbol) {
		String val= get(hsymbol);
		return ("true".equalsIgnoreCase(val) || "1".equals(val) || "on".equalsIgnoreCase(val));
  	}

  	
	public final boolean isTrue(String hsymbol, boolean defaultvalue) {
		String val= get(hsymbol, String.valueOf(defaultvalue));
		return ("true".equalsIgnoreCase(val) || "1".equals(val) || "on".equalsIgnoreCase(val));
  	}
  	
			// add this in if needed for awt apps !
	/* public Font getFont( String hsymbol, Font defaultvalue) {
		String s= getsym(hsymbol); //, String.valueOf(defaultvalue));
		if (s != null) return Fonts.getFont( s);
		else return defaultvalue;
		}*/


  public void set(String hsymbol, String value) {
		syms.put( hsymbol, value); 
  	}
  	
  public void set(String hsymbol, boolean value) {
		syms.put( hsymbol, String.valueOf(value)); 
  	}
  	
  public void set(String hsymbol, int value) {
		syms.put( hsymbol, String.valueOf(value)); 
  	}

  public void set(String hsymbol, double value) {
		syms.put( hsymbol, String.valueOf(value)); 
  	}

  public void set(String hsymbol, Object value) {
		syms.put( hsymbol, String.valueOf(value)); 
  	}
    
  public String append(String hsymbol, String value) {
  	String val= (String) syms.get(hsymbol);
  	if (val==null) val= value; else val += "\t" + value;
  	set(hsymbol, val);
  	return val;
  	}
  	
  public void readProperties( String propertiesFile)  
  {
  	//syms.loadProperties(propertiesFile);
  	try {
	 		InputStream ins = new BufferedInputStream( new FileInputStream(propertiesFile));
	 		syms.load(ins);
	 		//PropertyResourceBundle prb= new PropertyResourceBundle(ins);
	 		//if (prb != null) read( prb);
			}
		catch (Exception e) { 
			System.err.println("Error loading " + propertiesFile + ": " + e.getMessage());
			}
		 
	}
	
  
}
