package iubio.readseq;

import iubio.bioseq.SeqRange;
import Acme.Fmt;

public class SwissDoc extends EmblDoc
{

	/*****
	public static String swissprop= "EmblDoc"; 
	private static FastHashtable slabel2keys = new FastHashtable();  // format label => biodockey
	private static FastProperties	keys2slabel= new FastProperties();  // biodockey => format label
	
	static { 
  	String pname= System.getProperty( swissprop, swissprop);
  	getDocProperties(pname,keys2slabel,slabel2keys);
		}

				// this isnt helping - still missing doc data (no AA length for swiss -> embl
	public String getBiodockey(String field) { return (String) slabel2keys.get(field); }
	public String getFieldName(int kind) 
	{ 
		indent= fFieldIndent;
		String lab= null;  
		String biodockey= getBiodockey(kind);
		if (biodockey!=null) lab= (String) keys2slabel.get( biodockey);
		switch (kind) {
			case kFeatureTable:  
 			case kFeatureItem: 	 
			case kFeatureNote:  indent= fFeatIndent; break;  
			}
		return lab;
	}

	****/
	
	public SwissDoc() { super(); swinit(); }
	public SwissDoc(BioseqDoc source) { super(source); swinit(); }
	public SwissDoc(String idname) { super(idname); swinit(); }

	protected void swinit() { 
		fFeatIndent= 14; //? 17? not sure - right-fmt number
		isAmino= true; 
		}


//	protected boolean keepField(int kind) { //? roblems here
//		return true; // (keepFields==null || keepFields.get(new Integer(kind))!=null);
//		}	//^^^^^^^ THIS IS THE DANG PROBLEM - why is setSkipDocs doing this to SwissDoc?



// FIXME for diff swiss feature table
//FT   DOMAIN        1     36       EXTRACELLULAR (POTENTIAL).
//FT   TRANSMEM    377    398       7 (POTENTIAL).
//FT   DOMAIN      399    468       CYTOPLASMIC (POTENTIAL).
//FT   CARBOHYD     78     78       POTENTIAL.
//123456789012345678901234567890123456789
//0        1         2          3 

	public void addFeature(String field, String value, int level, boolean append) 
	{ 
		if (lastlev == kField) { // put dummy FH entry
			addDocField( kFeatureTable, "");
			//Debug.println("addFeature - add feature header ");		
			}
		// fixme - convert  "1  36" into SeqRange(start,stop)
		String start= "", stop= "";
		int sp= value.indexOf(' '); // value is trim
		if (sp>0) {
			start= value.substring(0,sp);
			value= value.substring(sp+1).trim();
			sp= value.indexOf(' ');  
			if (sp>0) {
				stop= value.substring(0,sp);
				value= value.substring(sp+1).trim();
				}
			else { stop= value; value= ""; }
			start= start + ".." + stop;
			}
		super.addFeature( field, start, level, false); // DOMAIN 1..36
		addFeatureNote("note",value); //  note=EXTRACELLULAR (POTENTIAL).
	}

 //ID   FASTA          STANDARD;      PRT;   100 AA.  << swissprot/amino
	protected void addIdline( String field, String val )  // subclass isAmino
	{ 
		int vlen= val.length();
		int at= addlinefield( val, vlen, 0, " ", field, kName, kField);
		if (at>0) at= addlinefield( val, vlen, at, "; ", "dataclass", kDataclass, kField);
		if (at>0) at= addlinefield( val, vlen, at, "; ", "mol", kSeqkind, kField);
		if (at>0) at= addlinefield( val, vlen, at, " AA", "length", kSeqlen, kField);
	}

	protected String getIdValue( DocItem di) // subclass isAmino  
	{ 
		StringBuffer sb= new StringBuffer();
		putlinefield( sb, Fmt.fmt( di.getValue(), 15, Fmt.LJ), "Noname", "");
		putlinefield( sb, 8, getDocField(kDataclass), "STANDARD", "; ");
		putlinefield( sb, 8, getDocField(kSeqkind), "      PRT", "; ");
		putlinefield( sb, 5, getDocField(kSeqlen), "   0", " AA.");
		return sb.toString();
	}

//SQ   SEQUENCE   100 AA;  11907 MW;  5DB8D5B8 CRC32;    << swissprot/amino
	protected void addSeqstats(String field, String val )  // subclass isAmino
	{ 
		int vlen= val.length();
		super.addDocField( field, "", kSeqstats, kField, false);	// add blank seqstats, build for output				
		int at= val.indexOf("AA;") + 4; // skip past Sequence  462 AA;
		if (at>0) at= addlinefield( val, vlen, at, " MW;", "mw", kNumN, kSubfield);
		if (at>0) at= addlinefield( val, vlen, at, " CRC", "crc", kChecksum, kSubfield);
	}
	
	protected String getSequenceValue( DocItem di) // subclass isAmino  
	{ 
		StringBuffer sb= new StringBuffer("SEQUENCE ");
		putlinefield( sb, 5, getDocField(kSeqlen), "0", " AA; ");
		putlinefield( sb, 6, getDocField(kNumN), "", ""); // "0",  " MW; "
		putlinefield( sb, 9, getDocField(kChecksum), "", ""); //, "0",  " CRC32; " // CRC64; also used !
		return sb.toString();  
	}

	protected void putlinefield( StringBuffer sb, int wd, String sv, String defval, String tail)
	{
		sb.append( Fmt.fmt( (sv==null ? defval : sv), wd, 0) );  
		sb.append(tail);
	}

	protected void writeDocItem( DocItem nv, boolean writeAll) 
	{
		switch (nv.getKind())  {
			default:
				super.writeDocItem( nv, writeAll);
				break;
			
			case kFeatureTable:   /// need better way to drop FH for swiss
				if (!featWrit) {
					if (writeAll) { //writeKeyValue( nv) && 
						//Debug.println("writeDocItem - writeDocVector "+features().size());
						writeDocVector( features(), writeAll); 
						writeExtractionFeature();
						featWrit= true;
						}
					}
				break;
			}
	}

	protected String getFieldLabel( int level, DocItem di)
	{
		String name= null;
		indent= 0;
		subindent= 0; // none in embl	
		switch (level)  {
			default:
				return super.getFieldLabel(level, di);
								
			case kFeatCont  : 
				return null; // null?
				//a name= di.getName();
				//a if (!name.startsWith("/")) name= "/"+name;
				//a if (di.hasValue()) name += "="; 
				//a ? subindent= name.length();
				//a indent= fFeatIndent;
				//a return "FT" +  spaces( indent - 2)  + name;
		}
	}

	protected String getFieldValue( DocItem di) 
	{ 
		if (di instanceof FeatureItem) {
			SeqRange sr= ((FeatureItem) di).getLocation();
			String val= Fmt.fmt( sr.start()+1, 6, 0) + " " + Fmt.fmt( sr.stop()+1, 6, 0);
			String note= ((FeatureItem) di).getNoteValue("note");
			note=  getTrimFieldValue(note); // need to chop "" quotes?
			if (note!=null) val += Fmt.fmt( " ", 7) + note;
			return val;
			//return ((FeatureItem) di).locationString();  
			}
			
		if (di.getLevel()==kFeatCont) // == kFeatureNote
			return  getTrimFieldValue(di);  
		else
			return super.getFieldValue(di);
	}
	
}
