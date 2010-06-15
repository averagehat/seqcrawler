// iubio/readseq/XmlDoc.java -- was BioseqDocXml.java

package iubio.readseq;


import java.io.*;
import java.util.*;

import flybase.*;



/**
	* BioseqDoc subclass for XML data
	* @see iubio.readseq.XmlSeqReader
	* @see iubio.readseq.XmlSeqWriter
	*/
	
public class XmlDoc 
	extends BioseqDocImpl
{
	public static String xmlprop= "XmlDoc"; 
	private static FastHashtable xlabel2keys = new FastHashtable();  // format label => biodockey
	private static FastProperties	keys2xlabel= new FastProperties();  // biodockey => format label
	
	static { 
  	String pname= System.getProperty( xmlprop, xmlprop);
  	//getDocProperties(pname);
  	getDocProperties(pname,keys2xlabel,xlabel2keys);
		}
		
	public XmlDoc() { initx(); }
	
	public XmlDoc( BioseqDoc source) {
		super(source);
		fFromForeignFormat = !(source instanceof XmlDoc);
		initx();
		}
		
	public XmlDoc( String idname) { 
		super();
		addBasicName( idname);
		initx();
		}

	public void setSourceDoc(BioseqDoc source) {
		super.setSourceDoc(source);
		fFromForeignFormat = !(source instanceof XmlDoc);
		}
	
	
	
	void initx()
	{
		// some writer constants
		tagFeatureTable= getFieldName( kFeatureTable);
		tagFeatureNote= getFieldName( kFeatureNote);
		tagFeatureLocation= getFieldName( kFeatureLocation);
		tagFeatureValue= getFieldName( kFeatureValue);
		tagFeatureItem= getFieldName( kFeatureItem);
		kLinewidth= 80; //?
	}
	

		//
		// BioseqDoc reader
		//
		
	public void addDocLine(String line) //abstract
	{
		// need an XML parser here... see below, use SAX parser, not this method
		System.err.println("Use an XML parser instead of this XmlDoc.addDocLine()");
	}	


	public String getBiodockey(String field) {  return (String)xlabel2keys.get(field); }
		 
	public String getFieldName(int kind) {
		return getXMLFieldName(kind);
		}
	
	public final static String getXMLFieldName(int kind) {
		return getXMLFieldName( new Integer(kind)); 
		}

	public static String getXMLFieldName(Integer kind) {
		if (kind==null) return null;
		String lab= null; //getDoclabel( kind.intValue());
		String biodockey= getBiodockey( kind.intValue());
		if (biodockey!=null) lab= (String) keys2xlabel.get( biodockey);
		if (lab == null) {
			lab= (String) biodockinds.get(kind); 
			if (lab==null) lab= "Noname"+kind; 
			}
		return lab;
		}
	

		/**  called thru SAX parser startElement() for kFeatureItem at appearance of 1st note? */
	public void startFeature( int kind, String field, String value)
	{
		//if (!keepField(kind)) return; //?
		if (kind == BioseqDoc.kFeatureItem) {
			FeatureItem fi= new FeatureItem( field, value, kFeatField);
			this.addFeature(  fi);
			//curFieldItem= fi;
			}
	}
	
		/**  called thru SAX parser endElement() */
	public void endFeature( int kind, String field, String value)
	{
		//Debug.println("endFeature: "+kind+", "+field+", " +value);
		
		if (kind == BioseqDoc.kFeatureItem) {
			if (field!=null && field.length()>0) { //?? never append here // if (curFieldItem==null)
				FeatureItem fi= new FeatureItem( field, value, kFeatField);
				this.addFeature(  fi);
				}
			//else if (value.length()>0) { //if (append && curFieldItem != null)
			//	curFieldItem.appendValue(value); //?
			//	}
			this.curFieldItem= null; // always null out at endFeature()
			}
			
		else if (kind == BioseqDoc.kFeatureNote) {
			
			if (curFieldItem==null) {
				System.err.println("Error: null feature item for note '"+field+"', val="+value);
				return;
				}
				
			if (value!=null && value.length()>0) {
				int spc= value.indexOf(' ');
				if (spc >= 0 && "/translation".equals(field)) {
					String v= "";
					int at0= 0; 
					while (at0>=0) {
						v += value.substring(at0,spc); at0= spc+1;
					 	spc= value.indexOf(' ', at0);
					 	if (spc<0) { v += value.substring(at0); at0= -1; }
					 	}
					value= v;
					}
				else if (spc > 0 || !Character.isDigit(value.charAt(0))) 
					value= "\"" + value + "\""; //? do this on output for GB,EM features ??
				}
				
		
			if (field!=null && field.length()>0 && field.charAt(0) == '/') {
				curFieldItem.putNote( new FeatureNote(field, value, kFeatureNote, kFeatCont)); 
				}
			
			else if (value!=null && value.length()>0) {
				 // continuation?, append to last note value?
				curFieldItem.appendNote( value); 
				}
			}
	}
		
			// overrides superclass, not used now ?  
	public void addFeature( String field, String value, int level, boolean append) 
	{ 
		field= uncleanXmlTag( field);

		if (level == kFeatField) {
			if (append && curFieldItem != null)  
				curFieldItem.appendValue(value);
			else  
				addFeature( new FeatureItem(field, value, kFeatField) );
			}
			
		else if (curFieldItem!=null) {
			if (!value.startsWith("/")) curFieldItem.appendNote( value);  // append || 
			else {
				if (value!=null && value.length()>0) {
					int spc= value.indexOf(' ');
					if (spc > 0 || !Character.isDigit(value.charAt(0))) 
						value= "\"" + value + "\"";
					}
				addFeatureNote(curFieldItem, value);
			 	}
			/* 	
			if (value!=null && value.length()>0) {
				int spc= value.indexOf(' ');
				if (spc > 0 || !Character.isDigit(value.charAt(0))) 
					value= "\"" + value + "\"";
				}
		
			if (field.charAt(0) == '/') {
				curFieldItem.putNote( new FeatureNote(field, value)); 
				}
			
			else {
				 // continuation?, append to last note value?
				curFieldItem.appendNote( value); 
				}
			*/
			}
	} 
	

		//
		// BioseqDoc writer
		//

	protected XmlPrintWriter xpr;
	protected int xindent, lastlev= -1;
	protected FastStack endstack= new FastStack();
	protected String  tagFeatureTable, tagFeatureNote, tagFeatureLocation, tagFeatureValue, tagFeatureItem;
	
	public void setIndent(int indent) { this.xindent= indent; }

	public void setOutput( Writer outs)
	{
		if (outs instanceof XmlPrintWriter) {
			this.xpr= (XmlPrintWriter) outs;
			}
		else {
			BufferedWriter bufout;
			if (outs instanceof BufferedWriter) bufout=(BufferedWriter)outs; 
			else bufout= new BufferedWriter(outs); 
			this.xpr= new XmlPrintWriter(bufout);
			}
		this.pr= this.xpr;
		linesout= 0;
	}

	public void setOutput( OutputStream outs)
	{
		BufferedOutputStream bufout;
		if (outs instanceof BufferedOutputStream) bufout=(BufferedOutputStream)outs; 
		else bufout= new BufferedOutputStream(outs); 
		this.xpr= new XmlPrintWriter(bufout);
		this.pr= this.xpr;
		linesout= 0;
	}

	public int writeTo(Writer outs) { return writeTo( outs, false); } 

	public int writeTo(Writer outs, boolean doId) 
	{
		this.dontWriteId= !doId;
		setOutput( outs);
		writeAllText();
		popAllEndtags();
		pr.flush();
		return xpr.linesWritten();
	}
		

	protected void popAllEndtags() 
	{
		popend(kField);
		while (!endstack.empty()) {
			String endlab= (String) endstack.pop();
			xpr.writeEndElement( endlab, xindent);
			}
	}
	
	final void popend(int lev1) 
	{
		if ( (lastlev == kSubfield && lev1 == kField)
			 ) {  xindent--;  }
			 
		if (((lastlev == kSubfield && lev1 != kSubfield) 
				|| (lastlev == kField && lev1 != kSubfield)
				) && !endstack.empty() )  
					xpr.writeEndElement( (String) endstack.pop(), xindent);

		if ( (lastlev == kField && lev1 == kSubfield)
			 ) { xpr.println(); xindent++;  }
	}

		
	protected void writeDocItem( DocItem nv,  boolean writeAll) //int doclev, 
	{
		//int oldlev= xindent; xindent= doclev;
		int lev1= nv.getLevel(); 
		popend(lev1); lastlev= lev1;

		//String val= nv.getValue(); //? allow non-string vals, like SeqRange ?
		//if (val!=null) val= val.trim(); // got some blank vals?
		//int kind= nv.getKind();  

		switch (nv.getKind())
		{
			case kSeqdata:  return; // doing elsewhere
	
			case kFeatureTable:
				if (writeAll && !featWrit && features().size()>0) {
					xpr.writeStartElement( tagFeatureTable, xindent++);
					writeDocVector( features(), writeAll); 
					writeExtractionFeature();				
					popend(lev1); lastlev= lev1;
					xpr.writeEndElement( tagFeatureTable, --xindent); 
					featWrit= true;
					}
				break;
				
			case kFeatureItem:
				if (wantFeature(nv)) {
					xpr.writeTagStart( tagFeatureItem, nv.getName(), xindent++); // ! name is value here
					xpr.println();
					if (nv instanceof FeatureItem) { // should always be true here??
						FeatureItem fi= (FeatureItem) nv;
						xpr.writeTag( tagFeatureLocation, nv.getValue(), xindent); //val == fi.getLocationString() now
						if (fi.notes != null) writeDocVector( fi.notes,false);  
						}
					else if (nv.hasValue())  // are there any chars now in feat item???
						xpr.writeTag( tagFeatureValue, nv.getValue().trim(), xindent);
					xpr.writeEndElement( tagFeatureItem, --xindent);  
					}
				break;

 			case kFeatureNote:
				xpr.writeTagStart( tagFeatureNote, nv.getName(), xindent++); // ! name is value here
				if ( nv.hasValue() ) 	{
					int vlev= 0;
					String val= nv.getValue().trim();
					int lwidth= val.length() + tagFeatureValue.length() + 2 + xpr.atColumn();  
					if (lwidth > xpr.kLinewidth) { xpr.println(); vlev= xindent; }
					boolean noendeol= (lwidth + tagFeatureValue.length() + 2 < xpr.kLinewidth);
					if (noendeol) xpr.skipNextEndElementNewline();
					xpr.writeTag( tagFeatureValue, val, vlev);
					}
				xpr.writeEndElement( tagFeatureNote, --xindent); 
				break;
			
			default:
				String tag= getFieldLabel( lev1, nv);  
				if (tag!=null) { 
					//String val= getFieldValue( nv);
					String val= nv.getValue().trim();
					xpr.writeTagStart( tag, val, xindent);
					if ( lev1 == kField ) endstack.push(tag); else 
					xpr.writeEndElement( tag, xindent);
					}
				break;
		}
				
 		//xindent= oldlev;
	}


	protected String cleanXmlTag(String tag) {
		//if (tag.startsWith("/")) tag= tag.substring(1); // for feature tables
		//^^ don't drop '/' otherwise can dup other tags - change to _
		if (Character.isDigit(tag.charAt(0))) tag= "N" + tag;
		char[] buf= tag.toCharArray();
		for (int i= 0; i<buf.length; i++) {
			char c= buf[i];
			if (! (Character.isLetterOrDigit(c) 
					|| c == '_' || c == '-' || c == '.' || c == ':'
					)) buf[i]= '_';
			}
		return new String(buf);
	}

	protected String uncleanXmlTag(String tag) {
		if (tag.startsWith("_")) tag= "/" + tag.substring(1);
		else if (tag.startsWith("N") && Character.isDigit(tag.charAt(1)))
			tag= tag.substring(1);
		//if (tag.equals("5_UTR")) tag= "5'UTR";  
		//else if (tag.equals("3_UTR")) tag= "3'UTR"; 
		return tag;
	}
	
	
	protected String getFieldLabel( int level, DocItem di) //int level, int kind, String name, int valkind
	{
		//if (name.startsWith("/")) name= name.substring(1);
		String name= null;
		switch (level) {
			case kContinue  :
			case kSubfield  : 
			case kField     :  
				if (fFromForeignFormat) name= getFieldName( di.getKind()); // preserve orig name if null?
				else name= di.getName();
				//Debug.println("XML.getFieldLabel for " +di + ", lab="+name);
				if ( name==null || name.length()==0 ) return null; //? 
				break;
			
			//case kFeatField :  
			//case kFeatCont  :  
		  //case kFeatWrap  :  
			default: name= di.getName(); break;
			}
		name= cleanXmlTag( name);
		return name;
	}


	
};