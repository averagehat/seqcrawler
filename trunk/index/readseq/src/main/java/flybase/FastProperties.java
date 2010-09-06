//flybase/FastProperties.java
//split4javac// flybase/FastProperties.java date=07-Jun-2003

// flybase.FastProperties
/* from 
 * @(#)Properties.java	1.29 97/01/28
 */

package flybase; // flybase.util ?

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.InputStreamReader;
import java.io.BufferedReader;
//import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Date;


public
class FastProperties extends FastHashtable // Hashtable 
{
	public static String defaultRezpath = "rez/";
	
	protected FastProperties defaults;

  public FastProperties() { }

	public FastProperties(FastProperties defaults) { this.defaults = defaults; }

  // jun03 - was public void loadProperties(String propname)
  // change broke old drawseq - recompile? cannot overload function result
  
	public boolean loadProperties(String propname)
	{
		try {
     	String baseFileName= propname;
     		//?? drop this .props and ./ name fiddling? expect caller to know proper name?
     	int at= baseFileName.indexOf(".properties");
     	if (at>0) baseFileName= baseFileName.substring(0,at);
     	baseFileName = baseFileName.replace('.', '/');
      baseFileName +=  ".properties";
      
    	InputStream stream= null;
			ClassLoader cl = this.getClass().getClassLoader(); 
			//if (cl == null) cl= ClassLoader.getSystemClassLoader(); // j1.2 !
    	try { 
				if (cl==null)
					stream= ClassLoader.getSystemResourceAsStream(baseFileName); // throws Exception in j12!
				else 
					stream= cl.getResourceAsStream(baseFileName);
    		}
    	catch (Exception ex2) {}
    	if ( stream == null ) {
				if (cl==null)
					stream= ClassLoader.getSystemResourceAsStream( defaultRezpath+baseFileName); // throws Exception in j12!
				else
					stream= cl.getResourceAsStream( defaultRezpath+baseFileName);
    		}
    	if ( stream != null ) {
    		load( stream);
    		stream.close();  
    		return true;
    		}
  		}
  	catch (Exception e) {
  		System.err.println("Error loading " + propname + ": " + e.getMessage());
  		}
 		return false;
	}
	

	public void load(InputStream in) throws IOException 
  {
		//? in = Runtime.getRuntime().getLocalizedInputStream(in);

		int ch = in.read();
		while (true) {
			switch (ch) {
		  	case -1:
					return;

		    case '#':
		    case '!':
					do {
					    ch = in.read();
					} while ((ch >= 0) && (ch != '\n') && (ch != '\r'));
					continue;

	      case '\n':
	      case '\r':
	      case ' ':
	      case '\t':
					ch = in.read();
					continue;
		    }

		    // Read the key
			StringBuffer key = new StringBuffer();
			while ((ch >= 0) && (ch != '=') && (ch != ':') && 
			   (ch != ' ') && (ch != '\t') && (ch != '\n') && (ch != '\r')) {
					key.append((char)ch);
					ch = in.read();
					}
			while ((ch == ' ') || (ch == '\t')) {
				ch = in.read();
		    }
			if ((ch == '=') || (ch == ':')) {
				ch = in.read();
		    }
			while ((ch == ' ') || (ch == '\t')) {
				ch = in.read();
		    }

		    // Read the value
	    StringBuffer val = new StringBuffer();
	    while ((ch >= 0) && (ch != '\n') && (ch != '\r')) {
				int next = 0;
				if (ch == '\\') {
			    switch (ch = in.read()) {
			      case '\r':
							if (((ch = in.read()) == '\n') ||
							    (ch == ' ') || (ch == '\t')) {
							  // fall thru to '\n' case
							} else continue;
			      case '\n': 
							while (((ch = in.read()) == ' ') || (ch == '\t'));
							continue;
			      case 't': ch = '\t'; next = in.read(); break;
			      case 'n': ch = '\n'; next = in.read(); break;
			      case 'r': ch = '\r'; next = in.read(); break;
			      case 'u': {
							while ((ch = in.read()) == 'u');
							int d = 0;
						loop:
							for (int i = 0 ; i < 4 ; i++) {
						    next = in.read();
						    switch (ch) {
						      case '0': case '1': case '2': case '3': case '4':
						      case '5': case '6': case '7': case '8': case '9':
										d = (d << 4) + ch - '0';
										break;
						      case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
										d = (d << 4) + 10 + ch - 'a';
										break;
						      case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
										d = (d << 4) + 10 + ch - 'A';
										break;
						      default:
										break loop;
						    	}	
						    ch = next;
								}
							ch = d;
							break;
							}
			      default: next = in.read(); break;
			    }
			} else {
			    next = in.read();
			}
			val.append((char)ch);
			ch = next;
			}

		put(key.toString(), val.toString());
		}
  }

		// dgg addition
	/*
	public Enumeration sortedkeys() { return sortedkeys(this); }
	
	public Enumeration sortedkeys(FastHashtable h) {
		SortedEnumeration sen= new SortedEnumeration( h.keys());
		return sen.elements();
		}
	*/
		
	public synchronized void save( OutputStream out, String header) {
		save( new PrintWriter( out), header, false); }
	public synchronized void save( Writer out, String header) {
		save( new PrintWriter( out), header, false); }
		
	public synchronized void save( PrintWriter prnt, String header, boolean addDefaults) 
	{
			//OutputStream localOut = out; //Runtime.getRuntime().getLocalizedOutputStream(out);
			//PrintStream prnt = new PrintStream(localOut, false);
			boolean localize = true; //localOut != out;			
			if (header != null) {
			    prnt.write('#');
			    prnt.println(header);
			}
			
			prnt.write('#');
			try { prnt.print(new Date()); }  catch (Exception e) {} // dgg, catch MWerks Date() bug
			prnt.println();
			 
			FastHashtable h;
			if (addDefaults) {
				h = new FastHashtable();
				enumerate(h); // adds defaults !
				}
			else h= this;
			for (Enumeration e = h.keys(); //sortedkeys(h) ; 
				e.hasMoreElements() ;) {
			    String key = (String)e.nextElement();
			    prnt.print(key);
			    prnt.write('=');

			    //String val = (String) h.get(key);
			    String val = h.get(key).toString(); // to be safe...
			    int len = val.length();
			    boolean empty = false; //? to keep spaces - never true

			    for (int i = 0 ; i < len ; i++) {
				int ch = val.charAt(i);

				switch (ch) {
				  case '\\': prnt.write('\\'); prnt.write('\\'); break;
				  case '\t': prnt.write('\\'); prnt.write('t'); break;
				  case '\n': prnt.write('\\'); prnt.write('n'); break;
				  case '\r': prnt.write('\\'); prnt.write('r'); break;

				  default:
				    if ((ch < ' ') || (ch >= 127) || (empty && (ch == ' '))) {
					if ((ch > 255) && localize) {
					    prnt.write(ch);
					} else {
					    prnt.write('\\');
					    prnt.write('u');
					    prnt.write(toHex((ch >> 12) & 0xF));
					    prnt.write(toHex((ch >>  8) & 0xF));
					    prnt.write(toHex((ch >>  4) & 0xF));
					    prnt.write(toHex((ch >>  0) & 0xF));
					}
				    } else {
					prnt.write(ch);
				    }
				}
				empty = false;
			    }
			    //prnt.write('\n'); //!? or prnt.println() for local newline
			    prnt.println();
			}
			prnt.flush();
    }


    public String getProperty(String key) {
			String val = (String)super.get(key);
			return ((val == null) && (defaults != null)) ? defaults.getProperty(key) : val;
    }

    public String getProperty(String key, String defaultValue) {
			String val = getProperty(key);
			return (val == null) ? defaultValue : val;
 	   }

    public Enumeration propertyNames() {
			FastHashtable h = new FastHashtable();
			enumerate(h);
			return h.keys();
   	 }


    public void list( PrintStream out) {
    	save( new PrintWriter( out), "-- listing properties --", true);
    	/*
			out.println("-- listing properties --");
			FastHashtable h = new FastHashtable();
			enumerate(h);
			// dgg here
			for (Enumeration e = sortedkeys(h) ; e.hasMoreElements() ;) {
			    String key = (String)e.nextElement();
			    String val = (String)h.get(key);
			    //? if (val.length() > 80) val = val.substring(0, 77) + "...";
			    out.println(key + "=" + val);
			}
			*/
    }

    public void list(PrintWriter out) {
     	save( out, "-- listing properties --", true);
    	/*
			out.println("-- listing properties --");
			FastHashtable h = new FastHashtable();
			enumerate(h); // adds defaults!
			// dgg here
			for (Enumeration e = sortedkeys(h) ; e.hasMoreElements() ;) {
			    String key = (String)e.nextElement();
			    String val = (String)h.get(key);
			    //? if (val.length() > 80) val = val.substring(0, 77) + "...";
			    out.println(key + "=" + val);
			}
 			*/
   }

    private  void enumerate(FastHashtable h) { //synchronized
			if (defaults != null) { defaults.enumerate(h); }
			for (Enumeration e = keys() ; e.hasMoreElements() ;) {
			    String key = (String)e.nextElement();
			    h.put(key, get(key));
			}
    }

    private static char toHex(int nibble) {
			return hexDigit[(nibble & 0xF)];
    }

    private static char[] hexDigit = {
			'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
}
//split4javac// flybase/FastProperties.java line=305

