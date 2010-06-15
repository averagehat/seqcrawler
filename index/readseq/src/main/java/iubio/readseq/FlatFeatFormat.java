// iubio.readseq.FlatFeatFormat.java
// gnomap sequence features format i/o
// d.g.gilbert, 2001

package iubio.readseq;

import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;

import flybase.OpenString;
		 
	// interfaces
import iubio.readseq.BioseqReaderIface;
import iubio.readseq.BioseqWriterIface;
import iubio.readseq.BioseqDoc;

import iubio.readseq.BioseqFormat;
import iubio.readseq.BioseqWriter;

import iubio.bioseq.SeqRange;

import flybase.FastVector;
import flybase.FastHashtable;
import flybase.FastProperties;
import flybase.Utils;
import flybase.Debug;


// FlatFeatFormat: see http://iubio.bio.indiana.edu/eugenes/ for gnomap features and dna
// see flybase.map.CsomeReader


public class FlatFeatFormat extends BioseqFormat
{
	public String formatName() { return "FlatFeat|FFF"; }  //Flat Feature Format?
	public String formatSuffix() { return ".fff"; } 
	public String contentType() { return "biosequence/fff"; } // was gnomap, change to FFF format
	public boolean canread() { return true; } // ?? handle two files: features.tsv & seq.fasta for inputs
	public boolean canwrite() { return true; }
	public boolean hasdoc() { return true; }
	public boolean hasseq() { return false; } // new may01 for FlatFeat feature files

	public BioseqReaderIface newReader() { return new FlatFeatReader(); }
	public BioseqWriterIface newWriter() { return new FlatFeatWriter(); }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		// is it "# gnomap-" "##gnomap-"  or what?
		int at= line.indexOf("flatfeat-version");
		//if (at<0) at= line.indexOf("fff-version");
		if (at<0) at= line.indexOf("gnomap-version");
		if (at>0 && at<3) {
      formatLikelihood= 95;
      if (recordStartline==0) recordStartline= atline;
    	return false;
      }
    else
    	return false;
	}
}

//public 
class FlatFeatReader  extends BioseqReader
{
	protected FlatFeatDoc doc;
	
	public FlatFeatReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		}

	public boolean endOfSequence() {
	  return (true);
		}

		// read doc from .tsv, seq from .fasta/.fa ???
    // NO seq in .tsv, only doc.  Use some merge doc/seq method to combine doc w/ seq from other file
	 
	protected void read() throws IOException
	{  
		doc= new FlatFeatDoc();
		if (skipdocs) doc.setSkipDocs(skipdocs);
		seqdoc= doc;
	  while (!allDone) {
			boolean adddoc = ((atseq+1) == choice); //!  skip doc if wanted; readLoop() increments atseq
	    //if (nWaiting > 12) seqid= sWaiting.substring(12).toString();
	  	if (adddoc) doc.addDocLine( sWaiting.toString());
	    while (! endOfFile() ) {
	  		getline(); if (adddoc) doc.addDocLine(sWaiting);
	   		}
	    // readLoop(); // no seq to read
			// if (!allDone) { while (!(endOfFile() || (nWaiting > 0 )))  getline();  }
	    // if (endOfFile()) 
	    allDone = true;
	    seqid= doc.getID();
	  }
	} 


};



//public 
class FlatFeatWriter  extends BioseqWriter  
{
	//final static int seqwidth= 60, ktab= 0, kspacer= 10, knumwidth= 9, knumflags= 0;  	
		
	//protected final void writeTitle() { writeString("DEFINITION  "); writeln( seqid); }

	public void writeHeader()  throws IOException { 
		super.writeHeader();
		//? should do this in FlatFeatDoc, but don't want it for each record !
		writeln( FlatFeatDoc.getMagicString() );  //+ gnomapdoc.sversion ;  version 1 -- fixed columns:
		writeln( FlatFeatDoc.getColumnHeader() ); 
		}

	public void writeSeq() { }
	public void writeRecordEnd() { }  
	public void writeSeqEnd() { }  
		
	public void writeDoc()
	{
		//String cks= checksumString();
		if (seqdoc instanceof BioseqDoc) {
			FlatFeatDoc doc= new FlatFeatDoc((BioseqDoc)seqdoc); 
			boolean doid= true;
			//String docid= doc.getID();
			//if (docid==null || !docid.equals(idword)) { writeID(); doid= false; }
			//if (!doid && doc.getTitle()==null) writeTitle(); // should have, but not before id
			linesout += doc.writeTo(douts, doid);
			}
			
		//call super.writeDoc() to do fasta >seqname line 

 	}
 	
};


	// gnomap  version 1 -- fixed columns:
	//# Feature       gene    map     range   id      db_xref         notes

//public 
class FlatFeatDoc extends BioseqDocImpl
{
	public final static String gnomapprop= "FlatFeatDoc"; 
	public final static String gnomapvers  = "gnomap-version "; // should be # gnomap-
	public final static String flatfeatvers= "flatfeat-version "; // 
	public final static String featurecmt= "Features for ";  // should be # Features
	
	final static String featheadline1 = "# Feature\tgene\tmap\trange\tid\tdb_xref\tnotes";
	final static String featheadline2 = "Key\tLocation\tQualifiers";
	
	public String species= "", chromosome= "";
	public String datadate= "", datasource= "";
	public static String attribOutSeparator= " ; ";
	private static FastHashtable elabel2keys = new FastHashtable();  // format label => biodockey
	private static FastProperties	keys2elabel= new FastProperties();  // biodockey => format label

	public static int  kWriteVersion = 2; // output only?
	public String sversion;
	public int dataversion= 1;
	int dataline;
	int origin= SeqRange.kZero; 
	int firstSource;
	
	
	static { 
  	String pname= System.getProperty( gnomapprop, gnomapprop);
  	getDocProperties(pname,keys2elabel,elabel2keys);
		}


	public final static String getMagicString()	{ return getMagicString(kWriteVersion); }
	public static String getMagicString(int version)	{
		return "# "+flatfeatvers+String.valueOf(version); //? ##gnomap?
		}
		
	public final static String getColumnHeader()	{ return getColumnHeader(kWriteVersion); }
	public static String getColumnHeader(int version)	{
		switch (version) {
			default: 
			case 2: return featheadline2;
			case 1: return featheadline1;
			}
		}
		
	public FlatFeatDoc() { }
	
	public FlatFeatDoc(BioseqDoc source) {
		super(source);
		fFromForeignFormat = !(source instanceof FlatFeatDoc);
		}
		
	public FlatFeatDoc(String idname) { 
		super();
		addBasicName( idname);
		}

	public void setSourceDoc(BioseqDoc source)
	{
		super.setSourceDoc(source);
		fFromForeignFormat = !(source instanceof FlatFeatDoc);
	}
		
	//public String getID() { return getDocField(kName); } //"ID"
	//public String getTitle() { return getDocField(kDescription); } //"DE"
		
	public void addDocLine(String line) 
	{ 
	 	FeatureItem fi= processLine( line);
 			
		if (fi!=null) {
			inFeatures= kInFeatures;
			addFeature( fi);  
 			lastfld= fi.getName(); lastlev= kFeatField;

			if (firstSource==1 && (species.length()>0 || chromosome.length()>0) ) {
				String recid= species + "_" + chromosome;
				replaceDocField( kName, recid);
				firstSource++;
				}
 			}
	}


	//========== from flybase.map.CsomeReader =========
	
		// field columns
	protected final static int 
		kClassFld= 0, kNameFld= 1, kMapFld= 2, kRangeFld= 3, 
		kIdFld= 4, kDbxFld= 5, kNotesFld= 6, kNvals=  7;

	protected String[] vals= new String[kNvals];
	protected int[] valcols; // version 2
		
	// version 1 -- fixed columns:
	//# Feature       gene    map     range   id      db_xref         notes

  protected void getColumnVals(String line) // version 1
  { 
		int at0= 0;
		for (int i=0; i<kNvals; i++) {
			if (at0<0) vals[i]= null;
			else {
				int at= line.indexOf('\t', at0);
				if (at<0) { vals[i]= line.substring(at0); at0= -1; }
				else { vals[i]= line.substring(at0,at); at0= at+1; }
				}
			}
	}
	
	// version 2 -- flexible columns, 1st non-comment == column names
	
	protected int getFieldColumn2(String fldname) 
	{
		if (fldname==null) return -1;
		//Debug.println("getFieldColumn2=" + fldname);
		fldname= fldname.toLowerCase();
		//Key  Location   Qualifiers are only v2 columns ?
		
		if ("feature".equals(fldname)) return kClassFld;
		else if ("key".equals(fldname)) return kClassFld;

		else if ("location".equals(fldname)) return kRangeFld;
		else if ("range".equals(fldname)) return kRangeFld;

		else if ("qualifiers".equals(fldname)) return kNotesFld;
		else if ("notes".equals(fldname)) return kNotesFld;
		else if ("attributes".equals(fldname)) return kNotesFld;
		
		else if ("gene".equals(fldname)) return kNameFld;
		else if ("symbol".equals(fldname)) return kNameFld;
		else if ("name".equals(fldname)) return kNameFld;
		
		else if ("map".equals(fldname)) return kMapFld;
		
		else if ("id".equals(fldname)) return kIdFld;
		else if ("db_xref".equals(fldname)) return kDbxFld;
		
		else return -1;
	}

	
  protected void getColumnKeys2(String line) // version 2
  { 
		String key; 
		FastVector cols= new FastVector();
		for (int at0= 0, icol= 0; at0>=0; icol++) {
			int at= line.indexOf('\t', at0);
			if (at<0) { key= line.substring(at0); at0= -1; }
			else { key= line.substring(at0,at); at0= at+1; }
			int tocol= getFieldColumn2( key);	 
			//Debug.println("getColumnKeys2=" + tocol);
			cols.addElement( new Integer(tocol));
			}
		valcols= new int[ cols.size()];
		for (int i=0; i<cols.size(); i++)
			valcols[i]= ((Integer) cols.elementAt(i)).intValue();
	}

  protected void getColumnVals2(String line) // version 2
  { 
		String val; 
		for (int i= 0; i<kNvals; i++) vals[i]= null;
		for (int at0= 0, icol= 0; at0>=0 && icol < valcols.length; icol++) {
			int at= line.indexOf('\t', at0);
			if (at<0) { val= line.substring(at0); at0= -1; }
			else { val= line.substring(at0,at); at0= at+1; }
			int valcol= valcols[icol];
			if (valcol>=0) vals[valcol]= val;
			}
	}
		

  protected void processComment(String line) 
  { 
			// # Features for yeast from NCBI Genomes [/S_cerevisiae//Chr10/X.gbk.Z, 26-March-2000]
			// # Features for worm from WormBase GFF data [csome_X.gff, 26-May-2000]";
			// # gnomap-version 1
		String key= flatfeatvers;
		int at= line.indexOf(key);
		if (at<0) { key= gnomapvers; at= line.indexOf(key); }
		if (at>=0) {
			try {
				//int at= line.indexOf(gnomapvers);
				sversion= line.substring(at+key.length()).trim();
				Debug.println("flatfeat vers=" + sversion);
				int vers= Integer.parseInt(sversion);
				if (vers>0) dataversion= vers;
				}
			catch (Exception e) {}
			return;
			}

		if (line.indexOf(featurecmt)>=0) {
			String dtsource= "", dtdate= "";
		  at= line.indexOf(featurecmt);
			line= line.substring(at+featurecmt.length());
			int e= line.indexOf(" from");
			if (e>0) {
				species= line.substring(0,e).trim();
				line= line.substring(e+" from".length());
				e= line.indexOf("["/*]*/); 
				if (e>0) {
					dtsource= line.substring(0,e).trim();
					line= line.substring(e);  
 					at= line.indexOf(",");
 					if (at>=0) e= line.indexOf(/*[*/"]",at);
					if (at>=0 && e>at) {
						dtdate= line.substring(at+1,e).trim();
						}
					}
				}
			
			if (dtsource.length()>0 || dtdate.length()>0) {
				if (dtsource.length()>0) datasource= dtsource;
				if (dtdate.length()>0) datadate= dtdate;
				String dtinfo= "Data source: " + datasource;
				if (datadate.length()>0) dtinfo += ", " + datadate;
 				// Environ.gEnv.set("MAP_DATAINFO", dtinfo);
		  	}
			}
			
	}

	protected boolean processNondataLine(String line) 
	{
 		if (line.length()==0) return true;
 		else if ( line.startsWith("#") ) 	{
 			processComment(line);
			return true;
			}	
 		else if (dataversion >= 2 && dataline==0) {
			getColumnKeys2(line); dataline++;
			return true;
			}
		else return false;
	}
	

  protected FeatureItem processLine(String line)  
	{		
		line= line.trim(); //??? leave leading spc?
		if (processNondataLine(line)) return null;
 		else {
	 		String itemId= null;
			FeatureItem item= new FeatureItem(); //( "", "", BioseqDoc.kFeatField);
  		SeqRange seqrange= new SeqRange(); 
  		String key= null;
			boolean issource= false;

			if (dataversion >= 2) {
				if (dataline==0) getColumnKeys2(line);
				else getColumnVals2(line);
				dataline++;
				}
			else
				getColumnVals(line);
			
			for (int i=0; i<kNvals; i++) {
				String v= vals[i];
				if (i>0 && (v==null||"-".equals(v)||v.length()==0)) continue;
    		switch (i) {
    		case kClassFld: 
					if (v==null||"-".equals(v)||v.length()==0) return null;
    			key= v;
    			if ("source".equals(key)) firstSource++;
    			break;
    		case kNameFld: 
 					item.putNote( new FeatureNote("name",v)); 
  				if ("source".equals(key)) species= v; //?
    			break;
    		case kMapFld: 
 					item.putNote( new FeatureNote("map",v));  
  				if ("source".equals(key)) {
  					int ca= v.indexOf("Chr "); if (ca>=0) v= v.substring(ca+4);
  					chromosome= v; 
  					issource= true;
  					}
    			break;
    		case kRangeFld: 
  				try { 
  					//seqrange= SeqRange.parse(v); //? slow to be called so often?
  					//seqrange= new SeqRange(); 
  					int oldorig= SeqRange.kZero;
   					if (!issource) oldorig= seqrange.setDisplayOrigin(origin); // affects parse also
 						seqrange.parse1(v);
   					if (!issource) seqrange.setDisplayOrigin(oldorig); // reset to standard after parse!
  					} 
  				catch (Exception e) {}
    			break;
    		case kIdFld:  
  				item.putNote( new FeatureNote("ID",v));  
    			itemId= v;
    			break;
    		case kDbxFld: // dbx field: fbgnid,fbanid,dbxid,...
    			getDbxRefs( item, v, key);
    			break;
    		case kNotesFld:
    			getQualifiers( item, v, key);
    			break;
    			
    		}
			}
			
			if (key==null) return null;
			item.set( key, seqrange);  // null seqrange is okay, key must not be
			//Debug.println("F FeatItem="+item);
 			return item;
 			}
	}
	
	protected void getQualifiers( FeatureItem item, String s, String featkey)
	{	
			// format is "/key='value here'  where / may be missing, = may be space?, ' maybe " or missing
			// if "" or '', then need to skip to trailing quote
		//Debug.println("getQualifiers " +featkey+"="+ s);
		for (int at0= 0; at0>=0; ) {
			String key, val= "";
			int len= s.length();
			int at= s.indexOf('=', at0);
		  if (at<0) at= s.indexOf(' ', at0);
			if (at<0) { key= s.substring(at0); at0= -1; }
			else { key= s.substring(at0,at); at0= at+1; }
	  	key= key.trim();
			//!? if (key.startsWith("/")) key= key.substring(1);
			
			if (at0>0) {
				int e= -1; at= -1;
				for (int i= at0; i>0 && i<len && at<0; i++) {
					char c= s.charAt(i);
					if (c == '"') i= s.indexOf( c, i+1);
					else if (c == '\'')  i= s.indexOf( c, i+1);
					else if (c == ';' || c == ',' || c == ' ') {
						at= i;
						for ( ; i<len && e<0; i++) {
							c= s.charAt(i);
							if (c == '/' || Character.isLetterOrDigit(c)) e= i;
							}
						if (e<0) e= len;
						}
					}
				if (at>0) { val= s.substring(at0, at); at0= e; } 
				else { val= s.substring(at0); at0= -1; }		
		  	val= val.trim();
		  	}
		  	
	  	if (key.length()>0) {
	  		String akey= key;
				if (!key.startsWith("/")) key= "/"+key;
				FeatureNote fnote= new FeatureNote(key,val);
  			item.putNote( fnote); // standard is that note keys begin with '/'
				//Debug.println("FeatureNote " +key+"="+ val);
	  		
	  		/*  
	  		if ("source".equals(featkey)) {
	  			//akey= akey.toUpperCase();
					if (akey.startsWith("/")) akey= akey.substring(1);
						//? do we want this - leads to dup for fff->embl,gb - leave in feature notes only?
						// also strip "" from val
	  			val= getTrimFieldValue( fnote);
	  			String idtag= getFieldName( kName);
	  			String actag= getFieldName( kAccession);
	  			if (akey.equals(idtag)) 
	  				addDocField( idtag, val, kField, false);
	  			else if (akey.equals(actag)) 
	  				addDocField( actag, val, kField, false);
	  			}
	  		*/
	  		}
			}
	}
	
	protected void getDbxRefs( FeatureItem item, String s, String key)
	{	
		for (int at0= 0; at0>=0; ) {
			String val;
			int at= s.indexOf(',', at0);
			//if (at<0) at= s.indexOf(';', at0);
			if (at<0) {  val= s.substring(at0); at0= -1; }
			else { val= s.substring(at0,at); at0= at+1; }
	  	val= val.trim();
	  	if (val.length()>0) item.putNote( new FeatureNote("db_xref",val));  
			}
	}

	//========================
	
	public void addDocField(String field, String val, int level, boolean append) 
	{ 
		int kind= kUnknown;
		if (level == kField || level == kSubfield || level == kContinue) {
			kind= getBiodocKind(field); 
			switch (kind) {
				//case kName:
			 	//case kSeqstats:
				//	return;
				
				case kCrossRef:	
			  case kDate:
					level= kField;  
			  	append= false; // make sep. fields for each embl DT
			  	break;
	
				case kTitle: 
			 	case kReference: 
				case kTaxonomy:
				case kAuthor:
				case kJournal:
				case kRefCrossref:
				case kRefSeqindex:
				case kAccession:
					break;		  	
			 	}
	 	 
 			}
		super.addDocField( field, val, kind, level, append);
	}
	
	protected String keyvalsep;
	protected int keyvalat;
	protected DocItem accItem, idItem;
	int lastkind, partid;
	//String seqname;
	boolean didtop;
	
	protected boolean writeKeyValue( DocItem di ) // override
	{
		if (lastkind==kFeatureTable) return true;
		String lab= getFieldLabel( di); // lev1, kind, name, di.hasValue() 
		String val= getFieldValue( di); //val, lev1, kind, name
		
		if (lab!=null) { 
			//if (di.getKind()==kFeatureNote && lab.startsWith("/")) lab= lab.substring(1);
			// quick hack for hypertext links at this level
			// -- PrintableDocItem is bad news for formatting w/ gnomap outputs now...
			//if (di instanceof PrintableDocItem) 
			//	((PrintableDocItem)di).print( pr, this, lab, val);
			//else 
				{
				if (keyvalat>0 && keyvalsep!=null) pr.print(keyvalsep);
				keyvalat++;
				pr.print( lab );
				val= val.replace('\n',' '); // make sure no newlines
				pr.print( val);
				}
			return true;
			}
		else return false;
	}


	protected void writeFeatureVers1(FeatureItem	fi) 
	{
	// version 1 -- fixed columns:
	//# Feature       gene    map     range   id      db_xref         notes
			String featname= fi.getName();
			String id= "-"; // getID(); -- no, feature id from fi.notes
			String symbol= "-";
			String gmap= "-";
			
			FastVector notes= fi.notes;
			if (notes != null) {
				notes= (FastVector)notes.clone(); // so can remove 
				for (int i= 0; i< notes.size(); i++) {
					DocItem note= (DocItem) notes.elementAt(i);
					String nm= note.getName();
					boolean got= false;
					if ("ID".equals(nm)) { id= note.getValue(); got= true; }
					else if ("name".equals(nm)) { symbol= note.getValue(); got= true; }
					else if ("map".equals(nm)) { gmap= note.getValue();		 got= true; }				
					if (got) notes.removeElementAt(i);
					}
				}
			
			pr.print( featname ); 
			pr.print("\t");
			pr.print( symbol );  // gene symbol/label
			pr.print("\t");
			pr.print( gmap);   // ? from feature notes?
			pr.print("\t");
			pr.print( fi.getLocation() );   
			pr.print("\t");
			pr.print( id );   
			
			pr.print("\t");
			if (notes != null) { 
				for (int i= 0, k=0; i< notes.size(); i++) {
					DocItem note= (DocItem) notes.elementAt(i);
					String nm= note.getName();
					if ("db_xref".equals(nm)) {
						if (k>0) pr.print(",");
						pr.print( note.getValue() );  k++;
						notes.removeElementAt(i);
						}
					}
				}
	  	
			pr.print("\t"); 
			if (notes != null) { 
				keyvalat= 0; keyvalsep= attribOutSeparator; // " ; " for notes, " , " for dbxref
				writeDocVector(notes, false);
				keyvalsep= null; 
				}
					 
			pr.println(); linesout++;
	}	
		
	protected void writeFeatureVers2(FeatureItem	fi) 
	{
	// version 2 -- flattened Feature Table Format (FFF)
	//Key		Location		Qualifiers
			String featname= fi.getName();
			FastVector notes= fi.notes;
			pr.print( featname ); 
			pr.print("\t");
			pr.print( fi.getLocation() );   
			pr.print("\t");
			if (notes != null) { 
				keyvalat= 0; keyvalsep= attribOutSeparator; // " ; " for notes, " , " for dbxref
				writeDocVector( notes, false);
				keyvalsep= null; 
				}
			pr.println(); linesout++;
	}	

	
	protected void writeDocItem( DocItem nv, boolean writeAll) 
	{
		int kind= nv.getKind();
		lastkind= kind; 
		switch (kind) 
		{
			//case kTitle:  
			//case kDate:  
			  // instead of writing as #comment, stuff into features(source).notes ??
			case kAccession: 
				accItem= nv; 
				break;
			case kName:	
				idItem= nv; 
				//if (writeKeyValue(nv)) { pr.println(); linesout++; }
				break;
				
			case kFeatureTable: // writeDocVector( features(), writeAll); 
				super.writeDocItem( nv, writeAll);
				break;
			case kFeatureNote:
				super.writeDocItem( nv, writeAll);
				break;
			
			case kFeatureItem:
				if ((nv instanceof FeatureItem) && wantFeature(nv)) {
				FeatureItem	fi= (FeatureItem) nv; 
					// what if source has id/acc notes?

 				if ("source".equals(fi.getName())) {
					//if (findDocItem( fi.notes, "/AC", 0)!=null) accItem= null;
					if (fi.getNote("/AC")!=null) accItem= null;
					if (accItem!=null) fi.putNote( new FeatureNote(accItem)); accItem= null; 
					
					//if (findDocItem( fi.notes, "/ID", 0)!=null) idItem= null;
					if (fi.getNote("/ID")!=null) idItem= null;
					if (idItem!=null) fi.putNote( new FeatureNote(idItem)); idItem= null;  
					}
				if (kWriteVersion == 1) writeFeatureVers1( fi);
				else writeFeatureVers2( fi);
				}
				break;

			default:
				break;  
				

		}
	}
		
 	protected String getFieldValue( DocItem di)  
	{ 
		/* // do this in superclass ?
		int lev= di.getLevel();
		if (lev==kFeatCont) {
			String val= di.getValue();
			if (val.length()>1) {
				char c= val.charAt(0);
				if (!(c=='"' || c=='\'' || (c >= '0' && c <= '9') )) {
					val= '"' + val + '"';
					}
				} 
			return val;
			}
		*/
			
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
				//if (val.length()>1 && !val.endsWith("\";")) val= "\"" + val + "\";";
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
		switch (level) 
		{
			default:
			case kContinue  : 
			case kField     : 
			case kSubfield  :  
				if (fFromForeignFormat) name= getFieldName( di.getKind()); else name= di.getName();
				if ( name==null || name.length()==0 ) return null;
				return "#"+name+" ";  
								
			case kFeatField : 
				return di.getName()+" ";   
				
			case kFeatCont  : 
				name= di.getName();
				if (di.hasValue()) name += "="; // or " " ?
				if (!name.startsWith("/")) name= "/"+name;
				//if (name.startsWith("/")) name= name.substring(1);
				return name;  

			case kFeatWrap  : 
				return "";  
		}
	}

	
	public String getBiodockey(String field) { return (String) elabel2keys.get(field); }
	
	public String getFieldName(int kind) 
	{ 
		//indent= fFieldIndent;
		String lab= null; //getDoclabel( kind);
		String biodockey= getBiodockey(kind);
		if (biodockey!=null) lab= (String) keys2elabel.get( biodockey);
		/*switch (kind) {
			case kFeatureTable: 
				if (gotOneFT) return null;
				gotOneFT= true;
				indent= fFeatIndent;
				break;
			case kFeatureItem: 	 
			case kFeatureNote:  indent= fFeatIndent; break;  
			}*/
		return lab;
	}
	 
	
};


