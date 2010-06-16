//app.java
//split4javac// readseqmain.java date=13-Jun-2003

/** 
	readseqmain.java
	-- IBM trick to put real main in package and wrapper main w/o package  
	@see iubio.readseq.run
*/

//split4javac// readseqmain.java line=17
public class app  {	
	public static void main(String[] args) { new iubio.readseq.app(args); }
	}

/**  @see iubio.readseq.cgi */
