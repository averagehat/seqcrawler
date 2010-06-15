package iubio.readseq;

class TestGcgBase extends TestBiobase
{

	public int isSeqChar(int c) {
		//if (Character.isSpace((char)c) || Character.isDigit((char)c)) return 0;
		if (c<=' ' || (c >= '0' && c <= '9')) return 0;
		else {
	    if (c == '.') return '-';  //do the indel translate 
	    else return c;
	    }
		}		
}