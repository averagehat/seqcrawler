// flybase/utils.java
// d.g.gilbert 


package flybase;
// pack edu.indiana.bio.dmap

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;




public final class Utils {

	public final static String[] splitString(String val) {
		return splitString(val,"\n\r\t", -1);
		}	
		
	public final static String[] splitString(String val, String delims) {
		return splitString(val, delims, -1); 
		}
		
	public static String[] splitString(String val, String delims, int maxparts) 
	{
		//? replace w/ OpenString algorithm?
		if (val==null) return null;
		StringTokenizer st= new StringTokenizer(val,delims);
		int i, n= st.countTokens();
		String[] ss;
		boolean getrem;
		if (maxparts>0 && n>maxparts) { 
			n= maxparts-1; getrem= true; 
			ss= new String[maxparts];
			}
		else {
			getrem= false;
			ss= new String[n];
			}
		for ( i=0; i<n; i++) ss[i]= st.nextToken();
		if (getrem) {
			val= st.nextToken("\000");
			if (val.length()>0 && delims.indexOf(val.charAt(0))>=0) val= val.substring(1);
			ss[n]= val;
			}
		return ss;
	}
	
	private final static int skipDelimiters(OpenString ost, String delims, int at, int len) {
		while ( (at < len) && (delims.indexOf(ost.charAt(at)) >= 0)) at++;
		return at;
    }

	public final static OpenString[] splitString(OpenString val) {
		return splitString(val,"\n\r\t", -1);
		}	
		
	public final static OpenString[] splitString(OpenString val, String delims) {
		return splitString(val, delims, -1); 
		}

	public static OpenString[] splitString(OpenString val, String delims, int maxparts) 
	{
		if (val==null) return null;
		FastVector v= new FastVector();
		int len= val.length();
		int at= 0, np= 0;
		while (at<len) {
			if (maxparts>0 && np>=maxparts) {
				OpenString endval= val.substring(at);
				if (endval.length()>0 && delims.indexOf(endval.charAt(0))>=0) 
					endval= endval.substring(1);
				v.addElement(endval);
				at= len; break;
				}
			else {
				at= skipDelimiters( val, delims, at, len);
				if (at < len) {
					int at0= at;
					while ((at < len) &&  (delims.indexOf(val.charAt(at)) < 0))  at++;
					v.addElement( val.substring(at0, at));
					np++;
					}
				}
			}
		OpenString[] ss= new OpenString[v.size()];
		v.copyInto(ss);
		return ss;
	}

	public static String joinStrings(String[] vals, String delim) {
		if (vals==null) return null;
		StringBuffer sb= new StringBuffer();
		boolean first= true;
		for (int i=0; i<vals.length; i++) if (vals[i]!=null) {
			if (!first) sb.append(delim); first= false;
			sb.append(vals[i]);
			}
		return sb.toString();
		}

	public static String joinStrings(OpenString[] vals, String delim) {
		if (vals==null) return null;
		StringBuffer sb= new StringBuffer();
		boolean first= true;
		for (int i=0; i<vals.length; i++) if (vals[i]!=null) {
			if (!first) sb.append(delim); first= false;
			sb.append(vals[i].toString()); //! argh
			}
		return sb.toString();
		}

	public static String removeChars(String s, String remove) 
	{
		StringBuffer sb= new StringBuffer();
		for (int i=0; i<s.length(); i++) {
			char c= s.charAt(i);
			if (remove.indexOf(c)<0) sb.append(c);
			}
		return sb.toString();
	}

	public static int countChars(String s, String charset) 
	{
		int nc= 0;
		for (int i=0; i<s.length(); i++) {
			char c= s.charAt(i);
			if (charset.indexOf(c)>=0) nc++;
			}
		return nc;
	}
	
	public static int countChar(String s, char c) 
	{
		int nc= 0;
		for (int i=0; i<s.length(); i++) {
			if (c == s.charAt(i)) nc++;
			}
		return nc;
	}
		
		// java 1.2 String.hashCode()
	public static int stringHashCode(String s) {
		int h = 0;
		int len = s.length();
		for (int i = 0; i < len; i++)  h = 31*h + s.charAt(i);
		return h;
		}

		
	public static String quote(String s) {
		if (s.indexOf(' ')>=0) {
			char dqc= '"', sqc= '\'';
			int len= s.length();
			if (s.charAt(0) == dqc && s.charAt(len-1) == dqc) return s;
			if (s.charAt(0) == sqc && s.charAt(len-1) == sqc) return s;
			String qs= (s.indexOf(dqc)>=0) ? "'" : "\"";
			return qs + s + qs; 
			}
		else return s;
		}

	public final static String stuffLink(String href, String linkid)
	{
		return stuffLink(href, linkid, false);
	}
	
	public static String stuffLink(String href, String linkid, boolean noencode)
	{
				//! this is called a lot - and is using 16% of runtime -- optimize &/or reduce # calls
		// handle these variants
		//<a href=http:///dbsts_query?QUERY=%s[nc]>" -- new
		//<a href=http:///dbsts_query?QUERY=%s%s[nc]>%s</a>" -- old, drop 1st %s and after >
		if (!noencode) linkid= URLEncoder.encode( Utils.quote(linkid));
		StringBuffer sb= new StringBuffer();
		int at= href.indexOf("%s");  
		if (at>=0) {
			sb.append( href.substring(0,at));
			at += 2;
			int at2= href.indexOf("%s", at);
			if (at2>0) {
				sb.append( href.substring(at, at2));
				sb.append( linkid);
				at2 += 2;
				int close= href.indexOf(">", at2);
				if (close>0) {
					sb.append( href.substring( at2, close+1));
					}
				else {
					at= href.indexOf("%s", at2);
					if (at > 0) {
						sb.append( href.substring( at2, at));
						sb.append( href.substring( at+2));
						}
					else 
						sb.append( href.substring(at2));
					}
				}
			else {
				sb.append( linkid);
				sb.append( href.substring(at));
				}
		 	}
		else {
			sb.append(href);
			sb.append(linkid);
			} 
		return sb.toString();
	}


	public static boolean isUrl(String s)
	{
		if (true) {
				try { URL u= new URL(s);  return (u!=null); }
				catch (MalformedURLException e) { return false; }
		} else {
				if (s==null) return false;
				s= s.toLowerCase();
				if (s.startsWith("http://")) return true;
				else if (s.startsWith("ftp://")) return true;
				else if (s.startsWith("file:")) return true;
					// what other forms does java url handle?
				else return false;
		}
	}
		
	public static String stripHtmlCodes(String s)
	{
		int a= s.indexOf('<');
		if (a >= 0) {
			int b= s.indexOf('>', a);
			if (b>a) {
				StringBuffer sb= new StringBuffer(s.substring(0,a));
				int len= s.length();
				int a0= b+1;
				a= (a0>=len) ? -1 : s.indexOf('<',a0);
				while (a>0) {
					b= s.indexOf('>',a);
					if (b>a) {
						sb.append(s.substring(a0, a));
						a0= b+1;
						a= (a0>=len) ? -1 : s.indexOf('<',a0);
						}
					else a= -1;
					}
				if (a0<len)  sb.append(s.substring(a0));
				return sb.toString();
				}
			}
		return s;
	}



	static Hashtable htmlcodes, isocodes;
													
  static void initHtmlCodes()
	{		
		htmlcodes = new Hashtable();
		htmlcodes.put("<p>","\n");
		htmlcodes.put("<br>","\n");
		htmlcodes.put("<hr>","\n---------------------------\n");
		htmlcodes.put("<sup>","[");
		htmlcodes.put("</sup>","]");
		htmlcodes.put("<sub>","[[");
		htmlcodes.put("</sub>","]]");

		isocodes = new Hashtable();
		isocodes.put("&lt;","<");
		isocodes.put("&gt;",">");
		isocodes.put("&amp;","&");
		isocodes.put("&quot;","\"");
		isocodes.put("&reg;","TM");
		isocodes.put("&copy;","(C)");
		isocodes.put("&nbsp;"," ");
	}
	
	public static String convertHtmlCode(String s) {
		if (htmlcodes==null) initHtmlCodes();
		return (String) htmlcodes.get(s.toLowerCase());  // null if no match
		}
	
	public static String convertIsoCode(String s) {
		if (isocodes==null) initHtmlCodes();
		String cs= (String) isocodes.get(s.toLowerCase());
		if (cs!=null) return cs;
		else {
			if (s.length()>4 && s.charAt(1)=='#') 
			try {
				int c= Integer.parseInt(s.substring(2,5));
				cs= new Character( (char)c).toString();
				return cs;
				}
			catch (Exception e) {}
			}
		return s; // original if no match
		}
		
	public static String isoCodesToText(String s)
	{
		int a= s.indexOf('&');
		if (a >= 0) {
			int b= s.indexOf(';', a);
			if (b>a && b<a+8) {
				StringBuffer sb= new StringBuffer(s.substring(0,a));
				String hcode= convertIsoCode(s.substring(a,b+1));
				if (hcode!=null) sb.append(hcode);
				int len= s.length();
				int a0= b+1;
				a= (a0>=len) ? -1 : s.indexOf('&',a0);
				while (a>0) {
					b= s.indexOf(';',a);
					if (b>a && b<a+8) {
						sb.append(s.substring(a0, a));
						hcode= convertIsoCode(s.substring(a,b+1));
						if (hcode!=null) sb.append(hcode);
						a0= b+1;
						a= (a0>=len) ? -1 : s.indexOf('&',a0);
						}
					else a= -1;
					}
				if (a0<len)  sb.append(s.substring(a0));
				return sb.toString();
				}
			}
		return s;
	}
		
	public static String htmlToText(String s)
	{
		int a= s.indexOf('<');
		if (a >= 0) {
			int b= s.indexOf('>', a);
			if (b>a) {
				StringBuffer sb= new StringBuffer(s.substring(0,a));
				String hcode= convertHtmlCode(s.substring(a,b+1));
				if (hcode!=null) sb.append(hcode);
				int len= s.length();
				int a0= b+1;
				a= (a0>=len) ? -1 : s.indexOf('<',a0);
				while (a>0) {
					b= s.indexOf('>',a);
					if (b>a) {
						sb.append(s.substring(a0, a));
						hcode= convertHtmlCode(s.substring(a,b+1));
						if (hcode!=null) sb.append(hcode);
						a0= b+1;
						a= (a0>=len) ? -1 : s.indexOf('<',a0);
						}
					else a= -1;
					}
				if (a0<len)  sb.append(s.substring(a0));
				s= sb.toString();
				}
			}
		return isoCodesToText(s);
	}
	
	public static String titleToSentenceCase(String s) {
			//if (isupper(cp) && !isupper(cp[1])) cp= tolower(cp);
		if (s.length()>1 
			&& Character.isUpperCase( s.charAt(0)) 
			&& Character.isLowerCase( s.charAt(1))
			) return s.toLowerCase();
		return s;
		}
		
	public static OpenString titleToSentenceCase(OpenString s) {
			//if (isupper(cp) && !isupper(cp[1])) cp= tolower(cp);
		if (s.length()>1 
			&& Character.isUpperCase( s.charAt(0)) 
			&& Character.isLowerCase( s.charAt(1))
			) return new OpenString( s.toLowerCase());
		return s;
		}
		
	public static OpenString trimSentenceEnd(OpenString s, int cut) {
		int at= s.indexOf(". ");
		if (at<0) { 
			int lat= s.length()-1;
			if (lat>=0 && s.charAt(lat) == '.') at= lat; 
			}
		if (cut<0 || (at>0 && at<cut)) cut= at;
		if (cut>=0) s= s.substring(0,cut).trim();
		return s;
		}
		
	public static String trimSentenceEnd(String s, int cut) {
		if (cut<0) cut= s.indexOf(". ");
		if (cut<0) { int lat= s.lastIndexOf("."); if (lat == s.length()-1) cut= lat; }
		if (cut>=0) s= s.substring(0,cut).trim();
		return s;
		}

		
	public static String fileSizeLabel(long size, boolean brief)
	{
		String s, lb;
		if (size >= 1024000000L) { 
			s= Long.toString( size / 102400000L);
			lb= (brief ? "Gb" : " Gigabytes");
			}
		else if (size >= 1024000L) {
			s= Long.toString( size / 102400L);
			lb= (brief ? "Mb" : " Megabytes");
			}
		else if (size >= 1024L) {
			s= Long.toString( size / 100L);
			lb= (brief ? "kb" : " kilobytes");
			}
		else 
			return Long.toString(size) + (brief ? "b" : " bytes");
		
		int l= s.length() - 1;
		if (l>0) {
			String d= s.substring(l);
			s= s.substring(0,l);
			if (l < 2 && !d.equals("0")) s += "." + d;
			}
		return s + lb;
	}
	
	final static String spaces = 
		new String("                                            ");

	public static String formatString( String s, int width)
	{
		int len= s.length();
		if (width > len) s = spaces.substring(0,width-len) + s;
		else if (-width > len) s = s + spaces.substring(0,(-width)-len);
		return s;
	}

	public final static String formatNum( int num, int width) {
		return formatString( String.valueOf(num), width);
		}

	public final static String formatDouble( double num, int width, int precision) {
		return formatReal(String.valueOf(num), width, precision); // bad for 7.6e-05 or other smallnums !?
		}
	
	public final static String formatFloat( float num, int width, int precision) {
		return formatReal(String.valueOf(num), width, precision);
		}
	
	public static String formatReal( String s, int width, int precision)
	{
		int dec= s.indexOf('.');
		if (dec>0) {
			int len= s.length();
			while (len>dec && s.charAt(len-1) == '0') len--; // chop trailing 00
			if (len-1 == dec || precision==0) len= dec; // drop .
			else if (precision>0) {
				dec += precision + 1;  
				if (dec < len) len= dec;
				}
			s= s.substring(0,len);
			}
		return formatString( s, width);
	}
	
	public static int hexValue(String val) {
		val= val.trim();
		int i= val.indexOf("0x");
		if (i<0) i= val.indexOf("0X");
		if (i>=0) i += 2;
		if (i>0) val= val.substring(i);
		try { return Integer.parseInt(val,16); } 
		catch (NumberFormatException ex) { return 0; }
		}

	public static long hexLongValue(String val) {
		val= val.trim();
		int i= val.indexOf("0x");
		if (i<0) i= val.indexOf("0X");
		if (i>=0) i += 2;
		if (i>0) val= val.substring(i);
		return Long.parseLong(val,16);
		}

	public static BitSet bitset(String s) 
	{
		BitSet bs= new BitSet();
		String[] ss= splitString(s," ,\t\n");
		for (int i=0; i<ss.length; i++) {
			try { bs.set( Integer.parseInt(ss[i])); } 
			catch (Exception e) {}
			}
		return bs;
	}
		
	public static boolean findString(String[] list, String target, boolean partmatch)
	{
		if (list==null || target==null) return false;
		for (int i=0; i<list.length; i++) {
			if (partmatch) { if (target.indexOf(list[i])>=0) return true; }
			else if (target.equals(list[i])) return true;
			}
		return false;
	}
	

	public static String decode(String str) 
		//  part of the HTTPClient package, Copyright (C) 1996,1997  Ronald Tschalaer
	{
		if (str == null)  return null;
		char[] res  = new char[str.length()];
		int didx = 0;
		for (int sidx=0; sidx<str.length(); sidx++) {
			char ch = str.charAt(sidx);
			if (ch == '+') res[didx++] = ' ';
			else if (ch == '%') {
				try {
					res[didx++] = (char)
					  Integer.parseInt(str.substring(sidx+1,sidx+3), 16);
				 	sidx += 2;
					}
				catch (NumberFormatException e) {
					// throw new ParseException()
					Debug.println( "urlDecode:"+ str.substring(sidx,sidx+3) + " is an invalid code");
					}
				}
			else
				res[didx++] = ch;
		}
		return String.valueOf(res, 0, didx);
	}

	protected final static int pipesize= 2048;
	public static void pipeStream(InputStream ins, OutputStream ous) 
	{
		if (ous!=null && ins!=null) try { 
			int len;
			byte[] pipe= new byte[pipesize];
			do {
				len= ins.read(pipe);
				if (len>0) ous.write(pipe,0,len);
			} while (len >= 0);
			ous.flush(); //? not close()
			ins.close();
			}
		catch (IOException ex) {}
	}


	public static void pipeTextStream(InputStream ins, OutputStream ous) 
	{
		if (ous!=null && ins!=null) {
		try { 
				// buffered r/w much faster than DataInputStream readLine - is it bad?
			BufferedReader rdr= new BufferedReader( new InputStreamReader(ins));
			BufferedWriter wtr= new BufferedWriter( new OutputStreamWriter(ous));
			String s;
			while ((s= rdr.readLine()) != null) { 
				wtr.write( s); wtr.newLine();  
				} 
			wtr.flush(); 
			ins.close();
			ous.flush();
			//ous.close(); //? caller may want to write more to ous
			}
		catch (IOException ex) {}
		}
	}


	public static void writeObject(OutputStream os, Object ob, boolean dogzip)
	{
		try {
			GZIPOutputStream  gzos;
			if (dogzip) { gzos= new GZIPOutputStream(os); os= gzos; }
			ObjectOutputStream out= new ObjectOutputStream(os);
			out.writeObject(ob);
			out.flush();
			if (dogzip) out.close(); //! must do to finish up gzip
			}
		catch (Exception ex) { 
			System.err.println(ex); 
			if (Debug.isOn) ex.printStackTrace();
			}
	}
	
	
	public static Object readObject(InputStream in, boolean dogzip)
	{
		Object ob= null;
		try {
			GZIPInputStream gzis = null;
			// ? test in for GZIP format !?
			if (dogzip) { gzis= new GZIPInputStream(in); in= gzis; }
			ObjectInputStream oin= new ObjectInputStream(in);
			ob= oin.readObject();
			//oin.close();
			}
		catch (Exception ex) { 
			System.err.println(ex);  
			if (Debug.isOn) ex.printStackTrace();
			}
		return ob;
	}


	private final static int Crypt1 = 52895, Crypt2 = 22419;
	private final static int Cryptkey= 23647;

	public final static String encrypt(String s) { return encrypt(s,Cryptkey); }
	public final static String decrypt(String s) { return decrypt(s,Cryptkey); }

 	public static String encrypt(String s, long key)
	{
		if (s==null || s.length()<1) return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
		for (int i=0; i < s.length(); i++) {
			char decr= (char) s.charAt(i);
   		char encr= (char)((decr ^ key) & 0xff);
			key= (long) ((encr + key) * Crypt1 + Crypt2);
			out.write('%');
			out.write(Character.forDigit(encr >> 4, 16));
			out.write(Character.forDigit(encr & 0xF, 16));
			}
		return out.toString();
	}
	
	public static String decrypt(String s, long key)
	{
		if (s==null || s.length()<1) return null;
		char[] buf= new char[s.length()];
		int buflen= 0;
		for (int i=0; i<s.length(); i++) {
			char ch = s.charAt(i);
			if (ch == '%') try {
				buf[buflen++] = (char) Integer.parseInt(s.substring(i+1,i+3), 16);
			 	i += 2;
				}
			catch (NumberFormatException e) {
				throw new InternalError( "decrypt: invalid code");
				}
			else
				buf[buflen++] = ch;
			}
		for (int i=0; i < buflen; i++) {
			char encr= (char) buf[i];
   		char decr= (char) ((encr ^ key) & 0xff);
			key= (long) ((encr + key) * Crypt1 + Crypt2);
			buf[i]= decr;
			}
		return String.valueOf(buf, 0, buflen);
	}
	

};
