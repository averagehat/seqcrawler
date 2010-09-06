//iubio/readseq/test.java
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


//split4javac// iubio/readseq/readseqrun.java line=1094
public class test extends run
{
	public static String testcmds = "rez/test.properties";
		
	public static void main(String[] args)  { 
		test t= new test();
		try { t.test(args, null); }
		catch (Exception e) { e.printStackTrace(); }
		}
		
	public test()  { super(); }
	

	public static void testusage(PrintStream out)
	{
		out.println();
    out.println("  Test Readseq using a built-in data set");
		out.println("    Usage: java -cp readseq.jar test" );
	}
	

	public void test(String[] args, PrintStream out)  throws Exception {
		if (out==null) out= System.out;

		getargs( args);		
		//if (Debug.isOn) run(); 

		PrintStream pr;
		if (outname==null) pr= out;
		else pr= new PrintStream( new FileOutputStream(outname));  
					
 		pr.println("Testing "+  Readseq.version);

		File testdir= new File( Environ.gEnv.get("testdir","testrs") );
		if (testdir.isDirectory() || testdir.mkdir()) {
			pr.println("Writing test files to "+testdir);
			Properties sp= System.getProperties();
			sp.put("user.dir", testdir.toString());
			outdir= testdir;
			indir= testdir; //!?
			//outdirname= testdir.toString(); //
			}
		

  	String pname= System.getProperty( "test", testcmds); // Environ.gEnv.get()
		
  	DataInputStream cmdin= new DataInputStream(  AppResources.global.getStream(pname) );
  	int nt= 0; String cmd;
  	while ((cmd= cmdin.readLine()) != null) {
  		cmd= cmd.trim();
  		if (cmd.startsWith("echo=")) pr.println("## "+ cmd.substring("echo=".length())); 
  		//else if (cmd.startsWith("message=")) pr.println("## "+ cmd.substring("message=".length())); 

  		else if (cmd.startsWith("compare=")) { // use parameters= compare=1
  			pr.println("## "+ cmd.substring("compare=".length())); //?
  			}

  		else if (cmd.startsWith("parameters=")) {
  			nt++;
  			cmd= cmd.substring("parameters=".length());
  			pr.println(".......... "+ nt + ". readseq("+cmd+")..........");
  			String[] targs= Utils.splitString( cmd, " \t");
    	
		    long tstart = System.currentTimeMillis();
    		initrun();  
				getargs( targs);			
				run();
				
	   		long telapsed = System.currentTimeMillis() - tstart;
				pr.println(".......... "+ nt + ". readseq done, time=" + telapsed+"...............");				
  			pr.println();
  			}
  		}
  					 
		}

}


/*  classic readseq args
readSeq (1Feb93), multi-format molbio sequence reader.
usage: readseq [-options] in.seq > out.seq
 options
    -a[ll]         select All sequences
    -c[aselower]   change to lower case
    -C[ASEUPPER]   change to UPPER CASE
    -degap[=-]     remove gap symbols
    -i[tem=2,3,4]  select Item number(s) from several
    -l[ist]        List sequences only
    -o[utput=]out.seq  redirect Output
    -p[ipe]        Pipe (command line, <stdin, >stdout)
    -r[everse]     change to Reverse-complement
    -t[ranslate=]io translate input symbol [i] to output symbol [o]
                    use several -tio to translate several symbols
    -v[erbose]     Verbose progress
    -f[ormat=]#    Format number for output,  or
    -f[ormat=]Name Format name for output:
         1. IG/Stanford           10. Olsen (in-only)
         2. GenBank/GB            11. Phylip3.2
         3. NBRF                  12. Phylip
         4. EMBL                  13. Plain/Raw
         5. GCG                   14. PIR/CODATA
         6. DNAStrider            15. MSF
         7. Fitch                 16. ASN.1
         8. Pearson/Fasta         17. PAUP/NEXUS
         9. Zuker (in-only)       18. Pretty (out-only)

   Pretty format options:
    -wid[th]=#            sequence line width
    -tab=#                left indent
    -col[space]=#         column space within sequence line on output
    -gap[count]           count gap chars in sequence numbers
    -nameleft, -nameright[=#]   name on left/right side [=max width]
    -nametop              name at top/bottom
    -numleft, -numright   seq index on left/right side
    -numtop, -numbot      index on top/bottom
    -match[=.]            use match base for 2..n species
    -inter[line=#]        blank line(s) between sequence blocks
*/
