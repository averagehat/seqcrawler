//iubio/readseq/BioseqDocImpl.java
//split4javac// iubio/readseq/BioseqDocImpl.java date=07-Jun-2001

// iubio.readseq.BioseqDocImpl.java
// sequence format information (document,features) handlers
// d.g.gilbert, 1997++


package iubio.readseq;


import java.io.*;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;

import iubio.bioseq.*;
import flybase.*;
import Acme.Fmt;



/**
  * sequence format information (document,features)
  * common document fields among formats
  *	@see iubio.drawseq.DrawableBioseqDoc for display of features
	*/
	
//split4javac// iubio/readseq/BioseqDocImpl.java line=26
public abstract class BioseqDocImpl
	implements BioseqDoc, Cloneable //!?
{
	public static String bioseqprop= "BioseqDoc"; 
	public static String gbfeatname= "Features";

  // should be read from .props --
	public final static String sExtractionFeature = "extracted_range";
	public final static String sExtractRangeFeature = "extract_range";
  public static String sExtractionNote = 
"Range of sequence extracted from original.  Feature locations are for original, not for this sequence.";
			  				
	protected static FastHashtable  biodockeys = new FastHashtable();  // biodockey => intval
	protected static FastHashtable	biodockinds= new FastHashtable();  // intval => biodockey
	protected static FastHashtable	biodoclabels= new FastHashtable();  // biodockey => label
	
	static { 
  	//String pname= System.getProperty( propname, propname);
		//getBioseqdocProperties(pname);  //! better not allow changes - see below
		getBioseqdocProperties();
		}
		
		//
		// ! keep this in sync with above BioseqDoc values
		//
	private static void getBioseqdocProperties()
	{ 
		putdocval("kUnknown",kUnknown,"Other field");
		putdocval("kBioseqSet",kBioseqSet,"Biosequence collection");
		putdocval("kBioseq",kBioseq,"Biosequence record");
		putdocval("kBioseqDoc",kBioseqDoc,"Documentation");
		putdocval("kName",kName,"Locus name");
		putdocval("kDivision",kDivision,"Databank division");
		putdocval("kDataclass",kDataclass,"Data class");
		
		putdocval("kDescription",kDescription,"Description");
		putdocval("kAccession",kAccession,"Accession");
		putdocval("kNid",kNid,"NID (part id)");
		putdocval("kVersion",kVersion,"Version");
		putdocval("kKeywords",kKeywords,"Keywords");
		putdocval("kSource",kSource,"Organism source");
		putdocval("kTaxonomy",kTaxonomy,"Organism taxonomy");
		putdocval("kReference",kReference,"Reference number");
		putdocval("kAuthor",kAuthor,"Reference author");
		putdocval("kTitle",kTitle,"Reference title");
		putdocval("kJournal",kJournal,"Reference journal");
		putdocval("kRefCrossref",kRefCrossref,"Reference database ID (Medline)");
		putdocval("kRefSeqindex",kRefSeqindex,"Reference sequence index");
		
		putdocval("kFeatureTable",kFeatureTable,"Feature table");
		putdocval("kFeatureItem",kFeatureItem,"Feature item");
		putdocval("kFeatureNote",kFeatureNote,"Feature note");
		putdocval("kFeatureKey",kFeatureKey,"Feature name");
		putdocval("kFeatureValue",kFeatureValue,"Feature value");
		putdocval("kFeatureLocation",kFeatureLocation,"Feature location");
		 
		putdocval("kDate",kDate,"Date");
		putdocval("kCrossRef",kCrossRef,"Database cross reference");
		putdocval("kComment",kComment,"Comment ");
		putdocval("kSeqstats",kSeqstats,"Sequence statistics");
		putdocval("kNumA",kNumA,"No. A bases");
		putdocval("kNumC",kNumC,"No. C bases");
		putdocval("kNumG",kNumG,"No. G bases");
		putdocval("kNumT",kNumT,"No. T bases");
		putdocval("kNumN",kNumN,"No. other bases");

		putdocval("kSeqdata",kSeqdata,"Sequence data");
		putdocval("kSeqlen",kSeqlen,"Sequence length");
		putdocval("kSeqkind",kSeqkind,"Molecule kind");
		putdocval("kChecksum",kChecksum,"Sequence checksum");
		putdocval("kSeqcircle",kSeqcircle,"Circular sequence");
		putdocval("kStrand",kStrand,"Sequence strandedness");
		putdocval("kBlank",kBlank,"blank line");
	}
	
	private static void putdocval(String key, int val, String label)
	{ 
		Integer ival= new Integer(val);
		biodockeys.put(key, ival);
		biodockinds.put(ival, key);
		//? store label somewhere?
		biodoclabels.put(ival,label); //? by key or by ival?
	}
	

	/*private static void getBioseqdocProperties(String propname)
	{ 
		//key=int val|label
		//ID=10|Locus name
		//AC=30|Accession
		biodockeys.loadProperties(propname);
		Enumeration en= biodockeys.propertyNames();
		while (en.hasMoreElements()) {
			String key= (String) en.nextElement();
			String s= biodockeys.getProperty( key);
			String sval;
			int at= s.indexOf('|');
			if (at>0)  sval= s.substring(0,at); else sval= s;
			//? save label - use some ival, label structure ?
			Integer ival;
			try { ival= new Integer(sval); } 
			catch (Exception e) { ival= new Integer(0); }
			biodockeys.put(key, ival);
			biodockinds.put(ival, key);
			}
	}*/


	/*
	protected static FastProperties gbfeatures; //= new FastProperties(); // load only if asked for
	public static Enumeration getStandardFeatureList()
	{
		int count= getStandardFeatureCount(); // just to save init code
		return gbfeatures.propertyNames();
	}
	
	public static int getStandardFeatureCount()
	{
		if (gbfeatures==null) {
			gbfeatures= new FastProperties();
	  	String pname= System.getProperty( gbfeatname, gbfeatname);
			gbfeatures.loadProperties(pname);
			}
		return gbfeatures.size();
	}
	*/
	
	protected static String[] gbfeatures;  
	protected static String[] gbqualifiers;  
	
	public static String[] getStandardQualifiersList()
	{
		if (gbqualifiers==null) getStandardFeatureList();
		return gbqualifiers;
	}

	public static String[] getStandardFeatureList()
	{
		if (gbfeatures==null) 
		try {
				// want these names sorted ! - are sorted in file...so read file not as properties...
	  	String pname= System.getProperty( gbfeatname, gbfeatname);
	  	pname= AppResources.global.findPath(pname + ".properties");
			Debug.println("Feature list: " + pname);
			InputStream ins= AppResources.global.getStream( pname);
			DataInputStream rdr= new DataInputStream( new BufferedInputStream(ins));
			FastVector fv= new FastVector();
			FastVector qv= new FastVector();
			boolean inquals= false;
			String s;
			do { 
				s= rdr.readLine(); 
				if (s!=null) { 
					s= s.trim();
					if (s.indexOf("FEATURES")>0) inquals= false;
					else if (s.indexOf("QUALIFIERS")>0) inquals= true;
					else if (s.length()>0 && !s.startsWith("#")) {
						if (inquals) qv.addElement(s); 
						else fv.addElement(s); 
						Debug.print(s + ", ");
						}
					}
			} while (s!=null);
			gbfeatures  = new String[fv.size()]; fv.copyInto(gbfeatures);
			gbqualifiers= new String[qv.size()]; qv.copyInto(gbqualifiers);
			Debug.println();
			Debug.println(" n = "+fv.size());
			ins.close();
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		return gbfeatures;
	}
	

			//========= data ==============
			
	protected FastVector rootdoc, featlist;  
	//protected FastHashtable roothash; 
	//protected FastHashtable featkinds; // moved to DrawableBioseqDoc
	protected Hashtable wantedFeatures;
	protected SeqRange wantedRange;
	protected boolean fFromForeignFormat, featWrit, dontWriteId, notWantedFeature;
	protected boolean wantExtractionLoc; // for special sExtractRangeFeature only?


	public BioseqDocImpl() {
		rootdoc = new FastVector();  
		featlist= new FastVector(); 
		//roothash= new FastHashtable();
		}

	public BioseqDocImpl( BioseqDoc source) {
		//this(); // don't need new vectors
    setSourceDoc(source);
		}
		
		
		//
		// subclass for each format
		//
		
	public abstract String getFieldName(int kind);
	public abstract String getBiodockey(String field); 
	public abstract void addDocLine(String line); 	
	public void addDocLine(OpenString line) { addDocLine( line.toString()); }

	
	
	public static String getBiodockey(int kind) { 
		return (String) biodockinds.get( new Integer(kind));
		}

	public static String getBiodoclabel(int kind) { 
		return (String) biodoclabels.get( new Integer(kind));
		}

	public Integer getBiodocInteger(String field) { //? drop this one?
		String bdkey= getBiodockey(field); 
		return ((bdkey==null) ? null : (Integer) biodockeys.get( bdkey));
		}

	public int getBiodocKind(String field) {
		String bdkey= getBiodockey(field); 
		if (bdkey==null) return kUnknown;
		else { 
			Integer ikind= (Integer) biodockeys.get( bdkey);
			if (ikind==null) return kUnknown;
			else return ikind.intValue(); 
			}
		}
		

	protected static void getDocProperties(String propname, 
				FastProperties keys2label, FastHashtable label2keys)
	{ 
		keys2label.loadProperties(propname);
		Enumeration en= keys2label.keys(); //propertyNames();
		while (en.hasMoreElements()) {
			String biodockey= (String) en.nextElement();
			String label= keys2label.getProperty(biodockey);
			label2keys.put( label, biodockey);
			}
	}

	public Object clone() {
		try {
			int i;
			BioseqDocImpl c= (BioseqDocImpl) super.clone();
			if (rootdoc!=null) {
				c.rootdoc= (FastVector) rootdoc.clone();
				//c.rootdoc.cloneItems();
		    for ( i= 0; i< rootdoc.size(); i++) {
		    	Object el= c.rootdoc.elementAt(i);
					if (el instanceof DocItem) el= ((DocItem) el).clone();
		    	c.rootdoc.setElementAt(el, i);
					} 
				}

			//if (roothash!=null) {
				//? clone it -- are we using this clone()?
				// if so, use rootdoc elements...
				//}
			if (wantedFeatures != null) {
				//? clone
				}
		
			if (featlist!=null) {
				c.featlist= (FastVector) featlist.clone();
				//c.featlist.cloneItems();
		    for ( i= 0; i< c.featlist.size(); i++) {
		    	Object el= c.featlist.elementAt(i);
					if (el instanceof DocItem) el= ((DocItem) el).clone();
		    	c.featlist.setElementAt(el, i);
					} 
				}
			//c.doc= c.rootdoc;
			//if (featkinds!=null) c.featkinds= (FastHashtable)featkinds.clone();
		 	return c;
			}
		catch(CloneNotSupportedException ex) { throw new Error(ex.toString()); }
		}


	public void clear() {
	  if (rootdoc!=null) rootdoc.removeAllElements(); // clear
	  if (featlist!=null) featlist.removeAllElements();
	  }
	  	
		//
		// data accessor methods
		//

	public String getID() { return getDocField(kName); } // "ID"
	public String getTitle() { return getDocField(kDescription); } 
		
	public boolean hasFeatures() { return (featlist!=null && featlist.size()>0); }
	public boolean hasDocument() { return (rootdoc!=null && rootdoc.size()>0); }

	public FastVector documents() { return rootdoc; }

	public FastVector features()  { 
		//if (updatingfeats && featkinds==null) updateFeatures();
		return featlist; 
		}
		
	public void setSourceDoc(BioseqDoc source)
	{
		if (source==null) {
		rootdoc = new FastVector();  
		featlist= new FastVector(); 
		} else {
		rootdoc= source.documents();
		featlist= source.features();  
		if (source instanceof BioseqDocImpl) copyWanted((BioseqDocImpl)source);
		//roothash= source.roothash;
		fFromForeignFormat = true; // assume yes
		}
	}
	

			// get field by number - only for rootdoc/primary fields
			//
			
	public final String getDocField(int kind) { 
		DocItem di= findDocItem( rootdoc, kind, 0);
		if (di==null) return null; else return di.getValue();
		}

 			
	public final String getDocField( FastVector doc, int kind, int which) { 
		DocItem di= findDocItem( doc, kind, which);
		if (di==null) return null; else return di.getValue();
		}
	
	public void deleteDocItem( int kind ) { 
		DocItem di= findDocItem(rootdoc, kind, 0);
		if (di!=null) rootdoc.removeElement(di);
		}
		
	public void deleteDocItem( FastVector doc, int kind, int which) { 
		DocItem di= findDocItem(doc, kind, which);
		if (di!=null) doc.removeElement(di);
		}

	protected int docItemAt;
	//static int nrepl; // Debug
	
	public final void replaceDocItem( int kind, DocItem newitem ) { 
		if (newitem==null) deleteDocItem( kind);
		else {
			DocItem di= findDocItem(rootdoc, kind, 0);
			if (di!=null && docItemAt>-1) rootdoc.setElementAt(newitem, docItemAt);
			else addDocField(newitem); // sets  curDocitem
				//>? need handler around this: rootdoc.addElement(newitem); 
			//if (nrepl++<50) Debug.println("replaceDocItem " + kind + ", new="+newitem+", old="+di);
			}
		}
		
	public final DocItem findDocItem( int kind, int which) { 
		return findDocItem(rootdoc, kind, which); }

	public DocItem findDocItem( FastVector doc, int kind, int which) 
	{ 
		Object ob= null;
		docItemAt= -1;
		if (doc!=null) 
		for (int i= 0, count= 0; i<doc.size(); i++) 
			try {
			ob= doc.elementAt(i);
			if (ob instanceof DocItem) {
				DocItem anv= (DocItem) ob;
				if (anv.getKind()==kind) { 
					docItemAt= i;
					if (count == which) return anv; //.getValue();
					count++;
					}
				}
			else if (ob instanceof FastVector) {  
				DocItem res= findDocItem((FastVector) ob, kind, which);
				if (res!=null) return res;
				}
			}
			catch (Exception e) {
				System.err.println("failure in getDocField("+kind+") for object " + ob);
				e.printStackTrace();
				}
		return null;
	}


			// get field by name
			//
			
	public final String getDocField( String field) { return getDocField(field,0); }
	public final String getFeature( String field) { return getDocField( features(), field,0);  }

	public final String getDocField(String field, int which) {
		//return getDocField(rootdoc,field,which); 
		DocItem di= findDocItem( rootdoc, field, which);
		if (di==null) return null; else return di.getValue();
		}


	public final String getDocField( FastVector doc, String field, int which) { 
		DocItem di= findDocItem( doc, field, which);
		if (di==null) return null; else return di.getValue();
		}

	public DocItem findDocItem( FastVector doc, String field, int which) 
	{ 
		if (doc!=null) for (int i= 0, count= 0; i<doc.size(); i++) {
			Object ob= doc.elementAt(i);
			if (ob instanceof DocItem) {
				DocItem anv= (DocItem) ob;
				if (anv.sameName(field)) { //sameNameOrKind
					if (count == which) return anv; //.getValue();
					count++;
					}
				}
			else if (ob instanceof FastVector) {  
				DocItem res= findDocItem((FastVector) ob, field, which);
				if (res!=null) return res;
				}
			}
		return null;
	}

		


		//
		// methods for parsing doc
		//
		
	protected DocItem curDocitem;
	protected FeatureItem curFieldItem;
	protected int lastlev= kField;
	protected boolean skipdocs;
	protected String lastfld;
	protected int  inFeatures= kBeforeFeatures;
	protected FastHashtable keepFields; 
	
	
	public FeatureItem getCurFieldItem() { return curFieldItem; }
	public void setCurFieldItem(FeatureItem fi) { curFieldItem= fi; }
	
	public void setKeepField(int kind) {
		if (keepFields==null) keepFields= new FastHashtable();
		Integer ikind= new Integer(kind);
		keepFields.put(ikind,ikind);
		}

	protected boolean keepField(int kind) {
		return (keepFields==null || keepFields.get(new Integer(kind))!=null);
		}
		
	public void setSkipDocs(boolean turnon) {
		skipdocs= turnon;
		if (turnon) {
			setKeepField(kName); // only best info
			setKeepField(kDescription);  
			setKeepField(kAccession);  
			setKeepField(kSeqdata);  
			}
		else keepFields= null;
	}
	
		
		// store a basic locus id/description from various formats 
	public void addBasicName(String desc)
	{
		// from seqwriter:public void setSeqName( String name) 
		if (desc==null) return;
		int i;
		desc= desc.trim();
		if ( desc.indexOf("checksum") >0 ) {
	  	i= desc.indexOf("bases");
	  	if (i<0) i= desc.indexOf(" bp");
	    if (i>0) {
	    	for ( ; i > 0 && desc.charAt(i) != ','; i--) ;
	      if (i>0) desc= desc.substring(0, i);
	      }
	    }
		i= desc.indexOf(' ');
		if (i<=0) i= desc.length();
		if (i>30) i= 30; //???
		if (i == desc.length())  
			replaceDocField(  kName, desc);
		else { 
			String idword= desc.substring(0,i); 
			replaceDocField( kName , idword );
			addDocField( kDescription , desc );
			}
	}


	// convenience methods for programmers
	public void addComment(String comment) {   //I add to Iface
		addDocField( getFieldName( kComment), comment, kField, false);
		}
		
	public void addDate(Date date) {   //I add to Iface
		SimpleDateFormat sdf= new SimpleDateFormat("dd-MMM-yyyy");
  	String datestr= sdf.format(date);
		addDocField( getFieldName( kDate), datestr, kField, false);
		}

	public void addSequenceStats(Bioseq seq) {   //I add to Iface??
		//? putdocval("kStrand",kStrand,"Sequence strandedness");
		SeqInfo si= seq.getSeqStats();
		
		// these should all be 'replaceDocField()' !?
		replaceDocField( kSeqlen, String.valueOf(seq.length()) );
		replaceDocField( kSeqkind, si.getKindLabel());
		int[] agctn= null;
		if (si.getKind() != si.kAmino) agctn= si.getACGTcounts(); // fixme for kAmino ??
		if (agctn!=null) {
			replaceDocField( kNumA, String.valueOf(agctn[0]));
			replaceDocField( kNumC, String.valueOf(agctn[1]));
			replaceDocField( kNumG, String.valueOf(agctn[2]));
			replaceDocField( kNumT, String.valueOf(agctn[3]));
			replaceDocField( kNumN, String.valueOf(agctn[4]));
			}
		}
		
	public void addDocText(String text) {
		inFeatures= kBeforeFeatures;
		addText(text);
		}
	
	public void addFeatureText(String text) {
		inFeatures= kInFeatures;
		addText(text);
		}

	protected void addText(String text) {
		StringTokenizer st= new StringTokenizer(text, "\r\n");
		while (st.hasMoreTokens()) addDocLine( st.nextToken());
		} 	


			//! need a patch here or subclasses to make sure comments, etc. docs don't get put
			// AFTER special fields: kSeqstats (esp. EMBL) kSeqdata (? never in doc) 
			// kFeatureTable - better to put all others above kFeatureTable
			// also make sure kName is 1st field in vector
			// !? dont use this insert test when reading thru standard (gb/embl/..) docs?
			//   just when inserting from program, other sources? -- use inFeatures >= kInFeatures
	protected int featStartItem;
				
	public void addDocField( DocItem di) { 
		if (di!=null) { 
			curDocitem= di; 
			
			/**** // not working ?
			if (inFeatures == kBeforeFeatures) featStartItem= 0;	
			else if (featStartItem == 0 && (inFeatures == kInFeatures || inFeatures == kAtFeatureHeader ) ) {
				featStartItem= rootdoc.size();
				}
			else if (inFeatures >= kInFeatures && rootdoc.size()>2) {
				switch (di.getKind()) {
					case kName: case kSeqstats:  case kSeqdata: case kFeatureTable: case kFeatureItem:  break;
					default: {
						if (featStartItem > 0 && featStartItem < rootdoc.size()) {
							rootdoc.insertElementAt( di, featStartItem); 
							return;
							}
							//? step from last down to non-feature?
						DocItem lastd= (DocItem) rootdoc.lastElement(); // last isn't enough to check !?
						if (lastd!=null) 
						switch (lastd.getKind()) {
							case kSeqstats: case kSeqdata: case kFeatureTable: case kFeatureItem:  
								rootdoc.insertElementAt( di, rootdoc.size()-2); 
								return;
							default: break;
							}
						} break;
					} 
				}
			******/
			rootdoc.addElement( di); // this is/should be only rootdoc.add call
			}
			
		//? also store hash - 1st only, or last only?
		//if (roothash.get(ikind)==null)
		//	roothash.put( new Integer(di.getKind()), di);
	}

	public void addDocField( int fieldId, String value) { //I add to iface
		String field= getFieldName( fieldId);
		//int kind= getBiodocKind(field); 
		addDocField( field,value,fieldId,kField,false);
		}

	public void replaceDocField( int fieldId, String value) { //I add to iface
		String field= getFieldName( fieldId);
		//int kind= getBiodocKind(field); 
		DocItem di= new DocItem(field,value,fieldId,kField); 
		replaceDocItem(fieldId, di);
		}

	public void addDocField( String field, String value, int level, boolean append) {
		int kind= getBiodocKind(field); 
		addDocField(field,value,kind,level,append);
		}
		
	public void addDocField(String field, String value, int kind, int level, boolean append) 
	{ 
		if (!keepField(kind)) return; //?
		if (append && curDocitem != null) {
			//? check that curDocitem.field,kind matches ?
			curDocitem.appendValue( value); 
			// ?? preserve append continuation line feeds ? or store as new item?
			}
		else {
			addDocField( new DocItem(field,value,kind,level) );
			lastlev= level;
			}
	}



				// feature syntax is common to genbank & embl
				//  featfield   location
				//              /featnote=value 
				//              /featnote2=value  
				
		
	public void addFeatureNote( FeatureItem featItem, String key, String value) 
	{ 
		if (!key.startsWith("/")) key= "/"+key;   
		FeatureNote note= new FeatureNote(key, value);
		if (featItem!=null) featItem.putNote(note); 
	}
	
	public void addFeatureNote( FeatureItem featItem, String value)  
	{ 
		if (value!=null) {
			String key;
			int at= value.indexOf('='); // field == value if no = ?
			if (at<0) { key= value; value= ""; }
			else {
				key= value.substring(0, at);
				value= value.substring(at+1);
				}
			addFeatureNote( featItem, key, value);
			}
	}

	public void addFeatureNote( String key, String value) {  //I add to Iface
		addFeatureNote(curFieldItem, key, value);
		}
	
	public void addFeatureNote( String value) {  //I add to Iface
		addFeatureNote(curFieldItem, value);
		}
	

	public void addFeature( FeatureItem fi) { 
		if (fi!=null) { featlist.addElement( fi); curFieldItem= fi; }
		//featkinds= null; // force updateFeatures
		}

	public FeatureItem addFeature( String name, SeqRange sr)  { //I add to Iface
		addFeature( new FeatureItem( name, sr, kFeatField) );
		return curFieldItem;
		}

	public void addFeatures( FastVector addfeats)  { 
		FastVector feats= features();
		for (int i=0; i<addfeats.size(); i++) {
			Object el= addfeats.elementAt(i);
			if (el instanceof FeatureItem && findFeature(el) == null) {
				el= ((FeatureItem) el).clone(); //?
				feats.addElement((FeatureItem) el);  
				}
			}
		}

	public void addFeature(String field, String value, int level, boolean append) 
	{ 
		if (level == kFeatField) { if (!keepField(kFeatureItem)) return; }
		else if (level == kFeatCont) { if (!keepField(kFeatureNote)) return; }
		
		if (level == kFeatField) {
			if (append && curFieldItem != null)  
				curFieldItem.appendValue(value);
			else  
				addFeature( new FeatureItem(field, value, kFeatField) );
			}
		else if (curFieldItem!=null) {
			if (!value.startsWith("/")) curFieldItem.appendNote( value);  // append || 
			else addFeatureNote(curFieldItem, value);
			/*
			if (value!=null && value.startsWith("/")) {  
				int at= value.indexOf('='); // field == value if no = ?
				if (at<0) {
					field= value; value= "";
					}
				else {
					field= value.substring(0, at);
					value= value.substring(at+1);
					}
				curFieldItem.putNote( new FeatureNote(field, value)); 
				}
			else {
				 // continuation?, append to last note value?
				curFieldItem.appendNote( value); 
				}
			*/
			}
	}

	public void deleteFeature(String name, SeqRange sr) 
	{ 
		FeatureItem fi= findFeature(name, sr);
		if (fi!=null) features().removeElement(fi);
	}
	
	
	public FeatureItem findFeature(String name, SeqRange sr) 
	{ 
		FastVector feats= features();
		for (int i= 0, n= feats.size(); i<n; i++) {
			FeatureItem fi= (FeatureItem) feats.elementAt(i);
			if (sr==null) { if (fi.getName().equals(name)) return fi; }
			else if (name==null) { if (fi.getLocation().intersects(sr)) return fi; }
			else if (fi.getName().equals(name) && fi.getLocation().intersects(sr)) return fi;
			}
		return null;
	}

	public FeatureItem findFeature(String name) 
	{ 
		FastVector feats= features();
		for (int i= 0, n= feats.size(); i<n; i++) {
			FeatureItem fi= (FeatureItem) feats.elementAt(i);
			if (fi.getName().equals(name)) return fi;  
			}
		return null;
	}

	public FeatureItem findFeature(Object item) 
	{ 
		FastVector feats= features();
		for (int i= 0, n= feats.size(); i<n; i++) {
			FeatureItem fi= (FeatureItem) feats.elementAt(i);
			if (fi.equals(item)) return fi;
			}
		return null;
	}
	
	public final FeatureItem[] findFeatures( SeqRange sr) { 
		return findFeatures( (String)null, sr);
		}
	public final FeatureItem[] findFeatures( String name) { 
		return findFeatures(name, (SeqRange)null);
		}
	public FeatureItem[] findFeatures(String name, SeqRange sr) 
	{ 
		Vector v= findFeatures(name,sr, null);
		FeatureItem[] ss= new FeatureItem[v.size()];
		v.copyInto(ss);
		return ss;
	}
	
	public final Vector findFeatures( SeqRange sr, Vector addto) { 
		return findFeatures( (String)null, sr, addto);
		}
	public final Vector findFeatures( String name, Vector addto) { 
		return findFeatures( name, null, addto);
		}
	public Vector findFeatures( String name, SeqRange sr, Vector addto) 
	{ 
		if (addto==null) addto= new Vector();
		FastVector feats= features();
		for (int i= 0, n= feats.size(); i<n; i++) {
			FeatureItem fi= (FeatureItem) feats.elementAt(i);
			if (sr==null) {
				if (fi.getName().equals(name)) addto.addElement(fi);
				}
			else if (name==null || fi.getName().equals(name))
				if (fi.getLocation().intersects(sr)) addto.addElement(fi);
			}
		return addto;
	}

	public Vector findFeatures( Hashtable wantfeatures, SeqRange sr, Vector addto) 
	{ 
		if ( wantfeatures == null ) return findFeatures( sr, addto);
		if (addto==null) addto= new Vector();
		Vector removeto= new Vector();
		FastVector feats= features();
		int maxloc= 0;
		
		for (int i= 0, n= feats.size(); i<n; i++) {
			FeatureItem fi= (FeatureItem) feats.elementAt(i);
			String fname= fi.getName();
			SeqRange loc= fi.getLocation();
			maxloc= Math.max(maxloc, loc.stop());
				//? handle wantfeatures.get(fname) == "true", "false"  ?
				// 2010: handle wantfeatures == false as removeRange(...)
		 		
			if ( wantfeatures.get(fname)!=null ) { 
			  if(sr==null || fi.getLocation().intersects(sr)) {
			    if( "false".equals(wantfeatures.get(fname)) )
			      removeto.addElement(fi);
			    else
				    addto.addElement(fi);
				  }
				}
			}
			
	  if(!removeto.isEmpty()) {
//	    if( addto.isEmpty()) { // what?
//	      return addto;  
//	    }

      SeqRange remsr= new SeqRange();
		  for (int j= 0, nj= removeto.size(); j<nj; j++) {	  
			  FeatureItem rfi= (FeatureItem) removeto.elementAt(j);
			  SeqRange rsr= rfi.getLocation();
			  remsr.add(rsr);
			}  

		  if (Debug.isOn) Debug.println("findFeatures removeto: "+remsr);

      int seqlen= maxloc+1; //Integer.MAX_VALUE; 
      if(sr!=null) seqlen= 1 + sr.stop(); // WHAT!????
      SeqRange keepsr= remsr.invert(seqlen); // is this bad?
		  if (Debug.isOn) Debug.println("findFeatures keepsr: "+keepsr);

	    if( addto.isEmpty()) { // is this what we want? all feats but removeset?
	          //  more likely full sequence - removeset; would be source= in std GenBank, but cant count on it
	      addto= findFeatures( keepsr, addto);
	    }
      
      for (int i= 0; i<addto.size(); i++) { // -1; i>=0; i--
        FeatureItem fi= (FeatureItem) addto.elementAt(i);
        SeqRange filoc= fi.getLocation();
        
        if( filoc.intersects(remsr)) 
        {
		      if (Debug.isOn) Debug.println("findFeatures filoc: "+filoc);
          SeqRange inloc= filoc.intersectparts(keepsr); // intersection() is NOT RIGHT, no parts 
          // no intersect == empty range
		      if (Debug.isOn) Debug.println("findFeatures inloc: "+inloc);
          
          //? inloc= inloc.inorder(); //??
          //? filoc.copy(inloc);  
          //? if(inloc.isEmpty()) addto.removeElement( fi ); else 
          fi.setValue(inloc.toString());  // empty becomes 1 here .. bad?
          //if(inloc.isEmpty()) addto.removeElement( fi );
        }
      }
      
	  }
	  
		return addto;
	}


	public String[] getFeaturesAt( SeqRange sr) 
	{ 
		Vector v= new Vector();
		FastVector feats= features();
		for (int i= 0, n= feats.size(); i<n; i++) {
			FeatureItem fi= (FeatureItem) feats.elementAt(i);
			if (fi.getLocation().intersects(sr)) v.addElement(fi.getName());
			}
		String[] ss= new String[v.size()];
		v.copyInto(ss);
		return ss;
	}

 		/** changeflags: SeqRange.kDelete = 1, kInsert = 2, kReorder = 4, kChange = 8; */
	public void updateRange(int changeflags, int start, int length, byte[] changes) 
	{
		FastVector feats= features();
		for (int i= 0, n= feats.size(); i<n; i++) {
			FeatureItem fi= (FeatureItem) feats.elementAt(i);
			fi.updateRange(changeflags, start, length, changes);
			}
	}

	public final void removeRange(SeqRange sr) {
		for (SeqRange loc= sr; loc!=null; loc= loc.next())  
			updateRange( SeqRange.kDelete, loc.start(), loc.nbases(), null);
		}
		
	public final void insertRange(SeqRange sr) {
		for (SeqRange loc= sr; loc!=null; loc= loc.next())  
			updateRange( SeqRange.kInsert, loc.start(), loc.nbases(), null);
		}
		
		//!? dec'99 overload this to handle doc field extract/removal
		// and permit mix of extract/remove, per field (using 'false' or 'true' value of hash key)
	public Hashtable wantedFeatures() { return wantedFeatures; }
	public SeqRange wantedRange() { return wantedRange; }
	public boolean isNotWantedFeature() { return notWantedFeature; }
 
 	protected void copyWanted(BioseqDocImpl src) {
 		setWantedFeatures(src.wantedFeatures, src.wantedRange);
 		extractionNote= src.extractionNote;
 		}
 		
 	public void setWantedFeatures( Hashtable wantfeatures) {
		setWantedFeatures( wantfeatures, null);
		}
		
 	public void setWantedFeatures( Hashtable wantfeatures, SeqRange wantedrange) {
		boolean extract= true;
		if (wantfeatures!=null) 
			try { 
				wantExtractionLoc= (wantfeatures.size()==1 && wantfeatures.containsKey(sExtractRangeFeature));
				extract= ! ("false".equals( wantfeatures.elements().nextElement())); 
				} 
			catch (Exception ex) {} // NoSuchElementException
				/// ^^ is check of 1st all we need?
		setWantedFeatures( extract, wantfeatures, wantedrange);
		}

 	public void setWantedFeatures( boolean extract, Hashtable wantfeatures, SeqRange wantedrange) {
		wantedFeatures= wantfeatures;
		wantedRange= wantedrange;
		notWantedFeature= !extract;
		}
	
	public final SeqRange getFeatureRanges(int offset, int seqlen) {
		SeqRange srlist= getFeatureRanges(wantedFeatures, offset, seqlen);
		wantedRange= srlist; //?
		return srlist;
		}
		
	public SeqRange getFeatureRanges( Hashtable wantfeatures, int offset, int seqlen) 
	{
		if (wantfeatures==null) return null;
		boolean foundfeat= false;
		FastVector feats= features();
		boolean wantExLoc= (wantfeatures.size()==1 && wantfeatures.containsKey(sExtractRangeFeature));
		SeqRange srlist= new SeqRange(); // return empty range for no features found?
		
			// modify to select feats in start..stop == offset..offset+seqlen
			// use wantedRange ???
		SeqRange wantedrange;
		if (wantedRange!=null && !wantedRange.isEmpty()) wantedrange= wantedRange;
		else wantedrange= new SeqRange( offset, seqlen);
		
		ILOOP: for (int i= 0, n= feats.size(); i<n; i++) {
			FeatureItem fi= (FeatureItem) feats.elementAt(i);
			String fname= fi.getName();
			if (wantfeatures.get(fname)!=null) { 
				foundfeat= true;
				SeqRange sr= fi.getLocation();
				if (!sr.intersectsMax(wantedrange)) continue;
				if (wantExLoc) { srlist= sr; break ILOOP; }
				srlist= srlist.joinRange( sr);  
				}
			}
			
		if (notWantedFeature) {
			if (foundfeat) srlist= srlist.invert(seqlen);
			else return null; // process as if no wantedRange
			}
		if (Debug.isOn) Debug.println("getFeatureRanges: "+srlist);
		return srlist;
	}

	/*
	public SeqRange getFeatureRanges0( Hashtable wantfeatures) 
	{
		if (wantfeatures==null) return null;
		SeqRange srlist= new SeqRange(); // return empty range for no features found?
		boolean hasExcluded= false;
		FastVector feats= features();
		for (int i= 0, n= feats.size(); i<n; i++) {
			FeatureItem fi= (FeatureItem) feats.elementAt(i);
			String fname= fi.getName();
			boolean keep;
			if (notWantedFeature) {
				keep= (wantfeatures.get(fname)==null);
				if (!keep) hasExcluded= true;
				}
			else 
				keep= (wantfeatures.get(fname)!=null);
			if (keep) { 
				SeqRange sr= fi.getLocation();
				if (srlist==null) srlist= (SeqRange)sr.clone();
				else srlist= srlist.joinRange( sr);  
				}
			}
		if (Debug.isOn) Debug.println("getFeatureRanges: "+srlist);
		if (notWantedFeature && !hasExcluded) return null; // process as if no wantedRange
		return srlist;
	}
	*/
	
	
		//
		//  methods for writing doc
		//
		
	public int kLinewidth= 79;
	
	protected int indent, subindent, linesout;
	protected PrintWriter pr;
	
	public int linesWritten() { return linesout; }
	public int getIndent() { return indent; }

	final String spaces(int n) { return Fmt.fmt( "", n); }
		
	protected String getFieldLabel( DocItem di)  { return getFieldLabel( di.getLevel(), di); }
	
	public String getContinueLabel( DocItem di)  
	{
		switch (di.getLevel()) 
		{
			default:
				return getFieldLabel( kContinue, di);
			case kFeatField : 
			case kFeatCont  : 
			case kFeatWrap  : 
				return getFieldLabel( kFeatWrap, di);
		}
	}

	protected String getFieldLabel( int level, DocItem di)  
	{
		indent= 5;
		return Fmt.fmt( di.getName(), indent-1, Fmt.LJ)+" "; // teach Fmt.fmt to ensure 1 space for LJ?
	}

	protected String getFieldValue( DocItem di) { 
			// FeatureItem now does this as getValue()
		// if (di instanceof FeatureItem) return ((FeatureItem) di).locationString(); else 
		String val= di.getValue();
		if (val!=null) {
			//val= val.trim();  //? is value ever null?
			if (val.length()>1 && di.getLevel()==kFeatCont) { //?same as di.getKind() == kFeatureNote ?
				char c= val.charAt(0);
				if (!(c=='"' || c=='\'' || (c >= '0' && c <= '9') )) val= '"' + val + '"';
				}
			}
		return val; 
		}

	protected final String getTrimFieldValue( DocItem di) { 
		return getTrimFieldValue( di.getValue() );
		}
	protected String getTrimFieldValue(String val) { 
		if (val!=null) {
			val= val.replace('\n',' '); //? space or eat newline?
			val= val.trim();  //? is value ever null?
			int len= val.length();
			if (len>1) { // && di.getLevel()==kFeatCont is this same as di.getKind() == kFeatureNote ?
				char c= val.charAt(0); 
				if ((c=='"' || c=='\'') && (val.charAt(len-1) == c)) {
					val= val.substring(1,len-1);
					}
				}
			}
		return val; 
		}

	public void setOutput( Writer outs)
	{
		if (outs instanceof PrintWriter) {
			this.pr= (PrintWriter) outs;
			}
		else {
			BufferedWriter bufout;
			if (outs instanceof BufferedWriter) bufout=(BufferedWriter)outs; 
			else bufout= new BufferedWriter(outs); //! need to flush bufout !
			this.pr= new PrintWriter(bufout);
			}
		linesout= 0;
		indent= 0;
	}

	public void setOutput( OutputStream outs)
	{
		BufferedOutputStream bufout;
		if (outs instanceof BufferedOutputStream) bufout=(BufferedOutputStream)outs; 
		else bufout= new BufferedOutputStream(outs); //! need to flush/close bufout !
		this.pr= new PrintWriter(bufout);
		linesout= indent= 0;
	}

		
	public int writeTo(Writer outs) { return writeTo(outs, false); } 
	public int writeTo(Writer outs, boolean doId) 
	{
		this.dontWriteId= !doId;
		setOutput( outs);
		writeAllText();
		return linesWritten();
	}

			//ndiff += doc.compareTo(douts, osi.getdoc());

	public int compareTo(Writer outs, BioseqDocImpl otherdoc) 
	{
		setOutput( outs);
		// add compare flags 'no{field}' to skip some of doc fields
		
		int ddiff= compareDocVectors( "Document", this.documents(), otherdoc.documents() );
		if (ddiff==0 && documents().size()>0) 
			pr.println("# Document is same for n="+  documents().size() );
			
		int fdiff= compareDocVectors( "Features", this.features(), otherdoc.features() );
		if (fdiff==0 && features().size()>0) 
			pr.println("# Features are same for n="+  features().size() );

		return linesWritten();
	}


	protected int compareDocVectors( String lab, FastVector v, FastVector ov) {
		//? turn into DocItem key Hashtable ? -- need to at least sort by keys 1st
		
		int ndiff= 0;
		if (v==null && ov==null) return 0;
		else if (v==null || ov==null) 
			return diff( lab+" null", String.valueOf(v==null), String.valueOf(ov==null));
		if (v.size() != ov.size())   
			ndiff += diff( lab+ " count: ", String.valueOf(v.size()), String.valueOf(ov.size()));
		
		boolean isdoc= (this.documents() == v); // lab.startsWith("Doc");
		if (isdoc) {
			// check by kind
			int kind= 0, which= 0;
			Enumeration en= biodockinds.keys();  
	  	while (en.hasMoreElements()) {
	  		Integer key= (Integer) en.nextElement();
	  		kind= key.intValue();
	  		which= 0;
	  		for (boolean more= true; more; ) {
					DocItem mydi= findDocItem( v, kind, which);
					DocItem  odi= findDocItem( ov, kind, which);
  				more= (mydi!=null && odi!=null);
  				if (more) {
						ndiff += compareDocItems( "Document", mydi, odi);
  					}
  				else if (mydi==null && odi!=null) {
  					lab=  getBiodoclabel(odi.getKind());
  					if (which>0) lab += " "+String.valueOf(which+1);
						ndiff += compareDocItems( lab, mydi, odi);
  					}
  				which++;
  				}  
				}
			}
		
		else {		 
			int n= Math.min( v.size(), ov.size());
			for (int i= 0; i<n; i++) {
				ndiff += compareDocItems( lab, (DocItem)v.elementAt(i), (DocItem)ov.elementAt(i));
				}
			}
		return ndiff;
		}

	protected int compareDocItems( String lab, DocItem v, DocItem ov) {
		int ndiff= 0;
		if (v==null && ov==null) return 0;
		else if (v==null || ov==null) {
			String vnv= ( v==null) ? "(missing)" : getTrimFieldValue(v);
			String vov= (ov==null) ? "(missing)" : getTrimFieldValue(ov);
			ndiff += diff( lab, vnv, vov );
			return ndiff;
			}
		boolean isdoc= lab.startsWith("Doc");
		if (!v.equals(ov)) {
			if ( isdoc && !v.sameKind(ov)) 
				ndiff += diff("kind: ", getBiodoclabel(v.getKind()), getBiodoclabel(ov.getKind()));
			if (!isdoc && !v.sameName(ov)) 
				ndiff += diff("key: ", v.getName(), ov.getName());
			if (! v.sameValue(ov.getValue())) { 
				lab= getBiodoclabel(v.getKind()) + " value: ";
				String vnv= getTrimFieldValue(v);
				String vov= getTrimFieldValue(ov);
				if (!vnv.equals(vov)) ndiff += diff(lab, vnv, vov);
				}
			}
		// check feature notes !?
		if ( (v instanceof FeatureItem) &&  (ov instanceof FeatureItem)) {  
			ndiff += compareDocVectors( "Notes ", ((FeatureItem)v).notes, ((FeatureItem)ov).notes);  
			}
		return ndiff;
		}

  protected int diff(String fld, String newval, String oldval)
  {
  	pr.print( Fmt.fmt(fld, 15, Fmt.LJ) + " "); 
  	pr.print( Fmt.fmt(newval, 15));
  	pr.print(" != ");
  	pr.print(  Fmt.fmt(oldval, 15));
  	pr.println(); linesout++;
  	return 1;
   }
			
			
	public void writeAllText() 
	{ 
		featWrit= false;
		writeTextTop( rootdoc, true); 
			// force write features if not done
		if (!featWrit && features().size()>0) {
			String fn= getFieldName( kFeatureTable);
			//Debug.println("writeAllText - add features "+fn);
			writeDocItem( new DocItem( fn, "", kFeatureTable, kField), true); 
			}
		pr.flush();
	}

	public void writeDocumentText() { writeTextTop( documents(), false);	}
	public void writeFeatureText()  { writeTextTop( features(), false); }

	public String getDocumentText() { 
		ByteArrayOutputStream baos= new ByteArrayOutputStream();
		setOutput( new PrintWriter(baos));
		writeDocumentText(); 
		pr.flush();
		return baos.toString();
		}
		
	public String getFeatureText() { 
		ByteArrayOutputStream baos= new ByteArrayOutputStream();
		setOutput( new PrintWriter(baos));
		writeFeatureText();
		pr.flush();
		return baos.toString();
		}
		

	protected void writeTextTop( FastVector v, boolean writeAll) {
		writeDocVector( v, writeAll);  
		}
	
	protected void writeDocVector( FastVector v, boolean writeAll) {
		for (int i= 0; i<v.size(); i++) writeDocItem( (DocItem)v.elementAt(i), writeAll);
		}

	protected boolean wantFeature(DocItem di) {
		String name= di.getName();
		if (sExtractRangeFeature.equals(name)) return false;
		if (sExtractionFeature.equals(name)) return true;
		if (wantedFeatures==null || wantExtractionLoc) return true;

     //0804 patch wantedRange
		if (wantedRange!=null && !wantedRange.isEmpty() && di instanceof FeatureItem) {
      SeqRange sr= ((FeatureItem) di).getLocation();
      if (!sr.intersectsMax(wantedRange)) return false;
    }
    
		String wantval= (String) wantedFeatures.get(name);
		if (wantval==null) return notWantedFeature; // this is rub - what is default for no explicit value?
		else if ("true".equals(wantval)) return true;
		else if ("false".equals(wantval)) return false;
		else return notWantedFeature; // this is rub - what is default for no explicit value?
		//if (notWantedFeature)  return (wantedFeatures.get(name)==null);
		//else return (wantedFeatures.get(name)!=null);
		}

  protected String extractionNote= sExtractionNote;
  public void setExtractionNote(String notes) {
    extractionNote= (notes==null) ? sExtractionNote : notes;
    }		
    
	protected void writeExtractionFeature() 
	{
		/*
		if (wantExtractionLoc && wantedRange!=null) { // for sExtractRangeFeature
			String dq= (this instanceof XmlDoc) ? "" : "\"";
			
			FeatureItem wfi= new FeatureItem( sExtractionFeature, wantedRange, kFeatField);
			wfi.putNote( new FeatureNote("/note", 
				 dq + "Range of sequence extracted from original."  
			  		+" Feature locations are adjusted to this range." + dq )); 
			writeDocItem( wfi, false);
			}
		else 
		*/
		if (wantedFeatures != null && wantedRange!=null) { 
				//? write this as feature? -- or as comment ?
				//? write list of features removed?
						// dang - for embl/gb need "" around text, not for xml
			String dq= (this instanceof XmlDoc) ? "" : "\"";
			
			FeatureItem wfi= new FeatureItem( sExtractionFeature, wantedRange, kFeatField);
			wfi.putNote( new FeatureNote("/note", dq + extractionNote + dq )); 
				
			writeDocItem( wfi, false);
			}
	}
		
	protected boolean writeKeyValue( DocItem di ) 
	{
		//String name= di.getName();
		//String val= di.getValue(); //? allow non-string vals, like SeqRange ?
		//int kind= di.getKind();  
		//int lev1= di.getLevel(); 
		
		String lab= getFieldLabel( di); // lev1, kind, name, di.hasValue() 
		String val= getFieldValue( di); //val, lev1, kind, name

		if (lab!=null) { 
			// quick hack for hypertext links at this level
			if (di instanceof PrintableDocItem) 
				((PrintableDocItem)di).print( pr, this, lab, val);
			else {
				pr.print( lab );
				if (di.getLevel()==kFeatCont)
					indent += subindent; //!? fix for /translation=... line overflow
				while (val!=null) {
					val= writeWrapText( val, indent, kLinewidth); 
					if (val!=null) {
						String continuelab= getContinueLabel( di);  
						pr.print( continuelab );
						}
					}
				}
			return true;
			}
		else return false;
	}

	protected void adjustFeatureLoc(DocItem nv)
	{
		if (wantExtractionLoc && (nv instanceof FeatureItem)) { 
 		/** changeflags: SeqRange.kDelete = 1, kInsert = 2, kReorder = 4, kChange = 8; */
/*
	public void updateRange(int changeflags, int start, int length, byte[] changes) 
	{
		FastVector feats= features();
		for (int i= 0, n= feats.size(); i<n; i++) {
			FeatureItem fi= (FeatureItem) feats.elementAt(i);
			fi.updateRange(changeflags, start, length, changes);
			}
	}
	*/
			
			}
	}

	protected void writeDocItem( DocItem nv, boolean writeAll) 
	{
		switch (nv.getKind()) 
		{
			case kSeqdata: 
				return; // doing elsewhere

			case kName:
				if (!dontWriteId) writeKeyValue( nv);
				break;
			
			case kFeatureTable:
				if (!featWrit) {
					//Debug.println("writeDocItem - kFeatureTable "+nv);
					if (writeKeyValue( nv) && writeAll) {
						//Debug.println("writeDocItem - writeDocVector "+features().size());
						writeDocVector( features(), writeAll); 
						writeExtractionFeature();
						featWrit= true;
						}
					}
				break;
			
			case kFeatureItem:
				if (wantFeature(nv)) {
					if (writeKeyValue( nv) && (nv instanceof FeatureItem)) {  
						FeatureItem fi= (FeatureItem) nv; 
						if (fi.notes != null) writeDocVector( fi.notes, false);  
						}
					}
				break;
			
			case kFeatureNote:
				writeKeyValue( nv);
				break;
				
			default:
				if (wantFeature(nv)) writeKeyValue( nv); // added dec'99 - okay?
				break;
		}
		
	}
	
	public String writeWrapText( String val)
	{
		return writeWrapText( val, indent, kLinewidth);
	}
	
	protected String writeWrapText( String val, int indent, int width)
	{
		String rval= null;
		int maxw= width - indent;
		int at;
		int vlen= val.length();
		int max2= maxw+2;
		
		at= val.indexOf('\n'); // may01 - handle newlines
		if (at < 0) at= val.indexOf('\r');  
		if (at >= 0 && at <= max2) {
			if (at<vlen) rval= val.substring( at+1).trim();
			val= val.substring( 0, at);
			}
		else if (vlen > maxw) {
			at= val.lastIndexOf(' ',max2);
			if (at < 0) { at= val.lastIndexOf(',', max2); if (at>0) at++; }
			if (at < 0) { at= val.lastIndexOf(';', max2); if (at>0) at++; }
			if (at < 0) { at= val.lastIndexOf('.', max2); if (at>0) at++; }
			if (at < 0) at= maxw; // force break?
			if (at > 10) {
				rval= val.substring( at).trim();
				val= val.substring( 0, at);
				}
			}
		pr.println( val); linesout++;
		return rval;
	}
	

	
};



