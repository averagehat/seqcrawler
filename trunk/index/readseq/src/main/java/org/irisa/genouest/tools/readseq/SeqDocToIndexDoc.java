package org.irisa.genouest.tools.readseq;

import iubio.readseq.BioseqDoc;
import iubio.readseq.BioseqDocImpl;
import iubio.readseq.DocItem;
import iubio.readseq.FeatureItem;

import java.io.Writer;

import org.apache.tika.sax.XHTMLContentHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import flybase.FastHashtable;
import flybase.FastProperties;
import flybase.FastVector;

// make this static ??
public class SeqDocToIndexDoc extends BioseqDocImpl
{
  // use doc tags from readseq rez/XmlDoc.properties
	public  static String gffprop= "XmlDoc"; //? "SeqDocToLuceneDoc"; 
	public  static String attribInSeparator= "; ";
	public  static String attribOutSeparator= " ; ";
	private static  FastHashtable elabel2keys = new FastHashtable();  // format label => biodockey
	private static FastProperties	keys2elabel= new FastProperties();  // biodockey => format label
	
	String sversion;
	float version;
	
	private Logger log = LoggerFactory.getLogger(SeqDocToIndexDoc.class);
	
	static { 
  	String pname= System.getProperty( gffprop, gffprop);
  	getDocProperties(pname,keys2elabel,elabel2keys);
		}

	//XHTMLContentHandler handler=null;
	
	/*public SeqDocToIndexDoc(XHTMLContentHandler contentHandler) {
		handler = contentHandler;
	}*/
	public SeqDocToIndexDoc() {
		
	}
	

	public void setSourceDoc(BioseqDoc source)
	{
		super.setSourceDoc(source);
		fFromForeignFormat = !(source instanceof SeqDocToIndexDoc);
	}
		
	//public String getID() { return getDocField(kName); } //"ID"
	//public String getTitle() { return getDocField(kDescription); } //"DE"
		
	String lastgroup;
	FastVector vattr= new FastVector();
	
	boolean  goodval(String v) {
		return (v!=null && (v.length()>1 
			|| (v.length()==1 && ! ("-".equals(v) || ".".equals(v) || " ".equals(v))) ));
		}
		
	public void addDocLine(String line) 
	{} // no read



	public void addDocField(String field, String val, int level, boolean append) 
	{} // no read
	
	
	BioseqParser lucindexer;
	
	
	public void writeContentHandler(BioseqParser lucindexer, Writer outs)
	{
	  this.lucindexer= lucindexer;
	  this.writeTo(outs);
	}
	
 	
	protected String keyvalsep;
	protected int keyvalat;
	
	protected boolean writeKeyValue( DocItem di ) // override
	{
		if (lastkind==kFeatureTable) return true;
		String lab= getFieldLabel( di); // lev1, kind, name, di.hasValue() 
		String val= getFieldValue( di); //val, lev1, kind, name
		
		if (lab!=null) { 
				{
				if (keyvalat>0 && keyvalsep!=null) pr.print(keyvalsep);
				keyvalat++;
				pr.print( lab );
				pr.print( val);
				}
			return true;
			}
		else return false;
	}

	protected DocItem accItem, idItem;
	int lastkind, partid;
	//String seqname;
	boolean didtop;
	String lastId;
	
	protected void writeDocItem( DocItem nv, boolean writeAll)
	{
		if (linesout==0) { 
			//pr.println("##gff-version 2"); linesout++;  
			partid= 0; 
			}
			
		int kind= nv.getKind();
		int lev1= nv.getLevel(); 
		lastkind= kind; 
		String tag, val;
		
		switch (kind) 
		{
			//case kTitle:  
			//case kDate:  
			case kSeqdata:  return; // do elsewhere

			//case kAccession:  accItem= nv; break;
			//case kName:	 idItem= nv;  break;

			default:
			  tag= getFieldLabel( lev1, nv);  
			  val= (nv.hasValue()) ? nv.getValue().trim() : "";
				if (tag!=null && val.length()>0) { 
          lucindexer.addIndexField( tag, val); //? only callback
					}
				break;

			case kFeatureTable:
				if (writeAll && !featWrit && features().size()>0) {
					//xpr.writeStartElement( tagFeatureTable, xindent++);
					writeDocVector( features(), writeAll); 
					writeExtractionFeature();				
					//popend(lev1); lastlev= lev1;
					//xpr.writeEndElement( tagFeatureTable, --xindent); 
					featWrit= true;
					}
				break;
				
			case kFeatureItem:
			  tag= getFieldLabel( lev1, nv);  
			  val= (nv.hasValue()) ? nv.getValue().trim() : "";
				if (tag!=null && val.length()>0) { //&& wantFeature(nv)
					//xpr.writeTagStart( tagFeatureItem, nv.getName(), xindent++); // ! name is value here
					//xpr.println();
					if (nv instanceof FeatureItem) { // should always be true here??
						FeatureItem fi= (FeatureItem) nv;
            lucindexer.addIndexField( tag, val); // force to use NumericFilter 
						//xpr.writeTag( tagFeatureLocation, nv.getValue(), xindent); //val == fi.getLocationString() now
						if (fi.getNotes() != null) writeDocVector( fi.getNotes(), false);  
						}
					else {  // are there any chars now in feat item???
            lucindexer.addIndexField( tag, val);  
						//xpr.writeTag( tagFeatureValue, nv.getValue().trim(), xindent);
						}
					//xpr.writeEndElement( tagFeatureItem, --xindent);  
					}
				break;

 			case kFeatureNote:
				//xpr.writeTagStart( tagFeatureNote, nv.getName(), xindent++); // ! name is value here
  		  tag= getFieldLabel( lev1, nv);  
			  val= (nv.hasValue()) ? nv.getValue().trim() : "";
				if (tag!=null && val.length()>0) 	{
          lucindexer.addIndexField( tag, val);  
					}
				//xpr.writeEndElement( tagFeatureNote, --xindent); 
				break;

		}
	}
		

 	protected String getFieldValue( DocItem di)  
	{ 
		
		switch (di.getKind()) 
		{
			case kFeatureTable: return ""; // return "Key             Location/Qualifiers";
				
			case kName: {
				String val= di.getValue();
				return val;
				}

			case kAccession: {
				String val= di.getValue();
				return val;
				}	  	

			case kTitle: {
				String val= di.getValue();
				return val;
				}

			default:
				return super.getFieldValue(di);  
		}
	}

	protected String getFieldLabel( int level, DocItem di)   
	{
		String name= null;
		indent= 0;
		subindent= 0; // none in embl	
		switch (level) 
		{
			default:
			case kContinue  : 
			case kField     : 
			case kSubfield  :  
				if (fFromForeignFormat) name= getFieldName( di.getKind()); 
				else name= di.getName();
				if ( name==null || name.length()==0 ) return null;
				break; 
								
//			case kFeatField : return di.getName()+" ";    
			case kFeatCont  : 
				name= di.getName();
//				if (di.hasValue()) name += " "; // or "=" ?
				if (name.startsWith("/")) name= name.substring(1).trim();
        if (name.indexOf(' ')>=0) return null; //? error ; or trim
        break;
//				return name;  
//			case kFeatWrap  : return "";  
		}
		name= cleanXmlTag( name);
		return name;
	}

	protected String cleanXmlTag(String tag) {
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

	
	public String getBiodockey(String field) { return (String) elabel2keys.get(field); }
	
	public String getFieldName(int kind) 
	{ 
		//indent= fFieldIndent;
		String lab= null; //getDoclabel( kind);
		String biodockey= getBiodockey(kind);
		if (biodockey!=null) lab= (String) keys2elabel.get( biodockey);
		
		return lab;
	}
	 
	
}
