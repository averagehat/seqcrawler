//iubio/readseq/BioseqRecord.java
//split4javac// iubio/readseq/BioseqRecord.java date=27-May-2001

// BioseqRecord.java
// d.g.gilbert 

package iubio.readseq;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import iubio.bioseq.*;
		 
import flybase.Debug;

/**
  * BioseqRecord, like SeqFileInfo is an object to join seq and seqdoc objects.
  * It also includes some manipulations for managing features of sequences
  * @author  Don Gilbert
  * @version 	July 1999

 <pre>
  Sample program for feature extraction
  
import java.io.*;
import java.util.*;
import iubio.bioseq.*;
import iubio.readseq.*;
import flybase.Utils;

public class features {
  public static void main( String[] args) 
  {
    if (args==null || args.length==0) System.out.println(
      "Usage: jre -cp .:readseq.jar features find=exon,CDS,... inputfile(s)");
    else try {
      Hashtable feathash= new Hashtable();
      Vector names= new Vector();
      PrintStream out= System.out;
      for (int iarg= 0; iarg &lt; args.length; iarg++) {
        if (args[iarg].startsWith("find=")) {
          String[] ss= Utils.splitString( args[iarg].substring(5), " ,;:");
          out.println("Find features:");
          for (int k=0; k &lt; ss.length; k++) { feathash.put( ss[k], ss[k]); out.println(ss[k]); }
          }
        else names.addElement(args[iarg]);  
        }
      Readseq rd= new Readseq();
      Enumeration en= names.elements();
      while (en.hasMoreElements()) {
        String name= rd.setInputObject( en.nextElement());
        out.println("Reading from " + name);
        if ( rd.isKnownFormat() &amp;&amp; rd.readInit() )  {
          while (rd.readNext()) {
            SeqFileInfo sfi= rd.nextSeq();
            BioseqRecord bsrec= new BioseqRecord(sfi);
            out.println(bsrec);
            FeatureItem[] fits= bsrec.findFeatures( feathash);
            if (fits==null) out.println("  No such features found.");
            else {
              out.println("  Extracted features");
              for (int k= 0; k &lt; fits.length; k++) out.println( fits[k]);
              out.println("  Extracted sequence");
              try { 
                Bioseq bseq= bsrec.extractFeatureBases( feathash);
                out.println(bseq); out.println();
                }
              catch (SeqRangeException sre) { out.println(sre.getMessage()); }
              }
            }
          }
        }
      }
    catch (Exception ex) { ex.printStackTrace(); }
  }
}

	</pre>
  */
	
	//? merge this with SeqFileInfo, or at least make it a subclass of SeqFileInfo
	
//split4javac// iubio/readseq/BioseqRecord.java line=82
public class BioseqRecord 
	extends SeqFileInfo //? or replace it
{
		// for current seq
	// public int seqlen, offset;

		// superclass SeqFileInfo has these as bare, public Objects - should be protected
	// superclass is Object seq
	// public Bioseq seq;  
	// superclass is Object seqdoc
	// public BioseqDocImpl seqdoc; 
	// public String seqid;
	
	public BioseqRecord() {}

	public BioseqRecord( Bioseq seq, BioseqDocImpl seqdoc) { 
		set( seq, seqdoc, null);
		}
	public BioseqRecord( Bioseq seq, BioseqDocImpl seqdoc, String seqid) { 
		set(seq, seqdoc, seqid);
		}

	public BioseqRecord( SeqFileInfo si) { 
		set( si.seq, si.seqdoc, si.seqid);
		offset= si.offset;
		seqlen= si.seqlen; //? or use si.seq length
		}
			
	public void set( Object seqob, Object docob, String id) { 
		this.offset= 0;
		this.seqid = id;
		if (docob instanceof BioseqDocImpl) {
			seqdoc= (BioseqDocImpl) docob;
			}
		else seqdoc= null;  
	
		if (seqob==null) seq= null;
		else if (seqob instanceof Bioseq) seq= (Bioseq) seqob;
		else if (seqob instanceof byte[]) {
			Bioseq bs= new Bioseq();
			bs.setbases((byte[])seqob);
			seq= bs; 
			} 
		else seq= new Bioseq(seqob); //? will this except if  bad data?
		seqlen= (seq!=null) ? getseq().length() : 0; //? or trust si.seqlen ??

		if (hasdoc() && (seqid==null || seqid.startsWith(gBlankSeqid))) {
			String did= getdoc().getID(); 
			if (did!=null) seqid= did;
			}
		if (seqid==null || seqid.equals(gBlankSeqid))
			seqid= getNextBlankID();
  }

	
	SeqInfo seqinfo;
	long checksum;
	public long getChecksum() {
		if (seqinfo==null && hasseq()) {
		  seqinfo= this.getseq().getSeqStats();
		  checksum= seqinfo.getChecksum();
	     // BioseqWriter.CRC32checksum( this.getseq(), this.offset(), this.length());
      }
	  return checksum;
	  }

	String mytitle;
  public String getTitle() {
    if (mytitle==null && hasdoc()) mytitle= getdoc().getTitle();
		return mytitle;
	  }
	  
	//public final boolean hasseq() { return (seq!=null && seqlen>0); }
	//public final boolean hasdoc() { return (seqdoc!=null); }
	//public final boolean hasid() { 
	// return (seqid!=null && seqid.length()>0 && (seqid != SeqFileInfo.gBlankSeqid) ); }
	
		// force bare superclass Objects to our classes
	public final Bioseq getseq() { return (Bioseq) seq; }
	public final BioseqDocImpl getdoc() { return (BioseqDocImpl) seqdoc; }
	

	public void clear() // drop big data
	{
	  if (seq instanceof Bioseq) ((Bioseq)seq).clear();
	  if (seqdoc instanceof BioseqDocImpl) ((BioseqDocImpl)seqdoc).clear();
	  super.clear();
	}
	
	  
	public String toString() {
		StringBuffer sb= new StringBuffer( this.getClass().getName());
		sb.append(": id="); sb.append( seqid);
		sb.append(", length="); sb.append(seqlen);
		if (hasdoc()) {
			sb.append(", title=\""); sb.append(getdoc().getTitle()); sb.append('"');
			if (getdoc().hasDocument()) { sb.append(", n.docitems="); sb.append(getdoc().documents().size()); }
			if (getdoc().hasFeatures()) { sb.append(", n.features="); sb.append(getdoc().features().size()); }
			}
		return sb.toString();
		}

	
	public SeqFileInfo getSeqFileInfo() { // ? drop now we are subclass of SeqFileInfo ?
		SeqFileInfo sfi= new SeqFileInfo(); //? or new BioseqRecord()
		copyTo( sfi);
		return sfi;
		}
		
	public void copyTo( SeqFileInfo si) {
		si.seqlen= seqlen;
		si.offset= offset;
		si.seq= seq;
		si.seqdoc= seqdoc;
		si.seqid= seqid;
		}

	public Object clone() {
		BioseqRecord bsr= (BioseqRecord) super.clone();
		//? change anything?
		return bsr;
		}


		// some useful seqdoc functions
		
	public final FeatureItem[] findFeatures(String feature) {
		return findFeatures( new String[] {feature});
		}
	public final FeatureItem[] findFeatures( String[] features) {
		Hashtable feath= new Hashtable();
		for (int i=0; i<features.length; i++) feath.put(features[i], "true");
		return findFeatures(feath , null);
		}
	public final FeatureItem[] findFeatures( Hashtable wantfeatures) {
		return findFeatures(wantfeatures , null);
		}
	public final FeatureItem[] findFeatures( SeqRange wantrange) {
		return ( hasdoc() ) ? getdoc().findFeatures( wantrange) : null;
		//return findFeatures(null , wantrange);
		}

	public FeatureItem[] findFeatures( Hashtable wantfeatures, SeqRange wantrange)
	{
		if ((wantfeatures!=null || wantrange != null) && hasdoc()) {
			if (wantfeatures==null) return getdoc().findFeatures( wantrange);
			Vector v= new Vector();

				// 2010: handle wantfeatures == false as removeRange(...)
			//? if(wantrange == null && "false".equals( wantfeatures.elements().nextElement()))
			//   wantrange= new SeqRange( this.offset(), this.length());
			
			v= getdoc().findFeatures( wantfeatures, wantrange, v);
			/*
				// this produces unsorted list grouped by featurename
			Enumeration names= wantfeatures.keys();
			while (names.hasMoreElements()) {  
				String name= (String) names.nextElement();
				v= getdoc().findFeatures( name, wantrange, v);
				}
			*/
			if (v.isEmpty()) return null;
			FeatureItem[] fs= new FeatureItem[v.size()];
			v.copyInto(fs);
			return fs;
			}
		else return null;
	}

	public final Bioseq extractFeatureBases( FeatureItem feature) 
		throws SeqRangeException
	  { return extractBases( feature.getLocation()); }

	public final Bioseq extractFeatureBases( Hashtable wantfeatures)  
		throws SeqRangeException
		{ return extractRemoveFeatureBases( true, wantfeatures, null); }
		
	public final Bioseq extractFeatureBases( Hashtable wantfeatures, SeqRange wantrange)  
		throws SeqRangeException
		{ return extractRemoveFeatureBases(true, wantfeatures, wantrange); }
		
	public final Bioseq removeFeatureBases( Hashtable wantfeatures)  
		throws SeqRangeException
		{ return extractRemoveFeatureBases(false, wantfeatures, null); }
		
	public final Bioseq removeFeatureBases( Hashtable wantfeatures, SeqRange wantrange) 
		throws SeqRangeException
		{ return extractRemoveFeatureBases(false, wantfeatures, wantrange); }
		
	public Bioseq extractRemoveFeatureBases( boolean extract, 
				Hashtable wantfeatures, SeqRange wantrange) throws SeqRangeException
	{
		if (wantfeatures!=null && hasdoc()) {
			getdoc().setWantedFeatures( extract, wantfeatures, wantrange);
			SeqRange featsr= getdoc().getFeatureRanges(0, seqlen);
			Bioseq bseq= extractBases( featsr);
			getdoc().setWantedFeatures( extract, null, null);
			return bseq;
			}
		else return null;
	}
	
		
	public Bioseq extractBases( SeqRange range) throws SeqRangeException
	{
    return BioseqWriter.extractBioseqBases( getseq(), range, seqlen, seqid);
  }
 
  /***** 
	public Bioseq extractBases(Bioseq inseq, SeqRange range) throws SeqRangeException
	{
		if (range==null) throw new SeqRangeException("Null SeqRange");  
		int totlen= 0;
		boolean mainrevcomp= range.isComplement() ;   // test each .next ?
		
		// if (!range.isComplex()) revise to use same Bioseq.bSeq array, w/ offset, length change
		int seqstart= range.start();
		int seqstop = range.max();
		int seqbases= seqstop - seqstart + 1;
		
		//xr//boolean hasremote= range.isPartRemote(null); // null should be default loc id:
		for (SeqRange sr= range; sr!=null; sr= sr.next())  {
			//xr//if (sr.isRemote()) continue;
			totlen += sr.nbases();
			}
		StringBuffer msg= new StringBuffer("extractBases - Bad range ");
		if (totlen > 0) {
			int bat= 0;
			byte[] ba= new byte[totlen];
			byte[] bases= null; //= bioseq.toBytes(); // always? or check bioseq.isBytes() ?
			
			if (inseq.isBytes()) {
				seqstart= 0;
				bases= inseq.toBytes();
				}
			else {
				bases= inseq.toBytes(seqstart, seqbases, 0); // only get needed bytes?
				}
				
			int seqkind= (bases==null)? 0 : inseq.getSeqtype(bases,0,bases.length);
			
			boolean isamino= (seqkind == Bioseq.kAmino);
			boolean isrna= (seqkind == Bioseq.kRNA);
			for (SeqRange sr= range; sr!=null; sr= sr.next())  {
				//xr//if (sr.isRemote()) continue; // warn?
				int start= sr.start(); // is this correct for this.offset>0 ?
				start -= seqstart;
				int len= sr.nbases();

				boolean revcomp;
				//if (mainrevcomp) revcomp= mainrevcomp; // no, need to check part op 1st
				//else revcomp= sr.isComplement(); 
				if (sr.opint()<sr.opMax) revcomp= sr.isComplement(); // should be sr.hasOp()
				else  revcomp= mainrevcomp;
				
				boolean err= false;
				if (start < 0 ) { err= true; msg.append(start).append( " start<0"); start= 0; }
				else if (start+len > seqlen ) { err= true; 
					msg.append(start+len).append( " end>").append(seqlen);
					len= Math.max(0, seqlen - start);
					}
				else if (bat+len > totlen) { err= true; 
					msg.append(bat+len).append("  size>").append(totlen);
					len= Math.max(0, totlen - start);
					}
				else err= false;
				if (err) {
					msg.append(" of sr=").append(sr).append( " in r=").append( range);
					msg.append(", record=").append(seqid); 
					if (Debug.isOn) Debug.println(msg.toString());
					else throw new SeqRangeException(msg.toString());
					}
					
				if (revcomp) {
					int baend= totlen - bat - 1;
					if (isamino) {
						for (int i= 0; i<len; i++) ba[baend-i]= bases[start+i];
						}
					else {
						for (int i= 0; i<len; i++)  
							ba[baend-i]= BaseKind.nucleicComplement( bases[start+i], isrna);
						}
					}
					
				else
					System.arraycopy( bases, start, ba, bat, len);
					
				bat += len;
				}
			Bioseq newseq= new Bioseq();
			newseq.setbases(ba);
			return newseq;
			}
		else
			throw new SeqRangeException("Empty SeqRange");  
	}
  *******/
  
}



