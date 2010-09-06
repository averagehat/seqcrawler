//iubio/readseq/EmblDoc.java
//split4javac// iubio/readseq/EmblDoc.java date=27-May-2001

// iubio.readseq.EmblDoc.java
// sequence format information (document,features) handlers
// d.g.gilbert, 1997++


package iubio.readseq;


import java.io.*;
import java.util.*;

import iubio.bioseq.*;
import flybase.*;
import Acme.Fmt;



//split4javac// iubio/readseq/EmblDoc.java line=18
public class EmblDoc extends BioseqDocImpl
{
	public static String emblprop= "EmblDoc"; 
	private static FastHashtable elabel2keys = new FastHashtable();  // format label => biodockey
	private static FastProperties	keys2elabel= new FastProperties();  // biodockey => format label
	
	static { 
  	String pname= System.getProperty( emblprop, emblprop);
  	//getDocProperties(pname);
  	getDocProperties(pname,keys2elabel,elabel2keys);
		}

		
	public EmblDoc() { eminit(); }
	
	public EmblDoc(BioseqDoc source) {
		super(source); eminit();
		fFromForeignFormat = !(source instanceof EmblDoc);
		}
		
	public EmblDoc(String idname) { 
		super(); eminit();
		addBasicName( idname);
		}

	protected static String  fFeatureTag= "FT";
	protected int fFeatIndent= 21, fFieldIndent= 5;
	protected int gotOneFT; //?!
	protected boolean isAmino; //! for swissprot version
	public void setAmino(boolean turnon) { isAmino= turnon; }
	
	protected void eminit() {
		kLinewidth= 80;
		}
		
	public void setSourceDoc(BioseqDoc source)
	{
		super.setSourceDoc(source);
		fFromForeignFormat = !(source instanceof EmblDoc);
	}
		
	//public String getID() { return getDocField(kName); } //"ID"
	//public String getTitle() { return getDocField(kDescription); } //"DE"
	
	// may01 - fix so blank lines are permitted (still need proper indent)
	// -- keep newlines as given in comments!, elsewhere ?
		
	public void addDocLine(String line) 
	{ 
		String field, value;
		boolean append= false;
		
		int level= kField;
		int at= line.indexOf(' ');
		int len= line.length();
		
		if (at<0) {
			if (line.startsWith("XX")) return;
			line= line.trim();
			len= line.length();
			field= line;
			value= ""; // null; // may01 - preserve newlines?
				// are all such value-less fields window-dressing or bogus?
			switch (inFeatures) {
				case kBeforeFeatures: 
					if (field.equals(fFeatureTag)) inFeatures= kInFeatures;
					break;
				case kInFeatures: 
					//if (!field.equals(fFeatureTag)) inFeatures= kAfterFeatures;
					if (field.equals(fFeatureTag)) return; // 2nd value-less FT tag is redundant...
					else inFeatures= kAfterFeatures;
					break;
				}

				// may01 - ? treat blank line as continue?
			if (inFeatures != kInFeatures) {
				if (field.equals(lastfld)) {
					level= kContinue;  
					append= true;
					}
				else  
					level= kField;
				}
			else 
				level= kFeatField; 
			}
			
		else {
			field= line.substring(0,at);
			while (at<len && line.charAt(at) == ' ') at++;
							
			switch (inFeatures) {
				case kBeforeFeatures: 
					if (field.equals(fFeatureTag)) inFeatures= kInFeatures;
					break;
				case kInFeatures: 
					if (!field.equals(fFeatureTag)) inFeatures= kAfterFeatures;
					break;
				}

			if (inFeatures != kInFeatures) {
				if (field.equals(lastfld)) {
					level= kContinue;  
					append= true;
					}
				else  
					level= kField;
				}
				
			else {
				if (at<fFeatIndent) {
					level= kFeatField; 
					int e= at;
					len= line.length();
					while (e<len && line.charAt(e) != ' ' && e<fFeatIndent) e++;
					field= line.substring(at, e);
					at= e;
					while (at<len && line.charAt(at) == ' ' && at<fFeatIndent) at++;
					}
				else {
						// ! this could be an append of kFeatField ! -- long locations wrap
					if (lastlev == kFeatField && line.charAt(at) != '/') {
						level= kFeatField; append= true;
						}
					else level= kFeatCont; 
					field= lastfld; //??
					//? append or not - if key starts with / and has = ???
					}
				}

			value= line.substring(at).trim();  //?? always trim?
			}
			
		if (inFeatures==kInFeatures) addFeature( field, value, level, append);
		else addDocField( field, value, level, append);
		if (level != kContinue) { lastfld= field; lastlev= level; }
	}

	protected int addlinefield( String val, int vallen, int at, String scanto, 
														String fldname, int fldkind, int fldlev)
	{
		if (at > vallen) return -1;
		int e= val.indexOf(scanto, at);
		if (e<0) e= vallen;
		String sv= val.substring( at, e).trim();
		if (sv.length()>0) super.addDocField( fldname, sv, fldkind, fldlev, false);
		return e + scanto.length();
	}

//ID   DMEST6A    standard; DNA; INV; 1754 BP.
//ID   BDEJM5357  unannotated; RNA; UNC; 168 BP.
//ID   PPJCG      standard; circular DNA; UNC; 1288 BP.
//ID   FASTA          STANDARD;      PRT;   100 AA.  << swissprot/amino
	protected void addIdline( String field, String val )  // subclass isAmino
	{ 
		int vlen= val.length();
		int at= addlinefield( val, vlen, 0, " ", field, kName, kField);
		if (at>0) at= addlinefield( val, vlen, at, "; ", "dataclass", kDataclass, kField);
		if (at>0) {
			int cir= val.indexOf("circular", at);
			if (cir>0) {
				super.addDocField( "circ", "circular", kSeqcircle, kField, false);
				at= cir + "circular".length();
				}
			}
		if (at>0) at= addlinefield( val, vlen, at, "; ", "mol", kSeqkind, kField);
		if (at>0) at= addlinefield( val, vlen, at, "; ", "div", kDivision, kField);
		if (at>0) at= addlinefield( val, vlen, at, " BP", "length", kSeqlen, kField);
	}

	protected String getIdValue( DocItem di) // subclass isAmino  
	{ 
		StringBuffer sb= new StringBuffer();
		putlinefield( sb, Fmt.fmt( di.getValue(), 9, Fmt.LJ)+" ", "Noname", " ");
		putlinefield( sb, getDocField(kDataclass), "standard", "; ");

		String sv= getDocField( kSeqcircle);
		if (sv!=null) { sb.append( sv); sb.append(' '); }
		putlinefield( sb, getDocField(kSeqkind), "DNA", "; ");
		putlinefield( sb, getDocField(kDivision), "UNC", "; ");
		putlinefield( sb, getDocField(kSeqlen), "0", " BP.");
		return sb.toString();
	}


//SQ   Sequence 1859 BP; 609 A; 314 C; 355 G; 581 T; 0 other;
//SQ   SEQUENCE   100 AA;  11907 MW;  5DB8D5B8 CRC32;    << swissprot/amino
	protected void addSeqstats(String field, String val )  // subclass isAmino
	{ 
		int vlen= val.length();
		super.addDocField( field, "", kSeqstats, kField, false);	// add blank seqstats, build for output				
		int at= val.indexOf("BP;") + 4; // skip past Sequence  462 BP;
		if (at>0) at= addlinefield( val, vlen, at, " A;", "na", kNumA, kSubfield);
		if (at>0) at= addlinefield( val, vlen, at, " C;", "nc", kNumC, kSubfield);
		if (at>0) at= addlinefield( val, vlen, at, " G;", "ng", kNumG, kSubfield);
		if (at>0) at= addlinefield( val, vlen, at, " T;", "nt", kNumT, kSubfield);
		if (at>0) at= addlinefield( val, vlen, at, " other", "nn", kNumN, kSubfield);
	}
	
	protected String getSequenceValue( DocItem di) // subclass isAmino  
	{ 
		StringBuffer sb= new StringBuffer("Sequence ");
		putlinefield( sb, getDocField(kSeqlen), "0", " BP; ");
		putlinefield( sb, getDocField(kNumA), "0",  " A; ");  
		putlinefield( sb, getDocField(kNumC), "0",  " C; ");  
		putlinefield( sb, getDocField(kNumG), "0",  " G; ");  
		putlinefield( sb, getDocField(kNumT), "0",  " T; "); 
		putlinefield( sb, getDocField(kNumN), "0",  " other;");  
		return sb.toString();  
	}


	
	public void addDocField(String field, String val, int level, boolean append) 
	{ 
		int kind= kUnknown;
		if (level == kField || level == kSubfield || level == kContinue) {
			kind= getBiodocKind(field); 
				
			switch (kind) {
			
				case kName: 
					addIdline( field, val);
					return;

			 	case kSeqstats: 
			 		addSeqstats( field, val);
					return;
					
			 	case kReference: {
			 		int a= val.indexOf('[');
			 		int e= val.indexOf(']');
			 		if (a>=0 && e>a) val= val.substring(a+1,e);
			 		break;
					}
				
				case kCrossRef:	
			  case kDate:
					level= kField;  
			  	append= false; // make sep. fields for each embl DT
			  	break;
	
				case kTitle: {
					if (level == kField) level= kSubfield; // fix to match GB subfields
					if (val.startsWith("\"")) val= val.substring(1);
					int vlen= val.length();
					if (val.endsWith("\";")) val= val.substring(0,vlen-2);
					else if (val.endsWith(";")) val= val.substring(0,vlen-1);
					break;
					}
					
				
				case kTaxonomy:
				case kAuthor:
				case kJournal:
				case kRefCrossref:
				case kRefSeqindex:
					if (level == kField) 
					level= kSubfield; // fix to match GB subfields
					break;
					
					// trim some trailing ';'
				case kAccession:
					//int e= val.lastIndexOf(';');
					//if (e>0) val= val.substring(0,e);
					val= val.replace(';',' ').trim();
					break;		  	
			 	}
	 	 
 			}
		super.addDocField( field, val, kind, level, append);
	}
	

	int lastkind;
	
	protected void writeDocItem( DocItem nv, boolean writeAll) 
	{
		int kind= nv.getKind();
		switch (kind) {
			case kSeqkind: 
			case kSeqlen: 
			case kDivision: 
			case kDataclass:
			case kSeqcircle:
			case kStrand:
			case kNumA: 
			case kNumC: 
			case kNumG: 
			case kNumT: 
			case kNumN: 
			case kBioseqSet:
			case kBioseq:
			case kBioseqDoc:
				break; // doing elsewhere
				
			case kChecksum: 
				if (!isAmino) super.writeDocItem( 
					new DocItem( getFieldName(kComment), nv.getValue()+" CRC32;", kComment, kField), writeAll); 
				break; // write as comment for dna, as seqstat for amino !?

			case kSeqstats: //? always write - EMBL must have before seq data
				lastkind= kind; 
				if (!isAmino) pr.println("XX");
				writeKeyValue( nv);
				break;

			default:
					//??? put in some XX's for prettiness/compat w/ embl
				if (!isAmino && kind!=lastkind && kind!=kName && kind!=kSeqdata && nv.getLevel() == kField)  
					pr.println("XX");
				lastkind= kind; 
				super.writeDocItem( nv, writeAll);
				break;
		}
	}
		
	protected void putlinefield( StringBuffer sb, String sv, String defval, String tail)
	{
		sb.append( (sv==null ? defval : sv));  
		sb.append(tail);
	}
	
	
	protected String getFieldValue( DocItem di)  
	{ 
		switch (di.getKind()) 
		{
			case kFeatureTable:
				if (isAmino) return ""; else
				return "Key             Location/Qualifiers";
				
			case kName:  
				return getIdValue(di);

			case kSeqstats:
				return getSequenceValue(di);

			case kReference:
				String rn= di.getValue();
 				return "["+rn+"]";
 				
				// add back ';'
			case kAccession: {
				String val= di.getValue();
				if (val.indexOf(' ')>0) {
					StringBuffer sb= new StringBuffer();
					StringTokenizer st= new StringTokenizer(val, " ");
					while (st.hasMoreTokens()) { sb.append( st.nextToken()); sb.append("; "); }
					val= sb.toString();
					}
				return val + ";"; // expect val is trimmed always
				}	  	

			case kTitle: {
				String val= di.getValue();
				if (val.length()>1 && !val.endsWith("\";")) val= "\"" + val + "\";";
				return val;
				}

			default:
				return super.getFieldValue(di);  
		}
	}


	protected String getFieldLabel( int level, DocItem di) //int level, int kind, String name, boolean hasval
	{
		String name= null;
		indent= 0;
		subindent= 0; // none in embl	
		switch (level)  {
			default:
			case kContinue  : 
			case kField     : 
			case kSubfield  :  
				if (fFromForeignFormat) name= getFieldName( di.getKind()); else name= di.getName();
				if ( name==null || name.length()==0 ) return null;
				//if ( di.getKind() == kSeqstats) return null; //??
				indent= fFieldIndent; 
				return Fmt.fmt( name, indent-1, Fmt.LJ)+" ";
								
			case kFeatField : 
				indent= fFeatIndent;
				return "FT" +  spaces( fFieldIndent - 2)  
				      + Fmt.fmt( di.getName(), indent - fFieldIndent-1, Fmt.LJ)+" "; 
				
			case kFeatCont  : 
				name= di.getName();
				if (!name.startsWith("/")) name= "/"+name;
				if (di.hasValue()) name += "="; 
				subindent= name.length();
				indent= fFeatIndent;
				return "FT" +  spaces( indent - 2)  + name;

			case kFeatWrap  : 
				indent= fFeatIndent;
				return Fmt.fmt( "FT", indent, Fmt.LJ);
		}
	}

	public String getBiodockey(String field) { return (String) elabel2keys.get(field); }
	
	public String getFieldName(int kind) 
	{ 
		indent= fFieldIndent;
		String lab= null; //getDoclabel( kind);
		String biodockey= getBiodockey(kind);
		if (biodockey!=null) lab= (String) keys2elabel.get( biodockey);
		switch (kind) {
			case kFeatureTable: 
				//Debug.println("Embldoc.getFieldName kFeatureTable="+gotOneFT);
				//if (featWrit && gotOneFT>0) return null; 
					// this is bad for writeAllText call to get name to write !
						// do in getFieldLabel instead
				gotOneFT++;
				indent= fFeatIndent;
				break;
			case kFeatureItem: 	 
			case kFeatureNote:  indent= fFeatIndent; break;  
			}
		return lab;
	}
	
};




