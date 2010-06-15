// FetchFromSRS.java
// d.gilbert, jun'98 - example java command-line app to fetch data from an srs server
//
// to use from a unix command-line
// javac FetchFromSRS.java
// java FetchFromSRS 'esterase&drosophila' > srs.html; lynx srs.html
// java FetchFromSRS host=srs.ebi.ac.uk:5000 lib=embl lib=emblnew field=all 'melanogaster&esterase' > emblsrs.html

import java.io.*;
import java.util.*;
import java.net.*;

class UsageException extends Exception {}

public class FetchFromSRS
{
  public final static int v5= 0, v4 = 1;
  public static int gSRSversion = v5;
  public static String 
    gHost  = "iubio.bio.indiana.edu",
    gLibs  = "genbank genbanknew gbest",
    gField = "all",
    gOptions= "-f des", // describe entries
    gQuery = "esterase*";
  public static String[] gPath=  { "/srs5bin/cgi-bin/wgetz", "/srs/srsc" };
  String outname, libs; 

  public static void main(String[] args) 
  {         
    FetchFromSRS fsrs= new FetchFromSRS();
    try { fsrs.run(args); }
    catch (UsageException e) {}
    catch (Exception e) { e.printStackTrace(); }
  }
  
  public FetchFromSRS() {}
  
  public void run(String[] args) throws Exception
  {
    readArgs(args);
    String url= "http://" + gHost + gPath[gSRSversion] ;
    PrintStream pr= System.out;
    if (outname!=null) try { 
      pr= new PrintStream( new FileOutputStream(outname)); 
      }
    catch (IOException e) {}
    // make sure html contains base url
    pr.println("<HTML><HEAD><BASE HREF=\""+url+"\"></HEAD><BODY>");
    fetchFromUrl( pr, url + getQuery());
  }
  
  protected void readArgs(String[] args) throws Exception
  {
    if (args==null || args.length==0) usage();
    for (int i=0; i<args.length; i++) {
      String arg= args[i], val;
      int at= arg.indexOf('=');
      if (at>0) val= arg.substring(at+1); else val= null;
      if (arg.startsWith("out=")) outname= val;
      else if (arg.startsWith("host=")) gHost = val;
      else if (arg.startsWith("vers=")) {
        if (val.indexOf('4')>0) gSRSversion= v4; else gSRSversion= v5;
        }
      else if (arg.startsWith("field=")) gField= val;
      else if (arg.startsWith("lib=")) 
      	libs = (libs==null) ? val : libs + " " + val;
      else if (arg.startsWith("opts=")) gOptions= val;
      else if (val==null) gQuery= arg; // assume query if no =
      else usage();
      }
  }

  public void usage() throws UsageException
  {
    System.out.println( this.getClass().getName() + " [arguments] query-term ");
    System.out.println("arguments:");
    System.out.println( "  host=SRSserver[:port], default=" + gHost);
    System.out.println( "  lib=data-library, default=" + gLibs);
    System.out.println( "  field=data-field, default=" + gField);
    System.out.println( "  opts=srs-options, default=" + gOptions);
    System.out.println( "  vers=5 or 4 (srs server version)");
    System.out.println( "  out=output.file");
    throw new UsageException(); 
  }
  
  protected void fetchFromUrl(PrintStream pr, String url)
  {
    try {
      DataInputStream din= new DataInputStream(new URL(url).openStream());
      String s= din.readLine();
      while (s!=null) {
        // process input line ...
        pr.println(s);
        s= din.readLine();
        }
      }
    catch (Exception e) { 
    	System.err.println("fetch from '"+url+"' err: " + e.getMessage()); }
  }
  
  protected String getQuery()
  {
    String q;
    if (gQuery.indexOf(']')>0) q= gQuery;
    else {
      if (libs==null) libs= gLibs;
      q= "[" + encodeLibs(libs) + "-" + gField + ":" + gQuery + "]";
      }
    q += " " + gOptions; // add any SRS options...
  	return "?" + URLEncoder.encode(q);
  }
  
  protected String encodeLibs(String libs)
  {
    // convert more than one lib to this messy form to run the hurdles of
    // netscrape->httpd->unix-cmdline->wgetz
    // libs={genbank_SP_genbanknew_SP_gbest}  
    String elibs= null;
    int nlib= 0;
    StringTokenizer st= new StringTokenizer(libs, " ");
    while (st.hasMoreTokens()) {
    	nlib++;
      if (elibs==null) elibs= st.nextToken();
      else elibs +=  "_SP_" + st.nextToken(); // hidden SRS syntax for a space
      }
    if (nlib>1) elibs = "libs={" + elibs + "}";
    return elibs;
  }

}