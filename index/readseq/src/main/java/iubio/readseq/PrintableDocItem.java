package iubio.readseq;

import java.io.PrintWriter;

public interface PrintableDocItem
{
	public void print( PrintWriter pr, BioseqDocImpl doc, String label, String value);
}