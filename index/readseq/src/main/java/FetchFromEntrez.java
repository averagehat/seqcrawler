// FetchFromEntrez.java
// a simple example of a biocomputing network tool written in java
// d.gilbert, mar'98
// to compile:  javac  FetchFromEntrez.java
// to run    :  java FetchFromEntrez.java  id id id ...
// test ids  :  dna gb_U30153 gb_M81833 gb_L13173 gb_AF011224 
// test ids  :  prot gi_304809 gi_2286196 
   

import java.io.*;
import java.net.*;
import java.util.*;

public class FetchFromEntrez
{
  static boolean useStdout= false,
 		useBatchEntrez= false, // nph-batch doesn't like this posting yet
  	useSingleBatch= true;  // but we can batch requests to working service
  public static String entrezUrl=
    "http://www.ncbi.nlm.nih.gov/htbin-post/Entrez/query?form=6";
  public static String entrezBatchUrl=
		"http://www.ncbi.nlm.nih.gov/cgi-bin/Entrez/nph-batch/result"; 
  public static String usage=
    "This java program extracts dna or protein sequences using gid numbers.\n"+
    "from a perl script by Bill Pearson, converted to java by d. gilbert\n\n"+
    "Usage:  java FetchFromEntrez [options] gid gid gid ...\n"+
    "options:\n"+
    " -               - write to standard output\n"+
    " output=somefile - output filename\n"+
    " single|batch    - do single or batch request\n"+
    " dna|protein     - choose dna or protein output\n"+
    " html|text       - html or plain text\n"+
    " fasta|genbank   - sequence format\n";
    
  public static void main(String[] args) 
  {
    if (args.length==0) 
      System.out.println(usage);
    else {
      boolean html= false;
      String db= "n", form= "f",  outname= "entrez_fetch";
      for (int i=0 ; i<args.length; i++) {
        if (args[i].startsWith("-")) { useStdout= true; args[i]= null; }
        else if (args[i].startsWith("output=")) { outname= args[i].substring(7); args[i]= null; }
        else if (args[i].startsWith("dna")) { db= "n"; args[i]= null; }
        else if (args[i].startsWith("prot")) { db= "p"; args[i]= null; }
        else if (args[i].startsWith("fasta")) { form= "f"; args[i]= null; }
        else if (args[i].startsWith("genbank")) { form= "g"; args[i]= null; }
        else if (args[i].startsWith("html")) { html= true; args[i]= null; }
        else if (args[i].startsWith("text")) { html= false; args[i]= null; }
        else if (args[i].startsWith("batch")) { useSingleBatch= true; args[i]= null; }
        else if (args[i].startsWith("single")) { useSingleBatch= false; args[i]= null; }
        else if (args[i].startsWith("testbatch")) { useBatchEntrez= true; args[i]= null; }
        }
      if (html) outname += ".html";
   		if (useBatchEntrez) batchEntrez(args, outname, html, db, form);
   		else singleEntrez(args, outname, html, db, form);
    	}
  }

	static PrintStream openout(String outname) 
	{
    if (useStdout) return System.out;
    else {
      try { return new PrintStream( new FileOutputStream( outname, true)); }  
      catch (IOException e) { 
        System.err.println("Can't write to "+outname+", err=" + e.getMessage()); 
        System.exit(1); return null;
        }
      }
  }
  
	static void singleEntrez(String[] args, String outname, boolean html, String db, String form)
	{
   	String url= entrezUrl + (html ? "" : "&html=no") + "&db=" + db + "&Dopt=" + form + "&uid=";
 		String startkey= null;
 		if (!html) { startkey= (form.equals("f") ? ">" : "LOCUS"); }
   	if (useSingleBatch) {
			String gids= "";
			for (int i=0 ; i<args.length; i++) {
	      if (args[i]==null) continue;
	      gids += args[i].replace('_','|') + ",";
	      }
	    callEntrez( url, gids, openout(outname), startkey);    
   		}
   	else for (int i=0 ; i<args.length; i++) {
      if (args[i]==null) continue;
      String gid= args[i];
      gid= gid.replace('_','|');    
    	outname= args[i].toLowerCase();
	    callEntrez( url, gid, openout(outname), startkey);    
			}
	}
		
	static void callEntrez(String url, String gid, PrintStream pr, String startKey)
	{
    boolean haveStart= (startKey == null);
    boolean haveGt = false;
    try {
      DataInputStream din= new DataInputStream(new URL(url + gid).openStream());
      while (true) { 
        String s= din.readLine();
        if (s==null) break;
        else if (s.startsWith("ERROR") || s.startsWith("*** No Documents Found")) {
          System.err.println("ERROR - "+gid+" not found: " + s); 
          break; 
          }
        else if (startKey!=null && s.startsWith(startKey)) haveGt= true;
        if (haveGt) pr.println(s);
        } 
      pr.close();
      }
    catch (Exception e) { System.err.println("Error reading from "+url + gid); }
	}


			// NOYE: This one doesn't work yet -- NCBI batch server returns Proxy error msg
	static void batchEntrez(String[] args, String outname, boolean html, String db, String form)
	{
		try {
    	PrintStream pr;
    	HttpURLConnection.setFollowRedirects(true);
    	URLConnection hconn= new URL(entrezBatchUrl).openConnection();
			if (hconn instanceof HttpURLConnection) 
				((HttpURLConnection)hconn).setRequestMethod("POST");
			hconn.setRequestProperty("Content-type","multipart/form-data;boundary="+mimebound);
			hconn.setDoOutput(true);
			hconn.setDoInput(true);
				// send form data, including "file" upload
    	pr= new PrintStream( hconn.getOutputStream());
			printfield(pr,"SEQ_TYPE", (db.equals("n") ? "Nucleotide" : "Protein"));
			printfield(pr,"DUMP_TYPE","FILE");
			printfield(pr,"FORMAT", (form.equals("f") ? "FASTA" : "GenBank/GenPept"));
			printfield(pr,"HTML", (html ? "HTML": ""));
			printfield(pr,"ORGNAME","");
			printfield(pr,"LIST_ORG","(None)");
			printfield(pr,"REQUEST_TYPE","FILESUBMIT");
			printfield(pr,"USERFILE", null, "args");
  		for (int i=0 ; i<args.length; i++) {
        if (args[i]==null) continue;
      	pr.println( args[i].replace('_','|'));    
				}
			printfieldend(pr);
			pr.close();
			
    	DataInputStream din= null;
    	for (int i=0; i<10; i++) try {
    		Thread.sleep(50);
	    	try { din= new DataInputStream( hconn.getInputStream()); break; } 
	    	catch (IOException e) {}
	    	}
    	catch (InterruptedException e) {}
    	
			if (hconn instanceof HttpURLConnection) 
				System.err.println("Server msg: "+((HttpURLConnection)hconn).getResponseMessage());
			//Server msg: Proxy Error ?? -- why isn't server accepting this post ?
			//java.net.SocketException: Socket closed
			
      if (din!=null) {
	      pr= openout(outname);
	      while (true) { 
	        String s= din.readLine();
	        if (s==null) break;
	        pr.println(s);
	        }
	      pr.close();
	      }
			}
    catch (Exception e) {
      System.err.println("Error getting data from " + entrezBatchUrl); 
   		e.printStackTrace();
     }
	}
	
		// for multipart form post
	static String mimebound= "-----------------------------241862694713319";
  static void printfieldend(PrintStream pr) { pr.println(mimebound+"--"); }
	static final void printfield(PrintStream pr, String name, String value) { 
			printfield(pr,name,value,null); }
	static void printfield(PrintStream pr, String name, String value, String fname) {
		pr.println(mimebound);
		pr.print("Content-Disposition: form-data; name=\""+name+"\"");
		if (fname!=null) pr.print("; filename=\""+fname+"\"");
		pr.println();
		pr.println();
		if (value!=null) pr.println(value);
		}					

}


