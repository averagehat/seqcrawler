package iubio.readseq;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import flybase.AppResources;
import flybase.Environ;
import flybase.Utils;



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
