//iubio/readseq/GenbankDoc.java
//split4javac// iubio/readseq/GenbankDoc.java date=20-Jun-2001

// iubio.readseq.GenbankDoc.java
// sequence format information (document,features) handlers
// d.g.gilbert, 1997++


package iubio.readseq;


import java.io.*;
import java.util.*;

import iubio.bioseq.*;
import flybase.*;
import Acme.Fmt;


//split4javac// iubio/readseq/GenbankDoc.java line=17
public class GenbankDoc extends BioseqDocImpl
{
	public static String gbprop= "GenbankDoc"; 
	private static FastHashtable glabel2keys = new FastHashtable();  // format label => biodockey
	private static FastProperties	keys2glabel= new FastProperties();  // biodockey => format label

	public static class LocusIndex {
	  final int id, len, bp, ss, mol, cir, div, dat; //?
	  public LocusIndex(int i, int l, int b, int s, int m,  int c, int dv, int dt)
	    { id= i; len= l; bp= b; ss= s; mol= m; cir=c; div= dv; dat= dt; }
	    
	  public static LocusIndex checkin(String s) {
	    if (s!=null) {
        int bp= s.indexOf(" bp ");
        if (bp<0) bp= s.indexOf(" aa ");
        if (bp==30) return locIndex2000;
        //else if (bp==41) return locIndex2003;
        }
	    return locIndex2003;
	    }
	  }
	  
  public static LocusIndex locIndex2000 = new LocusIndex(
      13,
      23,
      31,
      34,
      37,
      43,
      53,
      63
      );
	   
  public static LocusIndex locIndex2003 = new LocusIndex(
      13,//-28
      30,//-40
      42,//-43
      45,//-47
      48,//-53
      56,//-63
      65,//-67
      69//-79
      );
    
  LocusIndex lco= locIndex2003; // output Locus format
	  
/****
 NCBI-GenBank Flat File Release 138.0 ; October 15 2003

Positions  Contents
---------  --------
01-05      'LOCUS'
06-12      spaces
13-28      Locus name
29-29      space
30-40      Length of sequence, right-justified
41-41      space
42-43      bp
44-44      space
45-47      spaces, ss- (single-stranded), ds- (double-stranded), or
           ms- (mixed-stranded)
48-53      NA, DNA, RNA, tRNA (transfer RNA), rRNA (ribosomal RNA), 
           mRNA (messenger RNA), uRNA (small nuclear RNA), snRNA,
           snoRNA. Left justified.
54-55      space
56-63      'linear' followed by two spaces, or 'circular'
64-64      space
65-67      The division code (see Section 3.3)
68-68      space
69-79      Date, in the form dd-MMM-yyyy (e.g., 15-MAR-1991)
***/


	static { 
  	String pname= System.getProperty( gbprop, gbprop);
  	getDocProperties(pname,keys2glabel,glabel2keys);
		}
	
	public GenbankDoc() { gbinit(); }
	
	public GenbankDoc(String idname) { 
		super(); gbinit();
		addBasicName( idname);
		}
		
	public GenbankDoc(BioseqDoc source) {
		super(source); gbinit();
		fFromForeignFormat = !(source instanceof GenbankDoc);
		}

	private void gbinit() {
		kLinewidth= 79;
		}

	private boolean gotOneFT;
	private final static String fFeatureTag= "FEATURES";
	private final static int fFeatIndent= 21, fFieldIndent= 12;
	private final static int fFieldIndent1= fFieldIndent+1;
	protected boolean isAmino; //! for genpep/refprot version
	public void setAmino(boolean turnon) { isAmino= turnon; }

	public void setSourceDoc(BioseqDoc source)
	{
		super.setSourceDoc(source);
		fFromForeignFormat = !(source instanceof GenbankDoc);
	}

	// may01 - fix so blank lines are permitted (still need proper indent)
	// -- keep newlines as given in comments!, elsewhere ?

	public void addDocLine(String line) 
	{ 
		String field= null, value= null;
		boolean append= false;
		int level= kField, at= line.indexOf(' ');
		int len= line.length();
		if (at<0) at= 0; //?
		
		if (at>0) {  
			level= kField;
if (true) {
			int vlen;
			int cut= fFieldIndent;
			//if (len <= cut) cut= len; // error !? blank lines are allowed, len == fFieldIndent
			if (len <= cut) {
				field= line.trim();
				value= ""; //? preserve newlines - may01
				vlen= 0;
				}
			else {
				field= line.substring( 0, cut).trim();
				value= line.substring( cut ); // don't trim, at least not for BASE COUNT line...	
				vlen= value.length();
				}
			int e= vlen;
			while (e>0 && value.charAt(e-1) <= ' ') e--;
			if (e<vlen) value= value.substring(0,e); // trim tail, esp. newline
} else {
			if (at<fFieldIndent-1 && Character.isLetterOrDigit(line.charAt(at+1)))
				at= line.indexOf(' ',at+1); // fix for 'BASE COUNT'
			field= line.substring(0,at);
			while (at<len && line.charAt(at) == ' ') at++;
			value= line.substring(at);
}			
			switch (inFeatures) {
				case kInFeatures: inFeatures= kAfterFeatures; break;
				case kBeforeFeatures: 
					if (field.equals(fFeatureTag)) inFeatures= kAtFeatureHeader;
					break;
				}
			}
			
		else if (at == 0) {
			while (at < len && line.charAt(at) == ' ' && at<fFieldIndent) at++;
			// may01 - treat at==len same as at==fFieldIndent ? (== continue) to fix possible blank line bugs
			//? skip plain old blank lines?? otherwise gets messy
			if (len<2 || at >= len-1) return; 
			
			if (at>=fFieldIndent || at==len) {
						// continue last field value
				value= line.substring(at).trim(); //? trim okay? need for '/' test
				field= lastfld;
				if (inFeatures==kInFeatures) {
					if (lastlev == kFeatField && !value.startsWith("/")) {
						level= kFeatField; append= true;
						}
					else level= kFeatCont; 
 					//field= " ";  //??
					}
				else {
					level= kContinue;  //??
					append= true;
					}
				}
			else {
					// indented subfield
				if (inFeatures==kInFeatures) 
					level= kFeatField; 
				else  
					level= kSubfield;  
				int e= line.indexOf(' ',at);
				if (e<0) e= len;
				field= line.substring(at,e);
				at= e; while (at < len && line.charAt(at) == ' ') at++;
				value= line.substring(at);
				}
			if (value!=null) value= value.trim(); //?? always
			}
		
		if (inFeatures==kInFeatures) addFeature( field, value, level, append);
		else addDocField( field, value, level, append);
		if (inFeatures==kAtFeatureHeader) inFeatures= kInFeatures;
		if (level != kContinue) { lastfld= field; lastlev= level; }
	}


	
	private int addlinefield( String val, int vallen, int btab, int etab, 
														String fldname, int fldkind, int fldlev)
	{
		btab -= fFieldIndent1; etab -= fFieldIndent1;
		if (etab > vallen) return -1;
		String sv= val.substring( btab, etab).trim();
		if (sv.length()<=0) return 0;
		super.addDocField( fldname, sv, fldkind, fldlev, false);
		return 1;
	}
	
	
	public void addDocField( String field, String val, int level, boolean append) 
	{ 
		int kind= kUnknown;
		if (level == kField || level == kSubfield || level == kContinue) {
			kind= getBiodocKind(field); 
			int vlen= val.length();
				
			switch (kind) {
			
				//LOCUS       AF005656     1504 bp    DNA   circular  UNA       02-JAN-1999
				//LOCUS       TRN10A02      178 bp ss-RNA             UNA       04-OCT-1994
			  //............13........23......31.34.37....43........53........63.........75
        // 2003 format change:
        //LOCUS       AI405930                 633 bp    mRNA    linear   EST 02-DEC-2003 
			  //............13...................34......42.45.48......56.......65..69........79
  	    //>> use lco.id, len, bp, ss, mol, cir, div, dat; //?
        // need to Check which format ... and/or use some splitting on whitespace
        
				case kName: {
				    // dang - 2003; spacing changed for longer names; use regex split on spaces ???
					LocusIndex lcr= LocusIndex.checkin(val);
					
					if (0 > addlinefield( val, vlen, lcr.id, lcr.len, field, kName, kField))
						return;
					if (0 > addlinefield( val, vlen, lcr.len, lcr.bp, "length", kSeqlen, kField))
						return;
					if (0 > addlinefield( val, vlen, lcr.ss, lcr.mol-1, "strand", kStrand, kField))
						return;
					if (0 > addlinefield( val, vlen, lcr.mol, lcr.cir, "mol", kSeqkind, kField))
						return;
					if (0 > addlinefield( val, vlen, lcr.cir, lcr.div, "circ", kSeqcircle, kField))
						return;
					if (0 > addlinefield( val, vlen, lcr.div, lcr.dat, "div", kDivision, kField))
						return;
					addlinefield( val, vlen, lcr.dat, vlen+fFieldIndent1, "date", kDate, kField);
					return;
					}
					
					// 2003 -- dropping this...
			  //BASE COUNT      276 a    246 c    295 g    262 t
				//BASE COUNT       27 a     40 c     32 g     17 t      2 others
			  //....................21.......30.......39.......48.......57.........
			 	case kSeqstats: {
					super.addDocField( field, "", kSeqstats, kField, false);	// add blank seqstats, build for output				
					if (0 > addlinefield( val, vlen, 13, 20, "na", kNumA, kSubfield))
						return;
					if (0 > addlinefield( val, vlen, 23, 29, "nc", kNumC, kSubfield))
						return;
					if (0 > addlinefield( val, vlen, 32, 38, "ng", kNumG, kSubfield))
						return;
					if (0 > addlinefield( val, vlen, 41, 47, "nt", kNumT, kSubfield))
						return;
					addlinefield( val, vlen, 50, 56, "nn", kNumN, kSubfield);
			 		return;
					}
					
			 	case kReference: {
			 		int a= val.indexOf("(bases");
			 		int e= val.indexOf(')');
			 		if (a>=0 && e>a) {
			 			String rsi= val.substring(a + "(bases".length(),e).trim();
			 			val= val.substring(0,a).trim();
			 			a= rsi.indexOf(" to ");
			 			if (a>0) rsi= rsi.substring( 0, a) + "-" + rsi.substring(a+4);
						super.addDocField( field, val, kReference, kField, false);
						super.addDocField( "pubseq", rsi, kRefSeqindex, kSubfield, false);
						return;
			 			}
			 		break;
			   	}
			   	
			 	}
	 		}
		super.addDocField( field, val, kind, level, append);
	}


	public String getBiodockey(String field) { return (String) glabel2keys.get(field); }



		//? need/want a getFieldLevel(int kind)
		// so embl/xml subfields match gb - ref subflds, source subflds ...
		
	public String getFieldName(int kind) 
	{ 
		indent= fFieldIndent;
		subindent= 0;
		String lab= null; //getDoclabel( kind);
		String biodockey= getBiodockey(kind);
		if (biodockey!=null) lab= (String) keys2glabel.get( biodockey);

		switch (kind) {
				//! hack - add subfield indent - need another way!
			case kTaxonomy: 
			case kAuthor: 
			case kTitle:  
			case kJournal:  
			case kRefCrossref: subindent= 2; break;
							
			case kFeatureTable: 
				//? if (gotOneFT) return null;
				indent= fFeatIndent; 
				gotOneFT= true;
				return "FEATURES"; 
			case kFeatureItem: return " "; //lab= fldlab; indent= 5; break;  
			case kFeatureNote: return " "; //lab= " "; indent= fFeatIndent; break;  
			}
		return lab;
	}


	protected void writeDocItem( DocItem nv, boolean writeAll) 
	{
		switch (nv.getKind()) 
		{
			case kSeqkind: 
			case kSeqlen: 
			case kDivision: 
			case kDataclass:
			case kSeqcircle:
			case kStrand:
			case kDate:
			case kCrossRef:	 // not in GB
			case kRefSeqindex:
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
				super.writeDocItem( new DocItem( getFieldName(kComment), nv.getValue(), kComment, kField), writeAll); 
				break; // write as comment
	
			case kSeqstats:  
				if (!isAmino) super.writeDocItem( nv, writeAll);
				break;
				
			default:
				super.writeDocItem( nv, writeAll);
				break;
		}
	}
		
	private boolean putlinefield( StringBuffer sb, String val, int etab, int just)
	{
		int width= etab - fFieldIndent1 - sb.length();
		if (val==null) { sb.append( spaces( width) ); return false; }
		else if (just == Fmt.LJ) { sb.append( Fmt.fmt( val, width-1, just)+" "); return true; } 
		  // +" " to ensure 1 space
		else { sb.append( Fmt.fmt( val, width, just)); return true; } 
	}
	
	
	protected String getFieldValue( DocItem di) //String val, int level, int kind, String name
	{ 
		switch ( di.getKind()) 
		{
			case kFeatureTable: 
				return "         Location/Qualifiers";

				// massage kName line
				//LOCUS       AF005656     1504 bp    DNA   circular  UNA       02-JAN-1999
				//LOCUS       TRN10A02      178 bp ss-RNA             UNA       04-OCT-1994
				//LOCUS       NP_007757     515 aa                    INV       24-AUG-1999 << Aminos
		    //............13........23......31.34.37....43........53........63.........75
        // 2003 format change:
        //LOCUS       AI405930                 633 bp    mRNA    linear   EST 02-DEC-2003 
			  //............13...................34......42.45.48......56.......65..69........79
  	    //>> use lco.id, len, bp, ss, mol, cir, div, dat; //?
			case kName: {
				StringBuffer sb= new StringBuffer();
				putlinefield( sb, di.getValue(), lco.len-1, Fmt.LJ);   // teach Fmt.fmt to ensure 1 space for LJ?
				putlinefield( sb, getDocField(kSeqlen), lco.bp-1, 0);
				if (isAmino) sb.append( " aa "); else sb.append( " bp ");
				if (putlinefield( sb, getDocField(kStrand), lco.mol-1, 0))
					sb.append('-'); else sb.append(' ');
				putlinefield( sb, getDocField(kSeqkind), lco.cir, Fmt.LJ);
				putlinefield( sb, getDocField(kSeqcircle), lco.div, Fmt.LJ);
				putlinefield( sb, getDocField(kDivision), lco.dat, Fmt.LJ);
				String dt= getDocField(kDate);
				// chop any frass
				if (dt!=null) {
					int sp= dt.indexOf(' '); // or trunc if dt.length()>11
					if (sp>0) dt= dt.substring(0,sp);
					}
				putlinefield( sb, dt, 79, Fmt.LJ); //+Fmt.TRUNC ?
				return sb.toString();  
				}

// need to fiddle with source/taxonomy fields also
//SOURCE      Acetobacter sp. (strain MB 58) rRNA.
//  ORGANISM  Acetobacter sp.
//            Prokaryotae; Gracilicutes; Scotobacteria; Aerobic rods and cocci;
//            Azotobacteraceae.


				//REFERENCE   1  (bases 1 to 118)
			case kReference: {
				StringBuffer sb= new StringBuffer(di.getValue());
				String rsi= getDocField(kRefSeqindex);
				if (rsi!=null) {
					sb.append("  (bases ");
					int mi= rsi.indexOf('-');
					if (mi>0 && mi < rsi.length()) {
						sb.append(rsi.substring(0,mi)); 
						sb.append(" to "); 
						sb.append(rsi.substring(mi+1));
						}
					else sb.append(rsi);
					sb.append(')');
					}
				return sb.toString();  
				}
				
				//BASE COUNT       27 a     40 c     32 g     17 t      2 others
			  //....................21.......30.......39.......48.......57.........
			case kSeqstats: {
				StringBuffer sb= new StringBuffer();
				putlinefield( sb, getDocField(kNumA), 20, 0); sb.append( " a");
				putlinefield( sb, getDocField(kNumC), 29, 0); sb.append( " c");
				putlinefield( sb, getDocField(kNumG), 38, 0); sb.append( " g");
				putlinefield( sb, getDocField(kNumT), 47, 0); sb.append( " t");
				if ( putlinefield( sb, getDocField(kNumN), 56, 0) ) sb.append(" others");
				return sb.toString();  
				}
				
			default: 
				return super.getFieldValue(di);  
		}
	}


	protected String getFieldLabel( int level, DocItem di) //int level, int kind, String name, boolean hasval
	{
		String name= null;
		indent= 0;
		subindent= 0;
		
		switch (level) {
		
					// need to know if kSubfield for "  " indent !! -- getFieldName() sets subindent
			default:
			case kSubfield  : 
			case kField     :  
				if (fFromForeignFormat) name= getFieldName( di.getKind());
				else name= di.getName();
				if ( name==null || name.length()==0 ) return null;
				indent= fFieldIndent;
				if (level == kSubfield) subindent= 2;  
				return Fmt.fmt( spaces(subindent) + name, indent-1, Fmt.LJ)+" ";
								
			case kContinue  : 
				indent= fFieldIndent;  //?? or fFeatIndent
				return spaces(indent);

			case kFeatField : 
				indent= fFeatIndent;
				return Fmt.fmt( spaces(5) + di.getName(), indent-1, Fmt.LJ)+" ";
				
			case kFeatCont  : 
				name= di.getName();
				if (!name.startsWith("/")) name= "/"+name;
				if (di.hasValue()) name += "=";
				subindent= name.length();
				indent= fFeatIndent; 
				return spaces( indent) + name;

			case kFeatWrap  : 
				indent= fFeatIndent;
				return spaces( indent);
			}
	}

	protected void writeTextTop( FastVector v, boolean writeAll) 
	{
		gotOneFT= false;
		super.writeTextTop( v, writeAll);
	}

	
};

