// iubio/readseq/XmlPrintWriter.java

package iubio.readseq;


import java.io.*;
import java.util.*;

import flybase.*;


/**
	* PrintWriter subclass class to write XML
	*/
	
public class XmlPrintWriter 
	extends PrintWriter 
{
	public int kLinewidth= 80; // was 78 - match EMBL, set?
	public static boolean compatableXmlChars= false;
	
	protected int atline, atcol;
	protected boolean doTrimValue= true, newline= true, needindent= true;
	protected boolean headerdone;
	

	public XmlPrintWriter(Writer out) { super(out);  }

	public XmlPrintWriter(Writer out, boolean autoFlush) { super(out, autoFlush);  }

	public XmlPrintWriter(OutputStream out) { super(out); }
 
 	public XmlPrintWriter(OutputStream out, boolean autoFlush) { super(out, autoFlush);  }

	public boolean atNewline() { return newline; }
	public int linesWritten() { return atline; }
	public int atColumn() { return atcol; }
	
	public void setTrimValue(boolean turnon) { doTrimValue= turnon; }
			
	public void tab( int val) { atcol += val; for ( ; val>0; val--) super.write(' '); }

	protected void endline() {  
		atline++;  atcol= 0;
		newline= true; needindent= true;
		}

		/** dang java.io.PrintWriter for not making newline() accessible! */
	public void println() { super.println(); endline(); }
	public void println(boolean x) { super.println(x); endline(); }
	public void println(char x) { super.println(x); endline(); }
	public void println(int x) { super.println(x); endline(); }
	public void println(long x) { super.println(x); endline(); }
	public void println(float x) { super.println(x); endline(); }
	public void println(double x) { super.println(x); endline(); }
	public void println(char x[]) { super.println(x); endline(); }
	public void println(String x) { super.println(x); endline(); }
	public void println(Object x) { super.println(x); endline(); }


	public void header() {
		println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");  //?encoding
		headerdone= true;
		}
		
	public final void writeTag(String tag, String value, int level) {
		writeTagStart( tag, value, level);
		writeEndElement( tag, needindent, level);
		}

	public void writeTagStart(String tag, String value, int level)
	{
		if (value==null || (value.length() + atcol + tag.length()+2 < kLinewidth)) {
			writeStartElement( tag, false, level);
			writeValue( value);
			}
		else {
			writeStartElement( tag, false, level);
			int indent= 2*level + tag.length()+2;
			while (value!=null) { value= writeWrapText( value, indent, kLinewidth); indent= 0; }
			}
	}

	public String trimValue( String val)
	{
		if (doTrimValue) {
			val= val.trim();
			int len= val.length();
			if (len>1) {
				char c= val.charAt(0);
				char e= val.charAt(len-1);
				if (c == e && (c == '"' || c == '\'')) {
					val= val.substring(1,len-1);
					len -= 2;
					}
				}
			}
		return val;
	}
		
	public void writeValue( String val)
	{
		if (val==null) return;
		val= trimValue(val);
		writeCharacters( val, 0, val.length());
	}
	
			// override
	public String writeWrapText( String val, int indent, int width)
	{
		if (val==null) return null;
		val= trimValue(val);
		String rval= null;
		int vlen= val.length();
		int maxw= width - indent;
		int max2= maxw+2;

		//val= val.replace('\n',' '); // make sure no newlines
		int at= val.indexOf('\n'); // may01 - handle newlines
		if (at < 0) at= val.indexOf('\r');  
		if (at >= 0 && at <= max2) {
			if (at<vlen) rval= val.substring( at+1).trim();
			val= val.substring( 0, at);
			}
		else if (vlen > maxw) {
			at= val.lastIndexOf(' ', max2);
			if (at < 0) { at= val.lastIndexOf(',', max2); if (at>0) at++; }
			if (at < 0) { at= val.lastIndexOf(';', max2); if (at>0) at++; }
			if (at < 0) { at= val.lastIndexOf('.', max2); if (at>0) at++; }
			if (at < 0) at= maxw; // force break?
			if (at > 10) {
				rval= val.substring( at).trim();
				val= val.substring( 0, at);
				}
			}
		//tab(indent);
		writeCharacters( val, 0, val.length());
		if (rval!=null) println(); // don't newline if endtag waiting
		return rval;
	}


  public void writeEmptyElement(String name, int level)   
  {
		tab( 2 * level);
    super.print('<');
    super.print(name);
    //printAttributes(null);
    super.print("/>");  
    atcol += 3 + name.length();
  	println(); 
	} 
	
  //public final void writeStartElement(String name) { writeStartElement(name,true,level); }
  //public final void writeStartElement(String name, boolean eol) { writeStartElement(name, eol, level); }

  public final void writeStartElement(String name, int level)  { writeStartElement(name,true,level); }

  public void writeStartElement(String name, boolean eol, int level)   
  {
		tab( 2 * level);
    super.print('<');
    super.print(name);
    //printAttributes(null);
    super.print('>');
    atcol += 2 + name.length();
    if (eol)  println(); 
	} 

	public void printAttributes(Object attributes) // need SAX AtrributeList class here
	{
		/* if (atts!=null) {
	    int len = atts.getLength();
	    for (int i = 0; i < len; i++) {
        super.print(' ');
        super.print(atts.getName(i));
        super.print("=\"");
        super.print(atts.getValue(i));     
        super.print('"');
    		atcol += 4 + atts.getName(i) + atts.getValue(i);
        }
    	}*/
	}
	
	public final void writeEndElement(String name, int level) { writeEndElement(name,needindent,level); }
	
	protected boolean noendeol;
	public void skipNextEndElementNewline() { noendeol= true; }
	
	public void writeEndElement(String name, boolean doindent, int level) {
		if (doindent) tab( 2*level);
  	super.print("</");  super.print(name); super.print('>');  
  	if (!noendeol) println(); noendeol= false;
		} 
	
	public void commentStart()  { print("<!-- "); }
	public void commentEnd()  { println(" -->"); }

	
		//? change to print()/write() overrides?
		
  public void writeCharacters(char ch[], int start, int length) {
  	for (int i = start; i < start + length; i++) printEncoded( ch[i]);
		}  
	
  public void writeCharacters(byte ch[], int start, int length) {
  	for (int i = start; i < start + length; i++) printEncoded( (char)ch[i]);
		}  
	
  public void writeCharacters(String s, int start, int length) {
  	for (int i = start; i < start + length; i++) printEncoded( s.charAt(i));
		}  
	
	private char lastc;

	public void printEncoded(char ch) 
  {
  	needindent= newline= false; 
  	atcol++;
    switch (ch) {
      case '&': super.write("&amp;"); break;
      case '<': super.write("&lt;"); break;
      case '>': super.write("&gt;"); break;
      case '"': super.write("&quot;"); break;
      case '\t': {
          if (compatableXmlChars) super.write("&#9;");
          else super.write(ch);
          break;
          }
      case '\n': {
          if (compatableXmlChars) super.write("&#10;");
          else if (lastc!='\r') println(); //super.write(ch);  
          break;
          }
      case '\r': {
          if (compatableXmlChars) super.write("&#12;");
          else if (lastc!='\n') println(); //super.write(ch); 
          break;
          }
      default: super.write(ch);  
      } 
    lastc= ch;
	}

	//? add other XML sections
	// writeComment()
	// writeProcessorInstruction()
	
}