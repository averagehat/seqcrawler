//iubio/readseq/XmlSeqFormat.java
//split4javac// iubio/readseq/XmlSeqFormat.java date=20-May-2001

// iubio/readseq/XmlSeqFormat.java

package iubio.readseq;


import java.io.*;
import java.util.*;

import flybase.*;


//split4javac// iubio/readseq/XmlSeqFormat.java line=12
public class  XmlSeqFormat extends BioseqFormat
{
	public String formatName() { return "XML"; }  
	public String formatSuffix() { return ".xml"; } 
	public String contentType() { return "biosequence/xml"; }  //? biosequence/xml  or "text/xml"
	public boolean canread() { return hasXmlParser; }
	public boolean canwrite() { return true; }
	public boolean hasdoc() { return true; }

	public BioseqReaderIface newReader() { return new XmlSeqReader(); }
	public BioseqWriterIface newWriter() { return new XmlSeqWriter(); }

	public static String tag1= "<Bioseq"; //, tag2; //! this could be changed in XmlDoc.properties !
	
	private static boolean hasXmlParser;
	public  static String saxParserClass;
	private static String saxlParserClasses[] =
				{ "com.ibm.xml.parsers.SAXParser",
	    		"com.sun.xml.parser.Parser",
	    	 	"com.microstar.xml.SAXDriver"
				};  
				
	static { locateXmlParser(); }
	
	public static void locateXmlParser()
	{
		if (System.getProperty("TOBA")!=null) {
			saxParserClass = System.getProperty ("org.xml.sax.parser", saxlParserClasses[0]);
			hasXmlParser= true;
			}
		else for (int i= 0; !hasXmlParser && i<saxlParserClasses.length; i++) 
			try { 
			String className= saxlParserClasses[i];
			if (i==0)  className = System.getProperty ("org.xml.sax.parser", className);
			Class c= Class.forName(className); 
			if (c!=null) { saxParserClass= className; hasXmlParser= true; }
			}
			catch (Exception ce) {}
	}
	
	/* public void formatTestInit() {
		super.formatTestInit();
		if (tag1==null) try  {
			tag1=  "<" + XmlDoc.getXMLFieldName(BioseqDoc.kBioseqSet);
			//tag2=  "<" + XmlDoc.getXMLFieldName(BioseqDoc.kBioseq);
						//^ these calls to XmlDoc force loading of xml classes - avoid for testing?
			} 
		catch (Exception e) {}
		}*/
	
	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
			//? check for <?xml tag ?
		if ( line.indexOf(tag1)>=0) {  //match Bioseq or Bioseq-set 
		 	formatLikelihood += 90;
		 	return true;
		 	}
		else if ( line.indexOf("<?xml")>=0) {
		 	formatLikelihood= 40; // could be someone else's xml
		 	return false;
			}
    else
    	return false;
	}
}

