package iubio.readseq;

import java.io.IOException;

//ReadseqException doesnt need to be public, but dang javac splitter wont do unless told otherwise

public class ReadseqException extends IOException
{
	public ReadseqException() { 
		super(); 
		}
	public ReadseqException(String err) { 
		super(err); 
		}
}