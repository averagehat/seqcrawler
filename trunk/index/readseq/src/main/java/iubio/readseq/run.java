//iubio/readseq/run.java
//split4javac// iubio/readseq/readseqrun.java date=28-Jun-2002

// iubio.readseq readseqrun.java

/*
main calls
	done: iubio.readseq.run => classic readseq commandline options
	done: iubio.readseq.app => window/gui interface (swing-based)
	done: iubio.readseq.cgi => HTTP server cgi interface 
	
	use IBM trick - put real main in package and wrapper main w/o package ?
	other uses:  rs= new iubio.readseq.run(); rs.getargs(args); rs.run(); -- support Runnable iface?
*/


package iubio.readseq;


import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Properties;

import iubio.bioseq.SeqRange;
import iubio.bioseq.SeqRangeException;
import iubio.readseq.*;

import flybase.Args;
import flybase.Debug;
import flybase.Utils;
import flybase.AppResources;
import flybase.FastVector;
import flybase.Environ;


//split4javac// iubio/readseq/readseqrun.java line=239
public class run 
	implements Runnable
{	
	public static String kReadseqcHelp = "rez/Readseqc-help.html";
	static String kInputStringKey = "indata=";
	
	static boolean dowritemask= false;
	static boolean s_reportmemory = false;
	static Runtime s_rt = Runtime.getRuntime();

	boolean degap, dolist, docompare, doall, dochecksum, dopipe, dolowercase, dohtml, hasoutformat,
					dotranslate, doaminotrans, douppercase, doreverse, doPairedDocNSeq, doUnpairDocNSeq,
					dotime, noEmptyFiles, doclassic, verbose, verboseClassic, doformhelp;
	char degapc= '-';
	String  informat, outname, outdirname, indirname, itemlist;
	String  outformat, featsubrange, extractrange;
	String	translates, compareval;
	String	featlist, nofeatlist, keepfeatures;
	Args argv;
	FastVector innames= new FastVector();
	Hashtable exfeatures= new Hashtable();
	SeqRange featSubrange, extractRange;
	WriteseqOpts pretty= new WriteseqOpts();
	File outdir, indir;
	
	
	static boolean bExtractRangeOkay = true; // not ready 17may01 - okay 25may ?
	
	
	public static void main(String[] args) {
		new run(args);
		}

	public run() { initrun(); }
	
	public run( String[] args) 
	{
    initrun();
    System.err.println( Readseq.version);
		getargs( args);
		
		if (Debug.isOn) { 
			verbose= true; dotime= true; 
			s_reportmemory= true; 
			bExtractRangeOkay = true; 
			}
		if (s_reportmemory) reportMemory( "Free/total memory at start:", s_rt);
    long tstart = System.currentTimeMillis();
		
		run();

    if (verbose || dotime || s_reportmemory) {
	    long telapsed = System.currentTimeMillis() - tstart;
   		System.err.println("time=" + telapsed);
			if (Debug.isOn) System.err.println("checksum time=" + BioseqWriter.checksumTime);
	    if (s_reportmemory) {
	      reportMemory("Free/total memory after processing:", s_rt);
	      s_rt.gc();
	      s_rt.runFinalization();
	      s_rt.gc();
	      reportMemory("Free/total memory after gc:", s_rt);
				}
			}
	}

	protected void initrun() 
	{
		// these inits mainly are a problem for test(), otherwise new run() is usual
		degap= dolist= docompare= doall= dochecksum= dopipe= dolowercase= dohtml= hasoutformat=
					dotranslate= doaminotrans= douppercase= doreverse= doPairedDocNSeq= doUnpairDocNSeq=
					dotime= doclassic= verbose= doformhelp = false;

		compareval= keepfeatures= nofeatlist= featlist= null;
			translates= extractrange= featsubrange= null;
			outformat= informat= outname= outdirname= indirname= itemlist= null;
		//outname= Environ.gEnv.get("output",outname);
		//? outdir= indir= null;
		
		innames.removeAllElements();  

    doclassic= true; // want opt to turn off
		XmlSeqWriter.dtdUrl= Environ.gEnv.get("APP_SOURCE_URL");
		XmlSeqWriter.includeDTD= Environ.gEnv.isTrue("XML_INCLUDE_DTD");

		String vers= Readseq.version;
		int at= vers.indexOf("version ");
		if (at>=0) vers= vers.substring(at+"version ".length()).trim();
		// String evers= Environ.gEnv.get("APP_VERSION");
		//?! change 2.1.1 type string to integer, compare versions?
		Environ.gEnv.set("APP_VERSION", vers); // rely on compiled Readseq.version
		
		outformat= Environ.gEnv.get("format","biosequence/fasta");
		//! check env for all options? == user prefs settings, cgi env settings

		doall= Environ.gEnv.isTrue("all",doall);
		dochecksum= Environ.gEnv.isTrue("checksum",dochecksum);
		//checksumtype= Environ.gEnv.get("checksumtype",checksumtype); // 2007jun patch for swiss std CRC32
		doreverse= Environ.gEnv.isTrue("reverse",doreverse);
		s_reportmemory= Environ.gEnv.isTrue("memory",s_reportmemory);
		dotime= Environ.gEnv.isTrue("time",dotime);
		doclassic= Environ.gEnv.isTrue("classic",doclassic);
		dolowercase= Environ.gEnv.isTrue("caselower",dolowercase);
		douppercase= Environ.gEnv.isTrue("CASEUPPPER",douppercase);
		degap= Environ.gEnv.isTrue("degap",degap); //? check degap=gapc
		dolist= Environ.gEnv.isTrue("list",dolist);
		dopipe= Environ.gEnv.isTrue("pipe",dopipe);
		verbose= Environ.gEnv.isTrue("verbose",verbose);
		doPairedDocNSeq= Environ.gEnv.isTrue("pair-feature-seq",doPairedDocNSeq);
		doUnpairDocNSeq= Environ.gEnv.isTrue("unpair-feature-seq",doUnpairDocNSeq);
		doformhelp= Environ.gEnv.isTrue("formhelp",doformhelp);
		
		//itemlist= Environ.gEnv.get("item",itemlist);
		featlist= Environ.gEnv.get("features",featlist);
		nofeatlist= Environ.gEnv.get("nofeatures",nofeatlist);
		keepfeatures= Environ.gEnv.get("keepfeatures",keepfeatures);
		featsubrange= Environ.gEnv.get("subrange",featsubrange);
		extractrange= Environ.gEnv.get("extract",extractrange);
				
			// add pretty format opts
		pretty.seqwidth= Environ.gEnv.getInt("pretty.width", pretty.seqwidth);
		pretty.tab= Environ.gEnv.getInt("pretty.tab", pretty.tab);
		pretty.spacer= Environ.gEnv.getInt("pretty.colspace", pretty.spacer);
		pretty.baseonlynum= ! Environ.gEnv.isTrue("pretty.gapcount", !pretty.baseonlynum);
		pretty.nameleft= Environ.gEnv.isTrue("pretty.nameleft",pretty.nameleft);
		pretty.nameright= Environ.gEnv.isTrue("pretty.nameright",pretty.nameright);
		pretty.nametop= Environ.gEnv.isTrue("pretty.nametop",pretty.nametop);
		pretty.namewidth= Environ.gEnv.getInt("pretty.namewidth", pretty.namewidth);
		pretty.numleft= Environ.gEnv.isTrue("pretty.numleft",pretty.numleft);
		pretty.numright= Environ.gEnv.isTrue("pretty.numright",pretty.numright);
		pretty.numtop= Environ.gEnv.isTrue("pretty.numtop",pretty.numtop);
		pretty.numbot= Environ.gEnv.isTrue("pretty.numbot",pretty.numbot);
		pretty.domatch= Environ.gEnv.isTrue("pretty.match",pretty.domatch); // collect matchchar
		pretty.interline= Environ.gEnv.getInt("pretty.interline", pretty.interline);

	}
		
		 
	public void setInputFiles( File[] inputfiles) 	{
		//this.inputfiles= inputfiles;
		//? innames.removeAllElements();
		if (inputfiles!=null)
			for (int i=0; i<inputfiles.length; i++) innames.addElement( inputfiles[i]);
		}
		
	public void setInputNames( String[] names) {
		innames.removeAllElements(); // why here and not there?
		if (names!=null)
			for (int i=0; i<names.length; i++) innames.addElement(names[i].trim());
		}
 
	public void setInputObjects( FastVector list) {
		innames.removeAllElements();
		if (list!=null)
			for (int i=0; i<list.size(); i++) innames.addElement(list.elementAt(i));
		}

	public void addInputObject( Object item) {
		//? if item is string name, append indir if exists?
		if (item!=null) innames.addElement(item);
		}
	
	public boolean haveInputData() { 
		return !innames.isEmpty(); 
		//return !(inputfiles == null && innames.isEmpty()); 
		}

	protected void message(String s) {
		BioseqReader.message(s);
		//System.err.println(s);
		}
	
	protected String guessOutname(int outid)
	{
		String outname= null; 
		for (int k= 0; outname==null && k<innames.size(); k++) {
			Object el= innames.elementAt(k);
			if (el instanceof File) outname= ((File)el).toString(); 
			else if (el instanceof URL) outname= ((URL)el).getFile(); 
			else if (el instanceof String) {
				outname= (String)el;
				if (outname.startsWith(kInputStringKey)) outname= null; //?
				else if (outname.length()>50) {
					//int at=
					}
				}
			}
			 
		if (outname==null) outname= "run-output"; // add random num?
		if (docompare) outname += ".diff";
		else if (dolist) outname += ".list";
		else outname += BioseqFormats.formatSuffix(outid);
		return outname;
	}

 /*
	   may01 -- doPairedDocNSeq to merge doc from one infile w/ seq from other 
		 only works now when innames is paired list of GFF/FlatFeat + seq files
		 assume hasdoc pair is 'one' entry, read its doc and apply to seq pair
		 not paired? or use pairdoc if !null
  	 ??? need to match IDs from doc featues and seq
  	 if this is GFF + fasta pair
		 worse - always/mostly want to split features extracted into
		 separate output records (in fasta,gb,whatever outformat)
		? assume/insist that input seq has one record to match one input feat doc
		 output is either one merged doc/seq or multiple extracted feature records
 */
 
 
  final boolean bUseLibdiff= true; // option?
 	// docompare; second=  en.nextElement()

 	protected void comparePair(String inname, Readseq rd, Object second, 
 	  int forceInformat, Writer outs )
 		throws IOException
 	{
		Readseq	rd2= new Readseq();
		rd2.setInDirectory(indir); // null ok
		String in2name= rd2.setInputObjectChecked( second);
		if (verbose || dotime) message("Comparing to "+in2name);
		if (in2name == null) return;				

		if (forceInformat > 0) rd2.setInputFormat(forceInformat);
		if (!rd2.isKnownFormat())  
			{ message("Unknown biosequence format for input " + in2name); return; }
		if (forceInformat > 0) 
			rd2.getBioseqFormat().setVariant( informat);
		if (!rd2.readInit()) 
			{ message("Error initializing readseq for input " + in2name); return; }
		
		CompareSeqWriter cmpwriter= new CompareSeqWriter();
		cmpwriter.setOutput( outs);
		cmpwriter.setFlags( compareval);
		cmpwriter.setSourceNames( in2name, inname);
		boolean more= true;			
				 
if (bUseLibdiff) {
    more= true;					 
    for (int irec= 1; more; irec++) {
			SeqFileInfo rec= rd.readAt(irec); 
			if (rec==null) more= false;
			else cmpwriter.addLib( inname, new BioseqRecord(rec));
			}
    more= true;					 
    for (int irec= 1; more; irec++) {
			SeqFileInfo rec= rd2.readAt(irec); 
			if (rec==null) more= false;
			else cmpwriter.addLib( in2name, new BioseqRecord(rec));
			}

		cmpwriter.compareLibs( inname, in2name); // in2 vs in1 always?
		
} else {		
		cmpwriter.writeHeader(); 
		for (int irec= 1; more; irec++) {
			SeqFileInfo rec1= rd.readAt(irec); 
			SeqFileInfo rec2= rd2.readAt(irec); 
			if (rec1 == null || rec2 == null) {
				more= false;
			 	if (rec2 != null) cmpwriter.writeln("End of file for "+inname);
				if (rec1 != null) cmpwriter.writeln("End of file for "+in2name);
				}
			else {
				cmpwriter.compareTo( new BioseqRecord(rec1) );
				if ( cmpwriter.setSeq( rec2)) cmpwriter.writeSeqRecord();
				}
			}
}
		cmpwriter.writeTrailer();	
		cmpwriter.close();  
	}
	
	
/*
aug01 patch for 'classic' readseq - match verbose output of that old classic as some (Pise) parse it

readseq.c main verbose calls:
-- once at top:
  const char *title = "readSeq (1Feb93), multi-format molbio sequence reader.\n";
  if (verbose || (!quietly && !gotinputfile)) fprintf( stderr, title);

-- per output seq:
  if (outform == kMSF) checksum= GCGchecksum(seq, seqlen, &checkall);
  else if (verbose) checksum= seqchecksum(seq, seqlen, &checkall);
  if (verbose)
    fprintf( stderr, "Sequence %d, length= %d, checksum= %X, format= %s, id= %s\n",
          whichSeq, seqlen, checksum, formatstr(format), seqidptr);

*/		
 		
	public void run()
	{
		SeqFileInfo.gWriteMask= dowritemask; //?
		if (doclassic && verbose) { verboseClassic= true; verbose= false; }
		else verboseClassic= false;
		
		BioseqReader.verbose= verbose;
		dohtml= (outname!=null && outname.indexOf("html")>=0);	

		if (doformhelp) {
			try {
			PrintStream pr;
			if (outname==null) pr=  System.out;
			else pr= new PrintStream( new FileOutputStream(outname));  
			help h= new help(false, dohtml, pr);
			if (outformat==null || !hasoutformat)
				h.formatHelp();
			else {
				h.formatDoc(BioseqFormats.getFormatId(outformat));
				h.formatDocEnd();
				}
			pr.close();
			} catch (Exception ex) {}
			return;
			}

		if (!haveInputData() && !dopipe) {
			usage(); return;
			}

		if (outformat==null) outformat= Environ.gEnv.get("format","biosequence/fasta");
		int outid= BioseqFormats.getFormatId(outformat);
		//BioseqFormat outfmt= BioseqFormats.bioseqFormat(outid);  
	
		if (outdirname!=null) {
			File dir= new File(outdirname);
			if (dir.isDirectory()) outdir= dir;
			}
		if (indirname!=null) {
			File dir= new File(indirname);
			if (dir.isDirectory()) indir= dir;
			}
		
		int forceInformat= 0;
		if (informat!=null) forceInformat= BioseqFormats.getFormatId(informat);
	
		if (innames.isEmpty() && dopipe) 
			addInputObject( System.in);

		if (!dopipe && outname==null && haveInputData())  
			outname= guessOutname(outid);

		featSubrange= null; extractRange= null;
if (bExtractRangeOkay) {
		if (extractrange!=null) try { 
			SeqRange sr= SeqRange.parse(extractrange); 
			if (sr!=null && !sr.isEmpty()) extractRange= sr; 
				Debug.println("parse extractrange="+extractrange+", extractRange="+extractRange);
			} catch (SeqRangeException sre) { extractRange= null; }
		if (featsubrange!=null) try { 
			SeqRange sr= SeqRange.parse(featsubrange); 
			if (sr!=null && !sr.isEmpty()) featSubrange= sr; 
				Debug.println("parse featsubrange="+featsubrange+", featSubrange="+featSubrange);
			} catch (SeqRangeException sre) { featSubrange= null; }
}
		
    if (verbose || dotime) {
    	message( getClass().getName() + " -- starting ");
			message("Writing to "+ (outname==null ? "Std. output" : outname));
			}

			
		try { 
			Writer outs= null, outs2= null;
			BioseqWriterIface seqwriter= null, seqwriter2= null;
			boolean iDidHeader= false;
			Object pairdoc= null;
			SeqFileInfo pairseq= null;
			String outname2= null;
			int outid2= 0;
			boolean seqfirst= false;
			Readseq	rd= new Readseq();
			URL outurl;
			rd.verboseClassic= verboseClassic;
			rd.noEmptyFiles= noEmptyFiles;
			
			if (outname==null) 
				outs= new OutputStreamWriter( System.out);
			else if ((outurl= rd.checkUrl(outname)) != null) { //!?
				URLConnection uconn= outurl.openConnection();
				outs= new OutputStreamWriter( uconn.getOutputStream());
				}
			else if (outdir!=null) 
				outs= new BufferedWriter( new FileWriter(new File(outdir, outname)));  
			else 
				outs= new BufferedWriter( new FileWriter(outname));  
			
					
			if (! (dolist || docompare)) {
				seqwriter= BioseqFormats.newWriter(outid);  
				if (seqwriter==null) 
					throw new ReadseqException("No BioseqWriter for this format: "+outformat);  
				setWriterOptions( seqwriter);
				seqwriter.setOutput( outs);
				seqwriter.writeHeader(); iDidHeader= true;
				}
					
			FastVector vitems= getItems(itemlist);	

			rd.setInDirectory(indir); // null ok
			rd.checkInList( innames, kInputStringKey); 
					//! checkInList AND setInputObject call checkInString...

			Enumeration en= innames.elements();
			while (en.hasMoreElements()) {
				String inname= rd.setInputObjectChecked( en.nextElement());
				if (inname == null) continue;				
	
				if (verbose || dotime) message("Reading from "+inname);
				
				if (forceInformat > 0) {
					// want option to use raw as default format, but not force it?
					rd.setInputFormat(forceInformat);
					}
				if (!rd.isKnownFormat()) {
					message("Unknown biosequence format for input " + inname);
				  }
				  
				else {
				
					if (forceInformat > 0) {
						// patch only for plain/raw ?! to keep them as one format w/ variations
						BioseqFormat fmt= rd.getBioseqFormat();
						fmt.setVariant(informat);
						}

					//if (verboseClassic) rd.verboseClassic();
						
					if (!exfeatures.isEmpty() || featSubrange!=null) {
						rd.setFeatureExtraction( exfeatures, featSubrange);
						}
						
				  //08apr19: why is this disabled? not working?		see Readseq:readAt() note // test enable
					if (extractRange!=null) rd.setExtractRange(extractRange);
						
					if (!rd.readInit())  
						message("Error initializing readseq for input " + inname);
						
					else if (dolist) rd.list( outs);

					else if (docompare) {
						comparePair( inname, rd,  en.nextElement(), forceInformat,  outs );
						}
											
					else if (doPairedDocNSeq) {
						BioseqFormat fmt= rd.getBioseqFormat();
						Debug.println("Pair: informat " + fmt.formatName());
						
						// FIXME - doc 'source' features should match seq.readAt(atseq) ??
						// also trap empty feature/seq input, don't write null '> 1bp\nN\n' seqwriter
						
						if (fmt.hasdoc() && !fmt.hasseq()) {
							SeqFileInfo si= rd.readAt(1);  
							pairdoc= si.seqdoc;
							if (Debug.isOn)
								Debug.println("Pair: got doc " + inname + " nfeat="+((BioseqDoc)pairdoc).features().size());
							}
						else if (fmt.hasseq()) {
							pairseq	= rd.readAt(1);
							if (Debug.isOn)
								Debug.println("Pair: got seq " + inname + " len="+pairseq.seqlen);
 							}
 							
						if (pairseq!=null && pairdoc!=null) {
							pairseq.seqdoc= pairdoc;
						  if (pairdoc instanceof BioseqDoc && ((BioseqDoc)pairdoc).getID()!=null)
						    pairseq.setSeqID(((BioseqDoc)pairdoc).getID());
							if (!iDidHeader) seqwriter.writeHeader(); 
							rd.writeSeqTo( pairseq, seqwriter, null);
							//if (seqwriter.setSeq( pairseq)) seqwriter.writeSeqRecord();
							if (!iDidHeader) seqwriter.writeTrailer();
							pairseq= null; pairdoc= null;
							}
 						}
 						
					else if (doUnpairDocNSeq) {
						boolean didwrite= false;
						BioseqFormat infmt= rd.getBioseqFormat();  // for multi-input files, assume open out only once?
						BioseqFormat outfmt= BioseqFormats.bioseqFormat(outid);  
						if ( (infmt.hasseq() && infmt.hasdoc()) 
							&& !(outfmt.hasseq() && outfmt.hasdoc())
							&& outname!=null) 
							{
							if (seqwriter2==null) {
								if (outfmt.hasseq()) {
									outid2= BioseqFormats.formatFromName("FFF"); // FlatFeatFormat; need option
									seqfirst= true;
									}
								else {
									outid2= BioseqFormats.formatFromName("fasta"); // need option
									seqfirst= false;
									}
								outname2= outname + BioseqFormats.formatSuffix(outid2);
								if (outdir!=null) 
									outs2= new BufferedWriter( new FileWriter(new File(outdir, outname2)));  
								else
									outs2= new BufferedWriter( new FileWriter(outname2));  
								seqwriter2= BioseqFormats.newWriter(outid2);  
								if (seqwriter2!=null) {
									setWriterOptions( seqwriter2);
									seqwriter2.setOutput( outs2);
									seqwriter2.writeHeader();  
								 	}
							 	}
							if (seqfirst) 
								didwrite= rd.readToPair( seqwriter, seqwriter2, !iDidHeader);
							else 
								didwrite= rd.readToPair( seqwriter2, seqwriter, !iDidHeader);
							}
							
						if (!didwrite) 
							rd.readTo( seqwriter, !iDidHeader); 
						}	
							
					else if (vitems!=null) {
						for (int i=0; i<vitems.size(); i++) {
							int iseq= ((Integer) vitems.elementAt(i)).intValue();
							SeqFileInfo si= rd.readAt( iseq); // fixed may'00
							//SeqFileInfo si= rd.nextSeq();
							rd.writeSeqTo( si, seqwriter, null);
							//if (seqwriter.setSeq( si)) seqwriter.writeSeqRecord();
							}
						}
						
					else 
						rd.readTo( seqwriter, !iDidHeader); 
					}	 
			}
			

			try {
				if (seqwriter!=null) {
					if (iDidHeader) seqwriter.writeTrailer();
					seqwriter.close(); // closes outs !?
					}
				if (seqwriter2!=null) {
					if (iDidHeader) seqwriter2.writeTrailer();
					seqwriter2.close(); // closes outs !?
					}
			 	if (outs!=null) outs.close(); 
			 	if (outs2!=null) outs.close(); 
			 	} catch (IOException e) {}
			 	
			rd.close();
			}
		catch (ReadseqException e) { 
			if (verbose) e.printStackTrace(); 
			else System.err.println(e.getMessage());
			}
		catch (IOException e) { 
			e.printStackTrace(); 
			}
		
    if (verbose || dotime) {
    	message( getClass().getName() + " -- done ");
			}
	}
	

							
	protected FastVector getItems(String list)
	{
		if (list==null || list.length()==0) return null; 
			// 1,2,3 4-7 8 10-11 -- or 10..20 ? parse as seqrange? no
		FastVector v= new FastVector();
		int len= list.length();
		boolean expand= false;
		int lastn= 1;
		for (int i= 0; i<len; i++) {
			char c= list.charAt(i);
			if (c == '-') { expand= true; continue; }
			else if (c == '.' && i<len-1 && list.charAt(i+1)=='.') { i++; expand= true; continue;  }
			else if (c < '0' || c > '9') continue;
			int e= i;
			while (e<len && Character.isDigit(list.charAt(e))) e++;
			if (e>i) try {
				Integer n= new Integer(list.substring(i,e));
				if (expand) {
					for (int k= lastn+1, ke= n.intValue()-1; k<ke; k++)
						v.addElement( new Integer(k));
					expand= false;
					}
				v.addElement(n);
				lastn= n.intValue();
				} catch (Exception ex) {}
			i= e-1; // i++ above increments
			}
		if (v.isEmpty()) return null;
		return v;
	}
	
	
	protected void setWriterOptions( BioseqWriterIface seqwriter )
	{
		OutBiobaseIntf outb= seqwriter.getOutputTranslation();
		if (degap) outb= new ToDegappedBase( degapc, outb);
		if (dolowercase) outb= new ToLowercaseBase(outb);
		else if (douppercase) outb= new ToUppercaseBase(outb);
		
    if (doaminotrans) outb= new ToAminoBase(outb);

		if (dotranslate && translates!=null && translates.length()>0) // !translates.isEmpty()
		{
			StringBuffer intr= new StringBuffer();
			StringBuffer outtr= new StringBuffer();
			//for (int i= 0, n= translates.size(); i<n; i++) 
				{
				String ft= translates; //(String) translates.elementAt(i);
				if (ft.length()>1) { 
						// parse multi-char lines also...
						// aAcCtU ... a:A c:C t:U
						// skip ' ', ',', ':' -- expect paired chars always?
					int flen= ft.length();
					char inc= 0;
					for (int j=0; j<flen; j++) {
						char c= ft.charAt(j);
						if (c <= ' ' || c == ',' || c == ':') continue;
						if (inc==0) inc= c;
						else { 
							intr.append(inc); 
							outtr.append(c);
							inc= 0; 
							}
						}
					//intr.append(ft.charAt(0)); 
					//outtr.append(ft.charAt(1)); 
					}
				}
			outb= new ToTranslatedBase( intr.toString(), outtr.toString(), outb);
			}
		if (outb!=null) seqwriter.setOutputTranslation(outb);

			// can exfeatures be overloaded to handle doc fields also?
		exfeatures.clear();
		if (hasFeatlist()) {
			String[] vals= Utils.splitString(featlist,", ");
			for (int k=0; k<vals.length; k++) exfeatures.put(vals[k], "true"); 

//?? do we want keepfeatures? -- let caller specify 'source' others
//			if (keepfeatures!=null) vals= Utils.splitString(keepfeatures,", ");
//			for (int k=0; k<vals.length; k++) exfeatures.put(vals[k], "true"); 

			}
		if (hasNofeatlist()) { 			 
			String[] vals= Utils.splitString(nofeatlist,", ");
			for (int k=0; k<vals.length; k++) exfeatures.put(vals[k], "false"); 
			}
			
		//if ( !exfeatures.isEmpty() || featSubrange!=null) {
			//seqwriter.setFeatureExtraction(exfeatures); //? or do this to Readseq ?
		//	}
			 
		       // need BioseqWriterIface add for these
		if (seqwriter instanceof BioseqWriter) { 
	  	String checksumtype= Environ.gEnv.get("checksumtype"); // 2007jun patch for swiss std CRC32
			((BioseqWriter)seqwriter).setChecksum(dochecksum,checksumtype);	
			((BioseqWriter)seqwriter).setReverseComplement(doreverse);	
			}

				// want BioseqWriterIface method to set selected pretty opts??
		if (seqwriter instanceof PrettySeqWriter) 
			((PrettySeqWriter)seqwriter).setOpts(pretty);
		else if (seqwriter instanceof PlainSeqWriter) 
			((PlainSeqWriter)seqwriter).setOpts(pretty);
		  // dec03 - allow some opts for any - seqwidth esp.
		else if (pretty.userchoice && seqwriter instanceof BioseqWriter)
				((BioseqWriter)seqwriter).setOpts(pretty);
	
			//? j02 - patch for all interleaved writers to turn off? - risky
	  if (pretty.interline == 0 && (seqwriter instanceof InterleavedSeqWriter))
			((InterleavedSeqWriter)seqwriter).setinterleaved(false);
			
	}
	
		
	final boolean  hasFeatlist() { return (featlist!=null && featlist.length()>0); }
	final boolean  hasNofeatlist() { return (nofeatlist!=null && nofeatlist.length()>0); }
	
	public void getargs(String[] sargs) 
	{	
		argv= new Args(sargs);
		getargs();
	}
	
	public void getargs() 
	{	
		if (doclassic) argv.setArgPattern("acCdi:lo:prt:vf:", false); // classic readseq
		pretty.prettyInit();
		//off dec03//pretty.userset(); // always, so we don't drop settings somewhere?

		while (argv.hasMoreElements()) {	
			argv.nextArg();
			getOneArg(); // for subclassing
			}
	}
	
	protected void handleUnknownArg()
	{
		message("Unknown argument: " + argv.argKey() + "=" + argv.argValue());
	}
	
	protected void getOneArg() 
	{	
		String argstr= argv.arg();
		String key= argv.argKey();
		String val= argv.argValue();
		boolean hasval=  argv.hasValue();
		boolean boolval= (argv.isBoolean()) ? argv.booleanValue() : true;			
		
		//? change to store these key/vals in hash/Environ.new ?
		
		
		if (!key.startsWith("C")) key= key.toLowerCase();
		
		if ("help".equals(key) || "h".equals(key)) usage();
		else if (argv.argType() == argv.kUnnamed) addInputObject(argstr);
		else if ( key.startsWith("ch") ) dochecksum= boolval; // new!
		else if ( key.startsWith("inform") ) informat= val; // new!
		else if ( key.startsWith("inter")) { 
			pretty.userset(); 
			if (argv.hasValue()) pretty.interline= argv.intValue();
			}
		else if ( key.startsWith("in") ) { if (hasval) addInputObject(val); }
			// ^^ urk - clashes w/ interline, inform ...
		else if ( key.startsWith("memory")) s_reportmemory = boolval;
		else if ( key.startsWith("time")) dotime = boolval;
		else if ( key.startsWith("formhelp") ) doformhelp= boolval; // new!
		else if ( key.startsWith("debug"))  Debug.isOn= boolval;
			
			// fuller versions of classic (1 letter) opts
		else if ( key.startsWith("classic")) doclassic = boolval;
		else if ( key.startsWith("all") ) doall= boolval; 		// all
		else if ( key.startsWith("caselo") ) dolowercase= true; //caselower
		else if ( key.startsWith("CASEUP") || key.startsWith("caseup") ) douppercase= true; //CASEUPPER
		else if ( key.startsWith("degap") ) { degap= true; if (hasval) degapc= val.charAt(0); }
		else if ( key.startsWith("format") ) { hasoutformat= true; if ( hasval) outformat= val; }// format=
		else if ( key.startsWith("item") ) itemlist= val;  // items=
		else if ( key.startsWith("list") ) dolist= boolval;   // list

		else if ( key.startsWith("compare") ) { docompare= boolval; compareval= val; }  
		else if ( key.startsWith("noempty") ) noEmptyFiles= boolval;    

		else if ( key.equals("pair-feature-seq") ) doPairedDocNSeq= boolval;  
		else if ( key.startsWith("pair") ) doPairedDocNSeq= boolval;  //?
		else if ( key.equals("unpair-feature-seq") ) doUnpairDocNSeq= boolval;  
		else if ( key.startsWith("unpair") ) doUnpairDocNSeq= boolval;   
		
		else if ( key.startsWith("indir") ) indirname= val;
		else if ( key.startsWith("outdir") ) outdirname= val;
		else if ( key.startsWith("out") ) outname= val;		// outname=
		else if ( key.startsWith("pipe") ) dopipe= boolval;		// pipe
		else if ( key.startsWith("rev") ) doreverse= boolval; // may01 add
		else if ( key.startsWith("trans") ) { //translate=TU
				//if (hasval) { dotranslate= true; translates= appends(translates,val); } 
				setTranslation( val, true);
				}
		else if ( key.startsWith("amin") ) doaminotrans= boolval; // dec03 add
			 
		else if ( key.startsWith("verb") ) verbose= boolval;		//verbose
			
		else if ( key.startsWith("feat") || key.startsWith("field")) { 
			if (hasval) { featlist= appends(featlist,val); }
			}
		else if ( key.startsWith("nofeat") || key.startsWith("nofield") ) { 
			if (hasval) { nofeatlist= appends(nofeatlist,val); } 
			}
		else if ( key.startsWith("keepfeature") ) { 
			if (hasval) { keepfeatures= appends(keepfeatures,val); } 
			}
			
				// change - need feature-subrange and global range for extracting
		else if ( key.startsWith("subrange") ) { if ( hasval) featsubrange= val; } 
		else if ( key.startsWith("extract") ) { if ( hasval) extractrange= val; } 
		
			// pretty format opts
		else if ( key.startsWith("wid")) { pretty.seqwidth= argv.intValue(); pretty.userset(); }
		else if ( key.startsWith("tab")) { pretty.tab= argv.intValue(); pretty.userset(); }
		else if ( key.startsWith("col")) { pretty.spacer= argv.intValue(); pretty.userset(); }
		else if ( key.startsWith("gap")) { pretty.baseonlynum= false; pretty.userset(); }
		else if ( key.startsWith("nameleft")) {
			pretty.nameleft= true; pretty.userset(); 
			if (argv.hasValue()) pretty.namewidth= argv.intValue();
			}
		else if ( key.startsWith("nameright")) {
			pretty.nameright= true; pretty.userset(); 
			if (argv.hasValue()) pretty.namewidth= argv.intValue();
			}
		else if ( key.startsWith("nametop")) { pretty.nametop= true;  pretty.userset();	}
		else if ( key.startsWith("numleft")) {
			pretty.numleft= true; pretty.userset();
			if (argv.hasValue()) pretty.numwidth= argv.intValue();
			}
		else if ( key.startsWith("numright")) {
			pretty.numright= true; pretty.userset();
			if (argv.hasValue()) pretty.numwidth= argv.intValue();
			}
		else if ( key.startsWith("numtop")) { pretty.numtop= true; 	 pretty.userset(); }
		else if ( key.startsWith("numbot")) { pretty.numbot= true;  pretty.userset();	}
		else if ( key.startsWith("match")) { 
			pretty.domatch= true; pretty.userset();
			if (argv.hasValue()) pretty.matchchar= val.charAt(0);
			}
		else if ( key.startsWith("inter")) { 
			pretty.userset(); 
			if (argv.hasValue()) pretty.interline= argv.intValue();
			}
		
			// classic readseq, single letter options -- turn this off for .cgi, others?
		else if (doclassic) {
			if ( key.startsWith("a") ) doall= true; 		// all
			else if ( key.startsWith("c") ) dolowercase= true; //caselower
			else if ( key.startsWith("C") ) douppercase= true; //CASEUPPER
			else if ( key.startsWith("d") ) { degap= true; if (argv.hasValue()) degapc= val.charAt(0); }
			else if ( key.startsWith("f") ) { if ( hasval) outformat= val; }// format=
			else if ( key.startsWith("i") ) itemlist= val;  // items=
			else if ( key.startsWith("l") ) dolist= true;   // list
			else if ( key.startsWith("o") ) outname= val;		// outname=
			else if ( key.startsWith("p") ) dopipe= true;		// pipe
			else if ( key.startsWith("r") ) doreverse= true;  
			else if ( key.startsWith("t") ) {  //translate=TU
				//if (hasval) { dotranslate= true; translates= appends(translates,val); } 
				setTranslation( val, true);
				}
			else if ( key.startsWith("v") ) verbose= true;		//verbose
			}
		
		else {
			handleUnknownArg(); //System.err.println("Unknown argument: " + key + "=" + val);
			}
	}
	
	final String appends(String val, String addval) {
		if (val==null) return addval; 
		else if (addval==null) return val;
		else return val + " " + addval;
		}
		
	public void setTranslation(String val, boolean append)
	{
		if (val!=null && val.length()>0) {
			dotranslate= true;
			if (append) translates= appends(translates,val);
			else translates= val;
			}
		else if (!append) dotranslate= false;
	}
	
	public static void usage() { usage(System.out); }
	public static void usage(PrintStream out)
	{
			// add classic readseq options ...
			// what of orgxml.jar, ibm-xml.jar ?? - include in rseq.jar?
		out.println();
    out.println("  " +Readseq.version);
    String helps= AppResources.global.getData( kReadseqcHelp);
    if (helps!=null) {
    	String lf = System.getProperty("line.separator");
    	int hat= helps.indexOf("<HTML");
    	if (hat>=0) {
    		int at= helps.indexOf(lf); if (at<0) at= 0; else at++;
    		int e= helps.indexOf("</HTML", at);
    		if (e>at) e= helps.lastIndexOf(lf, e);
    		if (e<0) e= helps.length();
    		helps= helps.substring(at,e);
    		}
    	out.println( helps);
    	}
    else {	
			out.println("  Read & reformat biosequences, command-line interface");
			out.println("  Usage: jre -cp readseq.jar " + run.class.getName() + " [options] input-file");
			out.println();
			out.println("  Options: [help not available]");
			out.println();
			}
		
		out.println("  Known biosequence formats:");
		out.println( BioseqFormats.getInfo( 0, "space-header"));
		for (int i=1; i<=BioseqFormats.nFormats(); i++) {
			out.println( BioseqFormats.getInfo(i, "space-command-line"));
			}
		out.println("   (Int'leaf = interleaved format; Features = documentation/features are parsed)");
		out.println();
	}

	private static void reportMemory(String lab, Runtime rt) {
    long total = rt.totalMemory();
    long free = rt.freeMemory();
    long using = total - free;
    System.err.print( lab);
    System.err.print('\t');
    System.err.println( free+"/"+total+": use "+using+" bytes");
 		System.err.flush();
    }
	
	
}

