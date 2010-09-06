//iubio/readseq/GFFSeqFormat.java
//split4javac// iubio/readseq/GFFSeqFormat.java date=26-Jun-2001

// GFFSeqFormat.java
// GFF format i/o
// d.g.gilbert, 2000

package iubio.readseq;

import java.io.*;
import java.util.Date;
import java.util.Enumeration;
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


// GFFSeqFormat: use GFF with Pearson/Fasta to handle seq doc - use two files: .gff and .fasta

//split4javac// iubio/readseq/GFFSeqFormat.java line=33
public class GFFSeqFormat extends BioseqFormat
{
	public String formatName() { return "GFF"; }  
	public String formatSuffix() { return ".gff"; } 
	public String contentType() { return "biosequence/gff"; } 
	public boolean canread() { return true; } //true -- need way to handle two files: doc.gff & seq.fasta for inputs
	public boolean canwrite() { return true; }
	public boolean hasdoc() { return true; }
	public boolean hasseq() { return false; } // new may01 for FlatFeat/GFF/ feature files

	public BioseqReaderIface newReader() { return new GFFSeqReader(); }
	public BioseqWriterIface newWriter() { return new GFFSeqWriter(); }

	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.startsWith("##gff-version")) {
      formatLikelihood= 95;
      if (recordStartline==0) recordStartline= atline;
    	return false;
      }
    else
    	return false;
	}
}

//public 
class GFFSeqReader  extends BioseqReader
{
	protected GFFDoc doc;
	
	public GFFSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		}

	public boolean endOfSequence() {
	  return (true);
		}

		// read doc from .gff, seq from .fasta/.fa ???
    // NO seq in .gff, only doc.  Use some merge doc/seq method to combine doc w/ seq from other file
	 
	protected void read() throws IOException
	{  
		doc= new GFFDoc();
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
	  }
	} 


};



//public 
class GFFSeqWriter  extends BioseqWriter // PearsonSeqWriter?  
{
	//final static int seqwidth= 60, ktab= 0, kspacer= 10, knumwidth= 9, knumflags= 0;  	
		
	//protected final void writeTitle() { writeString("DEFINITION  "); writeln( seqid); }

	public void writeHeader()  throws IOException { 
		super.writeHeader();
		writeln("##gff-version 2");  
		writeln("# seqname\tsource\tfeature\tstart\tend\tscore\tstrand\tframe\tattributes"); 
		}

	public void writeRecordEnd() { } // no extra newline!
	public void writeSeq() { }
		
	public void writeDoc()
	{
		//String cks= checksumString();
		if (seqdoc instanceof BioseqDoc) {
			GFFDoc doc= new GFFDoc((BioseqDoc)seqdoc); 
			boolean doid= true;
			//String docid= doc.getID();
			//if (docid==null || !docid.equals(idword)) { writeID(); doid= false; }
			//if (!doid && doc.getTitle()==null) writeTitle(); // should have, but not before id
			linesout += doc.writeTo(douts, doid);
			}
			
		//call super.writeDoc() to do fasta >seqname line 
 	}
 	
};


// GFF Fields are tab separated:
// <seqname> <source> <feature> <start> <end> <score> <strand> <frame> [group/attribute]  
// with '#' comments, and some special '##' comments
// [group/attribute] is now (v2) both GenBank/EMBL/DDBJ feature tags and ACEDB style tag/value ; tag/value list


//public 
class GFFDoc extends BioseqDocImpl
{
	public static String gffprop= "GFFDoc"; 
	public static String attribInSeparator= "; ";
	public static String attribOutSeparator= " ; ";
	private static FastHashtable elabel2keys = new FastHashtable();  // format label => biodockey
	private static FastProperties	keys2elabel= new FastProperties();  // biodockey => format label
	
	String sversion;
	float version;
	
	static { 
  	String pname= System.getProperty( gffprop, gffprop);
  	getDocProperties(pname,keys2elabel,elabel2keys);
		}

		
	public GFFDoc() { }
	
	public GFFDoc(BioseqDoc source) {
		super(source);
		fFromForeignFormat = !(source instanceof GFFDoc);
		}
		
	public GFFDoc(String idname) { 
		super();
		addBasicName( idname);
		}

	public void setSourceDoc(BioseqDoc source)
	{
		super.setSourceDoc(source);
		fFromForeignFormat = !(source instanceof GFFDoc);
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
	{ 
		line= line.trim(); //??? leave leading spc?	
		if (line.length()<1) ;
		else if (line.startsWith("#")) {
			// ? parse any comments? esp. ##gff-version 
			if (line.startsWith("#gff-version")) { 
				line= line.substring("#gff-version".length()).trim();
				sversion= line;
					// is it always integer, decimal, or somthing cranky like 2.3.4?
				try { version= new Float(sversion).floatValue(); }
				catch (Exception e) { version= 0; }
				}
			}
			
		else {
			boolean append= false;
			int level= kFeatField;
			inFeatures= kInFeatures;
			
			// <0 seqname> <1 source> <2 feature> <3 start> <4 end> <5 score> <6 strand> <7 frame> [8 group/attribute]  
			String[] flds= Utils.splitString(line,"\t");
			if (flds.length < 7) {
				Debug.println("GFFDoc: read malformed line:"+line);
				return;
				}
			String feature = flds[2];
			String location= flds[3] + ".." + flds[4]; // start..end location 
			if ("-".equals(flds[6]))  location= "complement(" + location + ")";

			String group= "";  //Arrghl... GFF doesn't really define grouping attribute ! use group "name_1"...
			vattr.removeAllElements();
			if (flds.length > 8) {
				String newgrp = null;
				String attribs= flds[8];
				int at0= 0;
				while (at0>=0) {
					String attr;
					int at= attribs.indexOf(attribInSeparator, at0);
					if (at>=at0) { attr= attribs.substring(at0, at); at0= at+attribInSeparator.length(); }
					else {  
						attr= attribs.substring(at0); at0= -1; 
						if (attr.endsWith(";")) attr= attr.substring(0,attr.length()-1); //!!
						}
					attr= attr.trim();
					if (attr.length()==0) ;
					else if (attr.startsWith("group")) newgrp= attr;
					else {
						vattr.addElement( attr);
						if (newgrp==null && attr.indexOf("group")>0) newgrp= attr;
						}
					}
				if (newgrp!=null) {
					group= newgrp; //else group= ""; //?
					//Debug.println("GFFDoc: newgrp="+group+" lastgrp="+lastgroup+" line="+line);
					}
				}
				
			if (group.length()>0 && group.equals(lastgroup)) {
				try {
					//FeatureItem fi= this.curFieldItem;
					SeqRange newsr= SeqRange.parse(location);
					SeqRange sr= curFieldItem.getLocation();
					sr.add( newsr);
					}
				catch (Exception sre) { if (Debug.isOn) sre.printStackTrace(); }
				}		
			else {	
				addFeature( feature, location, kFeatField, false);  
				//FeatureItem fi=  new FeatureItem(feature, location, kFeatField) ;
				//addFeature( fi);
					 
				Enumeration en= vattr.elements();
				while (en.hasMoreElements()) {
					String attr= (String) en.nextElement();
					if (!goodval(attr)) continue;
					int spc= attr.indexOf(' '); // change to '='; GFF spec. sez AceDB format, key space value, not key=value
					if (spc>0) attr= attr.substring(0,spc)+"="+attr.substring(spc+1);
					if (!attr.startsWith("/")) attr = "/"+ attr;
					addFeature( feature, attr, kFeatCont, true);  
					}

				if ( "source".equals(feature) && goodval(flds[0])) { // really seq ID
					String oval= curFieldItem.getNoteValue("/ID");
					if (oval==null || !oval.equals(flds[0]))
						addFeature( feature, "/ID="+flds[0], kFeatCont, true);  
						// ^? not if already have /ID == name ?
					}
				if (goodval(flds[1]))  
					addFeature( feature, "/source="+flds[1], kFeatCont, true);  
					
				if (goodval(flds[5]))  
					addFeature( feature, "/score="+flds[5], kFeatCont, true);  

				}
				
			lastgroup= group;
 			lastfld= feature; lastlev= kFeatField;
 			}
			
	}



	public void addDocField(String field, String val, int level, boolean append) 
	{ 
		int kind= kUnknown;
		if (level == kField || level == kSubfield || level == kContinue) {
			kind= getBiodocKind(field); 
			switch (kind) {
				//case kName: return;					
				 
				//SQ   Sequence 1859 BP; 609 A; 314 C; 355 G; 581 T; 0 other;
			 	case kSeqstats: return;
				
				case kCrossRef:	
			  case kDate:
					level= kField;  
			  	append= false; // make sep. fields for each embl DT
			  	break;
	
			 	case kReference: 
				case kTitle: 		
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
				//pr.print(" ");
				val= val.replace('\n',' '); // make sure no newlines
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
		lastkind= kind; 
		switch (kind) 
		{
			//case kTitle:  
			//case kDate:  

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
			
		// <seqname> <source> <feature> <start> <end> <score> <strand> <frame> [group/attribute]  
			case kFeatureItem:
				if ((nv instanceof FeatureItem) && wantFeature(nv)) {
				FeatureItem	fi= (FeatureItem) nv; 
				SeqRange	sr= fi.getLocation();
				String strand= ".";
				String name= fi.getName();
				String id= null;
				
				if ("source".equals(name)) {
					DocItem di= findDocItem( fi.notes, "ID", 0); 
					if (di!=null) id= getTrimFieldValue(di);
					}

				if (id!=null) ;
				else if (lastId!=null) id= lastId; //!?
				else if (idItem!=null) id= getTrimFieldValue(idItem);
				//else if (accItem!=null) id= accItem.getValue();
				else id= getID();
				lastId= id;
				int origin= sr.origin();

				boolean hasparts= (sr.next() != null);
				for (int part= 1; sr != null; part++, sr= sr.next() ) {
					if ( sr.isComplement() ) strand= "-"; else strand= "+"; // fix for non-dna
					pr.print( id );
					pr.print("\t-\t");//source
					pr.print( name);  // feat name
					pr.print("\t");
					pr.print( sr.start()+origin);  // feat loc start
					pr.print("\t");
					pr.print( sr.stop()+origin);  
					pr.print("\t.\t");
					pr.print(strand);  
					pr.print("\t.");// score-strand-frame
		
					keyvalat= 0; 
					boolean donotes= (part == 1 && fi.notes != null);
					if (hasparts || donotes) pr.print("\t"); 
					if (hasparts) { 
						if (part == 1) partid++; 
						pr.print("group " +name+"_"+partid); keyvalat++; 
						}
		  
					if (donotes) {// == group/attributes
						keyvalsep= attribOutSeparator; // is it ' ; ' or '; ' ?
						writeDocVector( fi.notes, false);
						keyvalsep= null; 
						}
						 
				  pr.println(); linesout++;
				  }
				}
				break;

			default:
			case kSeqstats:  
			case kSeqdata: 
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
				return "#"+name+" "; //Fmt.fmt( name, indent, Fmt.LJ);
								
			case kFeatField : 
				return di.getName()+" "; //"FT" +  spaces( fFieldIndent - 2)  + Fmt.fmt( di.getName(), indent - fFieldIndent, Fmt.LJ); 
				
			case kFeatCont  : 
				name= di.getName();
				if (di.hasValue()) name += " "; // or "=" ?
				if (name.startsWith("/")) name= name.substring(1);
				return name; //"FT" +  spaces( indent - 2)  + name;

			case kFeatWrap  : 
				return ""; //Fmt.fmt( "FT", indent, Fmt.LJ);
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

/*
Argh !! is there any gff standard for feature groups?

 <0 seqname> <1 source> <2 feature> <3 start> <4 end> <5 score> <6 strand> <7 frame> [8 group/attribute]  

wormbase/sanger ---------

CHROMOSOME_X    curated exon    74797   75033   .       +       .       Sequence
 "AC8.1"
CHROMOSOME_X    curated exon    76406   76621   .       +       .       Sequence
 "AC8.1"
CHROMOSOME_X    curated exon    76952   77205   .       +       .       Sequence
 "AC8.1"
CHROMOSOME_X    curated exon    77252   77330   .       +       .       Sequence
 "AC8.1"
CHROMOSOME_X    curated exon    80731   80811   .       +       .       Sequence
 "AC8.3"
CHROMOSOME_X    curated exon    80855   81064   .       +       .       Sequence
 "AC8.3"
CHROMOSOME_X    curated exon    81109   81235   .       +       .       Sequence
 "AC8.3"

gadfly ------------

AE003457.2      gadfly  gene    3785    6710    .       +       .       genegrp=
CG5819; name=CG5819; symbol=CG5819
AE003457.2      gadfly  exon    21554   21778   .       -       .       genegrp=
CG3413; transgrp=CT11415; name=CT11415; name=CG3413:1
AE003457.2      gadfly  exon    18066   18114   .       -       .       genegrp=
CG3413; transgrp=CT11415; name=CT11415; name=CG3413:2
AE003457.2      gadfly  exon    8764    10867   .       -       .       genegrp=
CG3413; transgrp=CT11415; name=CT11415; name=CG3413:3
AE003457.2      gadfly  translation     8767    10797   .       -       .
genegrp=CG3413; transgrp=CT11415; name=CT11415; name=pp-CT11415
AE003457.2      gadfly  transcript      8764    21778   .       -       .
genegrp=CG3413; transgrp=CT11415; name=CT11415
AE003457.2      gadfly  exon    21554   21774   .       -       .       genegrp=
CG3413; transgrp=CT39092; name=CT39092; name=CG3413:4
AE003457.2      gadfly  exon    8764    10867   .       -       .       genegrp=
CG3413; transgrp=CT39092; name=CT39092; name=CG3413:3
AE003457.2      gadfly  translation     8767    10797   .       -       .
genegrp=CG3413; transgrp=CT39092; name=CT39092; name=pp-CT39092
AE003457.2      gadfly  transcript      8764    21774   .       -       .
genegrp=CG3413; transgrp=CT39092; name=CT39092
AE003457.2      gadfly  gene    8764    21778   .       -       .       genegrp=
CG3413; name=CG3413; symbol=CG3413
AE003457.2      gadfly  exon    27601   27658   .       +       .       genegrp=



*/

