//iubio/readseq/ScfTraceFormat.java
//split4javac// iubio/readseq/ScfTraceFormat.java date=24-May-2001

// iubio.readseq.ScfTraceFormat.java -- was ScfTraceData.java
// d.g.gilbert, 1999




package iubio.readseq;


import java.io.*;
import java.util.Hashtable;

import flybase.Debug;
import flybase.OpenString;
import flybase.FastHashtable;

import iubio.bioseq.Bioseq;


//split4javac// iubio/readseq/ScfTraceFormat.java line=20
public class ScfTraceFormat extends BioseqFormat
{		
	public String formatName() { return "SCF";  }  
	public String formatSuffix() { return ".scf"; }  
	public String contentType() { return "biosequence/scf"; }  

	public boolean canread() { return true; }    
	public boolean canwrite() { return false; }   
	public boolean interleaved() { return false; }  
	public boolean needsamelength() { return false; }  

	public BioseqReaderIface newReader() { return new ScfTraceReader(); }
	//public BioseqWriterIface newWriter() { return new ScfTraceWriter(); }		

  //private static final long SCF_MAGIC = (((((long)'.'<<8)+(long)'s'<<8)+(long)'c'<<8)+(long)'f');
		
	public boolean formatTestLine(OpenString line, int atline, int skiplines) 
	{
		if (line.startsWith(".scf")) { //? atline==0 && 
      formatLikelihood= 95;  
 			recordStartline= atline;
      return true; 
      }
    else
    	return false;
	}

}


/*
			// package to pass via SeqFileInfo.extradata object
public class TraceData
{
	public int samples, numbases, bases_left_clip, bases_right_clip;
	public short data[][]; 	// trace data, [4][0..samples]
	public short max, qualmax; // trace data maxs
	byte bases[];						// called bases -- [0..samples] or [0..numbases]<<
	int peaks[];						// where bases where called [0..numbases]
	short prob_A[],prob_C[],prob_G[],prob_T[]; // 0..numbases
	short quality[]; 				// [0..samples]
	Hashtable commentHash;	// hash of String
}

public class ScfTraceData extends TraceData
{
	public int magic_number, samples,samples_offset,numbases,bases_left_clip,bases_right_clip,
						 bases_offset, sample_size, code_set,private_size,private_offset;
	char version[] = new char[4];
	int spare[] = new int[18];
	short basespare[][];
	char private_data[];
	short p_sample;
	byte  p_sample1;
	int leftqual,rightqual,leftvec,rightvec,leftqualbase,leftvecbase,rightqualbase,rightvecbase,insertpos;
}
*/


/**
 from Michael Zody's (mczody@genome.wi.mit.edu) TraceViewer.java
 http://www.genome.wi.mit.edu/~mczody/java/TView/

CLASS TraceData (extends Object)
********************************
TraceData contains information read in from an scf type file. TraceData objects
are used to pass data from this file to TracePlot objects to display traces
and base calls in a Trev-like fashion.
*/

//public 
class ScfTraceReader 
	//extends  BioseqReader  //? or do Iface?
	implements BioseqReaderIface
{
	public static boolean verbose;
	final static boolean useBioseqBytes= true;

	protected int			formatId, seqlen, seqoffset, nseq, err, choice, atseq;
	protected String 	idword= SeqFileInfo.gBlankSeqid, seqid= SeqFileInfo.gBlankSeqid;

	//private Bioseq	seq; 	// change to Object, or some more flexible class
	private byte[]  bases; // local storage - faster than Bioseq
	protected Object seqdoc;
	
	private boolean fEof= false;
	//private int lastc= -1;
	//private int nbuf= 0;
	private Reader fIns; 		//  this fails with char reader !!!!!!!!!!!!!!!
	private	InputStream fByteIns; // hack, to test byte stream
	//private OpenString osbuf;
	//private char[] osval;
	//private byte[] bytebuf;
	private int oslinei, oslen;
	private final int kMaxbuf= 8192;  


	public ScfTraceReader() { }
		
	public ScfTraceReader(Reader ins) {
		setInput(ins);
		}


	public static MessageApp messageapp= null;
	
	public static void message(String s) {
		// need opt to print to app text component
		if (messageapp!=null) messageapp.infomessage(s); else
		System.err.println(s);
		}



		// 
		// BioseqReaderIface
		//
			
	public int formatID() { return formatId; }
	public void setFormatID(int id) { formatId= id; } //? Readseq sets id?



		/** set input stream */
	public void setInput( Reader ins)
	{
		this.fIns= ins; 
		fEof= false; 
		setReaderBuf(fIns);
	}

		/** end of sequence input stream was reached */
	public boolean endOfFile() { return fEof; }
	
		/** reset input stream for re-read (interleaved formats) */
	public void reset() 
	{
		//nbuf= 0; 
		seqlen= nseq= atseq= err= 0; //seqlencount= atseq= 0;
		//resetSeq() //?
		//ungetline= allDone= false;
		fEof= false; 
		if (fIns!=null) try {
			fIns.reset();
			fEof= !fIns.ready();
			setReaderBuf(fIns);  
			}
		catch (IOException ex) { 
			Debug.println( getClass().getName() + ".reset() err=" + ex.getMessage());
			}
	}

	void setReaderBuf(Reader ins)
	{
		oslen= oslinei= 0;  
		if (ins instanceof RsInput) fByteIns= ((RsInput)ins).getByteStream(); // may be null
		else fByteIns= null;
	}
	
		/** reset sequence storage for new sequence */
	public void resetSeq()
	{
		seqlen=  0; //seqlencount=
		//allDone= false;
		idword= SeqFileInfo.gBlankSeqid;
		seqid= SeqFileInfo.gBlankSeqid;
	}

		/** select which sequence to read 
			* @param seqchoice - index from 1 of sequence in stream
			*/
	public void setChoice(int seqchoice)
	{
		choice= seqchoice; 
	}
		
		/** get total number of sequences read <p>
			* may not be valid till all data read, 
			* or return number of current sequence after doRead() 
			*/
	public int getNseq() { return nseq; }
	
	
		/** copy last read sequence to storage record */
	public void copyto( SeqFileInfo si)
	{
		if (si==null) return;
		if (si.err==0) si.err= err;
		si.seqlen= seqlen;
		if (si.err==0 && seqlen>0) { // && seq!=null
			si.atseq= atseq; //<< counted to current sequence in file
			if (atseq>si.nseq) si.nseq= atseq;
			si.seqid= seqid;
			si.seqdoc= seqdoc;
			si.checkSeqID(); // make this part of a si.setId(seqid);
			//? si.offset= seqoffset; // will this bugger up output - fix
			si.seq= new Bioseq( bases, 0, seqlen); //, useBioseqBytes
			}		
	}

	
		/** skip past non-sequence info at top of stream <p>
			* @param skiplines - presumed number of lines to skip past
			*/
	public void skipPastHeader(int skiplines)
	{
		//?? noop for Scf
	}

		/** read next sequence to internal data
			* @see setChoice()
			*/
	public void doRead() throws IOException
	{
		nseq= 0; //?
		if (fIns==null) throw new FileNotFoundException(); //err= Readseq.eFileNotFound;
		getOsBuf();
		seqlen= getData();
		//seqlen= samples;
		nseq++;
		if (seqdoc==null) seqdoc= new GenbankDoc( seqid.toString()); //? to allow adding features?
	}
	
		/** reads all of sequence entries in stream, writing to writer <p>
			* for piping from one format to another 
			*/
	public void readTo( BioseqWriterIface writer, int skipHeaderLines)  throws IOException
	{
		int skiplines= skipHeaderLines;
	 	//this.setSkipDocs( ! writer.wantsDocument());
		boolean more= true;
		int atseqNum= 1; // must start w/ 1 not 0 !!
		for ( ; more; atseqNum++) {  
			this.skipPastHeader( skiplines);  skiplines= 0;
			SeqFileInfo sfi= this.readOne(atseqNum);
			if (sfi == null) more= false;
			else {
				if (Debug.isOn || verbose) 
					message("read "+atseqNum+" id=" + sfi.seqid + " seqlen=" + sfi.seqlen);
				if (writer.setSeq( sfi)) writer.writeSeqRecord();
				}
			}
	}

		/** read one sequence to SeqFileInfo structure */
	public SeqFileInfo readOne( int whichEntry) throws IOException
	{
		this.resetSeq();
    if ( this.endOfFile() ) return null; 
		this.setChoice( whichEntry); 
		
		this.doRead(); 

		SeqFileInfo sfi= new SeqFileInfo();
		this.copyto( sfi);
		sfi.nseq= this.getNseq(); // or is this atseq?
   	return sfi;	
	}


	private void getOsBuf()  throws IOException 
	{
		/*
		if (fByteIns!=null) return;
		
		int offs, len, oldlen;
		if (osval==null) {
			offs= 0; 
			len= kMaxbuf;
			oldlen= len;
			osval= new char[len];
			bytebuf= new byte[len]; // hack test
			//osbuf= new OpenString(osval);
			oslinei= 0;
			oslen= 0;
			}
		else {
			offs= oslen - oslinei; // unused remainder
			len= kMaxbuf - offs;
			oldlen= oslen; //osbuf.length();
			if (offs>0) {
				System.arraycopy( osval, oslinei, osval, 0, offs); // shift down
				if (fByteIns!=null) System.arraycopy( bytebuf, oslinei, bytebuf, 0, offs); // shift down
				}
			oslinei= 0;
			oslen= offs;
			}

		int rlen;
		if (fByteIns!=null) {// hack test byte reading
			rlen= fByteIns.read( bytebuf, offs, len);  
			Debug.println("fByteIns.read "+ rlen);
			}
		else
			rlen= fIns.read( osval, offs, len);  
		
		//Debug.println("getOsBuf("+offs+","+rlen+")");
		if (rlen<0) { if (oslen<=0) fEof= true; } 
		else oslen += rlen;
		*/
	}

	private final int readByte() throws IOException 
	{
		if (fByteIns!=null) { // debug test
			int b= fByteIns.read();
			if (b<0) { fEof= true; throw new EOFException(); }
			return b;
			}
 
				/// char reader is no good !!!! - why not ?????????
		int b= fIns.read();
		if (b<0) { fEof= true; throw new EOFException(); }
		return (byte) b;
		
		/*
		if ( oslinei>=oslen ) {
			getOsBuf(); //osbuf==null ||
			if ( oslinei>=oslen ) throw new EOFException(); //return -1;
			}
		if (fByteIns!=null) return bytebuf[oslinei++]; // why does this fail???
		return (byte) osval[oslinei++];
		*/
	}
  
  private final int readUint4() throws IOException {
    int ch1= readByte(); // fIns.read(); // << fails - why?
    int ch2= readByte();
    int ch3= readByte();
    int ch4= readByte();
		//if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
 		}
  private final int readUint2() throws IOException {
    int ch1= readByte();
    int ch2= readByte();
		//if ((ch1 | ch2) < 0) throw new EOFException();
		return (ch1 << 8) + (ch2 << 0);
 		}
  private final short readSint2s() throws IOException {
    int ch1= readByte();
    int ch2= readByte();
		//if ((ch1 | ch2) < 0) throw new EOFException();
		return (short)((ch1 << 8) + (ch2 << 0));
  	}
  private final short readUint1() throws IOException {
    return (short) readByte();
  	}  


	private static final long SCF_MAGIC = (((((long)'.'<<8)+(long)'s'<<8)+(long)'c'<<8)+(long)'f');
	public static final int NONE = 0;
	public static final int BASES = 1;
	public static final int LEFTVEC = 2;
	public static final int LEFTQUAL = 3;
	public static final int RIGHTQUAL = 4;
	public static final int RIGHTVEC = 5;
	public short data[][];
	public byte data1[][]; 	// only need in getData()
	public int magic_number, samples,samples_offset,numbases,bases_left_clip,bases_right_clip,
						 bases_offset,comments_size,comments_offset,sample_size,code_set,private_size,private_offset;
	public short max,qualmax;
	int spare[] = new int[18];	// only need in getData()
	char version[] = new char[4];
	//char bases[];
	//char rawcomment[];
	char private_data[];// only need in getData()?
	public int peaks[];
	public short prob_A[],prob_C[],prob_G[],prob_T[];
	public short quality[];
	short basespare[][];// only need in getData()?
	short p_sample;
	byte  p_sample1;
	public Hashtable commentHash;
	int leftqual,rightqual,leftvec,rightvec,leftqualbase,leftvecbase,rightqualbase,rightvecbase,insertpos;


	protected int getData() throws IOException 
	{
		int i,j;
		Debug.println("ScfTraceReader.getData");
		magic_number = readUint4();
		if (magic_number != SCF_MAGIC)
			throw new NotScfException("Input is not in SCF format.");
		samples = readUint4();
		Debug.println("samples="+samples);
		samples_offset = readUint4();
		numbases = readUint4();
		Debug.println("numbases="+numbases);
		bases_left_clip = readUint4();
		bases_right_clip = readUint4();
		bases_offset = readUint4();
		comments_size = readUint4();
		comments_offset = readUint4();
		for (i=0;i<4;i++) version[i]= (char)readByte();
		Debug.println("version="+version[0]);
		 
		sample_size = readUint4();
		Debug.println("sample_size="+sample_size);
		code_set = readUint4();
		private_size = readUint4();
		private_offset = readUint4();
		for (i=0;i<18;i++) spare[i] = readUint4();
	 
		if (sample_size == 1 && version[0] == '3') 
			data1 = new byte[4][samples];
		data = new short[4][samples];
		if (version[0] == '2' || version[0] == '1') {
			if (sample_size == 1) {
				for (i=0;i<samples;i++) {
					for (j=0;j<4;j++) {
						data[j][i] = readUint1();
					}
				}
			}
			else if (sample_size == 2) {
				for (i=0;i<samples;i++) {
					for (j=0;j<4;j++) data[j][i]= readSint2s();
				}
			}
		}
		else {
			if (sample_size == 1) {
				for (j=0;j<4;j++) {
					for (i=0;i<samples;i++) data1[j][i]= (byte) readByte();
				}
				for (j=0;j<4;j++) {
					p_sample1 = 0;
					for (i=0;i<samples;i++) {
						data1[j][i] += p_sample1;
						p_sample1 = data1[j][i];
					}
					
					p_sample1 = 0;
					for (i=0;i<samples;i++) {
						data1[j][i] += p_sample1;
						p_sample1 = data1[j][i];
						data[j][i] = (short) data1[j][i];
						if (data[j][i] < 0) data[j][i] += 256;
					}
				}
			}
			else if (sample_size == 2) {
				for (j=0;j<4;j++) {
					for (i=0;i<samples;i++) data[j][i]= readSint2s();
				}
				for (j=0;j<4;j++) {
					p_sample = 0;
					for (i=0;i<samples;i++) {
						data[j][i] = (short) (data[j][i] + p_sample);
						p_sample = data[j][i];
					}
					p_sample = 0;
					for (i=0;i<samples;i++) {
						data[j][i] = (short) (data[j][i] + p_sample);
						p_sample = data[j][i];
					}
				}	 
			}
			
		}
		/*
			read base call information
			bases are stored in an array of char of length = samples at a point corresponding to their peak
			all other data is simply collected for completeness' sake at this point
			*/
		
		seqlen= 0;
		bases= new byte[numbases]; //[samples]; //new char[samples];
		
		peaks= new int[numbases];
		prob_A= new short[numbases];
		prob_C= new short[numbases];
		prob_G= new short[numbases];
		prob_T= new short[numbases];
		basespare= new short[numbases][3];
		quality= new short[samples];
		
		if (version[0] == '1' || version[0] == '2') {
			for (i=0;i<numbases;i++) {
				peaks[i]= readUint4();
				prob_A[i]=readUint1();
				prob_C[i]=readUint1();
				prob_G[i]=readUint1();
				prob_T[i]=readUint1();
				byte base= (byte) readByte();
				int bat= peaks[i];
				//Debug.print(" b["+bat+"]="+(char)base);
				bases[seqlen++]= base;
				//bases[bat]= base; // this leaves big holes in bases[]
				for (j=0;j<3;j++) basespare[i][j]= readUint1();
			}
			//Debug.println();
			}
		else {
			for (i=0;i<numbases;i++) peaks[i]= readUint4();
			for (i=0;i<numbases;i++) prob_A[i]= readUint1();
			for (i=0;i<numbases;i++) prob_C[i]= readUint1();
			for (i=0;i<numbases;i++) prob_G[i]= readUint1();
			for (i=0;i<numbases;i++) prob_T[i]= readUint1();
			 
			for (i=0;i<numbases;i++) {
				//bases[peaks[i]]= (byte) readByte();
				byte base= (byte) readByte();
				int bat= peaks[i];
				//Debug.print(" b["+bat+"]="+ (char)base);
				bases[seqlen++]= base; 
				//bases[bat]= base; // this leaves big holes in bases[]
			 	}
			//Debug.println();
			 	
			for (j=0;j<3;j++) for (i=0;i<numbases;i++) basespare[i][j]= readUint1();
			}
		
		for (i=0;i<numbases;i++) {
			//byte base= bases[peaks[i]];
			byte base= bases[i];
			switch (base) {
			case 'A': quality[peaks[i]]=prob_A[i]; break;
			case 'C': quality[peaks[i]]=prob_C[i]; break;
			case 'G': quality[peaks[i]]=prob_G[i]; break;
			case 'T': quality[peaks[i]]=prob_T[i]; break;
			default : quality[peaks[i]]=0; break;
			}
			if (i!=0) {
				for (j= (peaks[i-1]+1); j<peaks[i]; j++) {
					quality[j]= (j<(peaks[i-1]+peaks[i])/2) ? quality[peaks[i-1]] : quality[peaks[i]];
				}
			}
		}
		qualmax = 100;

		max = 0;
		for (j=0;j<4;j++) {
			for (i=0;i<samples;i++) {
				if (data[j][i] > max) max = data[j][i];
			}
		}

		char[] rawcomment = new char[comments_size];
		for (i = 0; i<comments_size; i++) rawcomment[i]=(char) readByte();
		Debug.println("comments="+ new String(rawcomment));
		commentHash = parseComments(rawcomment);

		if (commentHash.contains("PHRED"))
			qualmax = 50;
		if (commentHash.containsKey("QMAX"))
			qualmax = (short)(Integer.parseInt((String)commentHash.get("QMAX")));
		leftvecbase=bases_left_clip;
		rightqualbase=bases_right_clip;
		leftqualbase=0;
		rightvecbase=numbases+1;
		if (commentHash.containsKey("CLIP"))
			if (((String)commentHash.get("CLIP")).equals("LRQV1.0")) {
				if (commentHash.containsKey("LQCE"))
					leftqualbase=Integer.parseInt((String)commentHash.get("LQCE"));
				if (commentHash.containsKey("RQCB"))
					rightqualbase=Integer.parseInt((String)commentHash.get("RQCB"));
				if (commentHash.containsKey("LVCE"))
					leftvecbase=Integer.parseInt((String)commentHash.get("LVCE"));
				if (commentHash.containsKey("RVCB"))
					rightvecbase=Integer.parseInt((String)commentHash.get("RVCB"));
			}
		if (leftqualbase <= 0)
			leftqual = 0;
		else if (leftqualbase >= numbases)
			leftqual = samples+1;
		else
			leftqual=(peaks[leftqualbase] + peaks[leftqualbase-1]) / 2;
		if (leftvecbase <= 0)
			leftvec = 0;
		else if (leftvecbase >= numbases)
			leftvec = samples+1;
		else
			leftvec=(peaks[leftvecbase] + peaks[leftvecbase-1]) / 2;
		if (rightqualbase <= 0)
			rightqual=0;
		else if (rightqualbase >= numbases)
			rightqual= samples + 1;
		else
			rightqual=(peaks[rightqualbase-1] + peaks[rightqualbase-2]) / 2;
		if (rightvecbase <= 0)
			rightvec = 0;
		else if (rightvecbase >= numbases)
			rightvec = samples + 1;
		else
			rightvec=(peaks[rightvecbase-1] + peaks[rightvecbase-2]) / 2;

		if (private_size > 0) {
			private_data = new char[private_size];
			for (i=0;i<private_size;i++)
				private_data[i] = (char) readByte();
		}
		
		return seqlen;
	}

	private Hashtable parseComments(char[] raw) {
		int i,j,k;
		char temp[];
		Hashtable comments;
		String tag,value;

		comments = new Hashtable(20,1);
		temp = new char[raw.length];
		i=0;
		while (i<raw.length) {
			j=i;
			while (i<raw.length && raw[i] != '=' && raw[i] != '\n')
				temp[i-j]=raw[i++];
			if (i>=raw.length || raw[i] == '\n') {
				i++;
				continue;
				}
			i++;
			k = 0;
			while(Character.isSpace(temp[k])) k++;
			tag = new String(temp,k,4);
			while(i<raw.length && Character.isSpace(raw[i])) i++;
			j=i;
			while (i<raw.length && raw[i] != '\n')
				temp[i-j]=raw[i++];
			value = new String(temp,0,(i-j));
			comments.put(tag,value);
			i++;
			}
		return comments;
	}
	
	 
}




/* 
public class ScfTraceData 
				//TraceData extends Object 
{
	private static final long SCF_MAGIC = (((((long)'.'<<8)+(long)'s'<<8)+(long)'c'<<8)+(long)'f');
	public static final int NONE = 0;
	public static final int BASES = 1;
	public static final int LEFTVEC = 2;
	public static final int LEFTQUAL = 3;
	public static final int RIGHTQUAL = 4;
	public static final int RIGHTVEC = 5;
	int editnow;
	int i,j;
	public short data[][];
	public byte data1[][];
	public int magic_number,samples,samples_offset,numbases,bases_left_clip,bases_right_clip,bases_offset,comments_size,comments_offset,sample_size,code_set,private_size,private_offset;
	public short max,qualmax;
	int spare[] = new int[18];
	char version[] = new char[4];
	char bases[];
	char rawcomment[];
	char private_data[];
	int peaks[];
	short prob_A[],prob_C[],prob_G[],prob_T[];
	short quality[];
	short basespare[][];
	short p_sample,p_delta;
	byte temp,p_sample1;
	int position;
	Hashtable comments;
	int leftqual,rightqual,leftvec,rightvec,leftqualbase,leftvecbase,rightqualbase,rightvecbase,insertpos;
	String filename;
	boolean insert,replace;
	
	DataInput in;
	
	public ScfTraceData(String filename) throws Exception {
		super();
		FileInputStream filein = new FileInputStream(filename);
		DataInputStream in = new DataInputStream(filein);
		getData(in);
		this.filename = filename;
	}
	
	public ScfTraceData(URL file) throws Exception {
		super();
		URLConnection netfile = file.openConnection();
		//netfile.setRequestProperty("Authorization","Basic c2VxOndndHN0ZzE=");
		netfile.connect();
		InputStream din = netfile.getInputStream();
		DataInput in = new DataInputStream(din);
		getData(in);
		this.filename = null;
	}

	public void getData(DataInput in) throws Exception {
		this.in = in;
		getData();
		}
		
	public void getData() throws Exception {
		magic_number = readUint4();
		if (magic_number != SCF_MAGIC)
			throw new NotScfException("Input file is not an scf file.");
		samples = readUint4();
		samples_offset = readUint4();
		numbases = readUint4();
		bases_left_clip = readUint4();
		bases_right_clip = readUint4();
		bases_offset = readUint4();
		comments_size = readUint4();
		comments_offset = readUint4();
		for (i=0;i<4;i++) {
			version[i]= (char)readByte();
		}
		sample_size = readUint4();
		code_set = readUint4();
		private_size = readUint4();
		private_offset = readUint4();
		for (i=0;i<18;i++) {
			spare[i] = readUint4();
		}
		if (sample_size == 1 && version[0] == '3') 
			data1 = new byte[4][samples];
		data = new short[4][samples];
		if (version[0] == '2' || version[0] == '1') {
			if (sample_size == 1) {
				for (i=0;i<samples;i++) {
					for (j=0;j<4;j++) {
						data[j][i] = readUint1();
					}
				}
			}
			else if (sample_size == 2) {
				for (i=0;i<samples;i++) {
					for (j=0;j<4;j++) {
						data[j][i]= readSint2s();
					}
				}
			}
		}
		else {
			if (sample_size == 1) {
				for (j=0;j<4;j++) {
					for (i=0;i<samples;i++) {
						data1[j][i]= readByte();
					}
				}
				for (j=0;j<4;j++) {
					p_sample1 = 0;
					for (i=0;i<samples;i++) {
						data1[j][i] += p_sample1;
						p_sample1 = data1[j][i];
					}
					
					p_sample1 = 0;
					for (i=0;i<samples;i++) {
						data1[j][i] += p_sample1;
						p_sample1 = data1[j][i];
						data[j][i] = (short) data1[j][i];
						if (data[j][i] < 0)
							data[j][i] += 256;
					}
				}
			}
			else if (sample_size == 2) {
				for (j=0;j<4;j++) {
					for (i=0;i<samples;i++) {
						data[j][i]= readSint2s();
					}
				}
				for (j=0;j<4;j++) {
					p_sample = 0;
					for (i=0;i<samples;i++) {
						data[j][i] = (short) (data[j][i] + p_sample);
						p_sample = data[j][i];
					}
					
					p_sample = 0;
					for (i=0;i<samples;i++) {
						data[j][i] = (short) (data[j][i] + p_sample);
						p_sample = data[j][i];
					}
				}	 
			}
			
		}
		 
			//read base call information
			//bases are stored in an array of char of length = samples at a point
			//corresponding to their peak
			//all other data is simply collected for completeness' sake at this point
		 
		bases= new char[samples];
		peaks= new int[numbases];
		prob_A= new short[numbases];
		prob_C= new short[numbases];
		prob_G= new short[numbases];
		prob_T= new short[numbases];
		basespare= new short[numbases][3];
		quality= new short[samples];
		
		
		if (version[0] == '1' || version[0] == '2') {
			for (i=0;i<numbases;i++) {
				peaks[i]= readUint4();
				prob_A[i]=readUint1();
				prob_C[i]=readUint1();
				prob_G[i]=readUint1();
				prob_T[i]=readUint1();
				bases[peaks[i]]=(char)readByte();
				for (j=0;j<3;j++) {
					basespare[i][j]= readUint1();
				}
			}
		}
		else {
			for (i=0;i<numbases;i++) {
				peaks[i]= readUint4();
			}
			for (i=0;i<numbases;i++) {
				prob_A[i]= readUint1();
			}
			for (i=0;i<numbases;i++) {
				prob_C[i]= readUint1();
			}
			for (i=0;i<numbases;i++) {
				prob_G[i]= readUint1();
			}
			for (i=0;i<numbases;i++) {
				prob_T[i]= readUint1();
			}
			for (i=0;i<numbases;i++) {
				bases[peaks[i]]=(char) readByte();
				//System.err.println(String.valueOf(bases[peaks[i]])+":");
			}
			for (j=0;j<3;j++) {
				for (i=0;i<numbases;i++) {
					basespare[i][j]= readUint1();
				}
			}
		}
		
		for (i=0;i<numbases;i++) {
			switch (bases[peaks[i]]) {
			case 'A':
				quality[peaks[i]]=prob_A[i];
				break;
			case 'C':
				quality[peaks[i]]=prob_C[i];
				break;
			case 'G':
				quality[peaks[i]]=prob_G[i];
				break;
			case 'T':
				quality[peaks[i]]=prob_T[i];
				break;
			default:
				quality[peaks[i]]=0;
				break;
			}
			if (i!=0) {
				for (j=(peaks[i-1]+1);j<peaks[i];j++) {
					quality[j]=(j<(peaks[i-1]+peaks[i])/2) ? quality[peaks[i-1]] : quality[peaks[i]];
				}
			}
		}
		qualmax = 100;

		max = 0;
		for (j=0;j<4;j++) {
			for (i=0;i<samples;i++) {
				if (data[j][i] > max)
					max = data[j][i];
			}
		}

		rawcomment = new char[comments_size];
		for (i = 0; i<comments_size; i++)
			rawcomment[i]=(char) readByte();
		comments = parseComments(rawcomment);

		if (comments.contains("PHRED"))
			qualmax = 50;
		if (comments.containsKey("QMAX"))
			qualmax = (short)(Integer.parseInt((String)comments.get("QMAX")));
		leftvecbase=bases_left_clip;
		rightqualbase=bases_right_clip;
		leftqualbase=0;
		rightvecbase=numbases+1;
		if (comments.containsKey("CLIP"))
			if (((String)comments.get("CLIP")).equals("LRQV1.0")) {
				if (comments.containsKey("LQCE"))
					leftqualbase=Integer.parseInt((String)comments.get("LQCE"));
				if (comments.containsKey("RQCB"))
					rightqualbase=Integer.parseInt((String)comments.get("RQCB"));
				if (comments.containsKey("LVCE"))
					leftvecbase=Integer.parseInt((String)comments.get("LVCE"));
				if (comments.containsKey("RVCB"))
					rightvecbase=Integer.parseInt((String)comments.get("RVCB"));
			}
		if (leftqualbase <= 0)
			leftqual = 0;
		else if (leftqualbase >= numbases)
			leftqual = samples+1;
		else
			leftqual=(peaks[leftqualbase] + peaks[leftqualbase-1]) / 2;
		if (leftvecbase <= 0)
			leftvec = 0;
		else if (leftvecbase >= numbases)
			leftvec = samples+1;
		else
			leftvec=(peaks[leftvecbase] + peaks[leftvecbase-1]) / 2;
		if (rightqualbase <= 0)
			rightqual=0;
		else if (rightqualbase >= numbases)
			rightqual= samples + 1;
		else
			rightqual=(peaks[rightqualbase-1] + peaks[rightqualbase-2]) / 2;
		if (rightvecbase <= 0)
			rightvec = 0;
		else if (rightvecbase >= numbases)
			rightvec = samples + 1;
		else
			rightvec=(peaks[rightvecbase-1] + peaks[rightvecbase-2]) / 2;

		if (private_size > 0) {
			private_data = new char[private_size];
			for (i=0;i<private_size;i++)
				private_data[i] = (char) readByte();
		}

	}

	private Hashtable parseComments(char[] raw) {
		int i,j,k;
		char temp[];
		Hashtable comments;
		String tag,value;

		comments = new Hashtable(20,1);
		temp = new char[raw.length];
		i=0;
		while (i<raw.length) {
			j=i;
			while (i<raw.length && raw[i] != '=' && raw[i] != '\n')
				temp[i-j]=raw[i++];
			if (i>=raw.length || raw[i] == '\n') {
				i++;
				continue;
			}
			i++;
			k = 0;
			while(Character.isSpace(temp[k]))
				k++;
			tag = new String(temp,k,4);
			while(i<raw.length && Character.isSpace(raw[i]))
				i++;
			j=i;
			while (i<raw.length && raw[i] != '\n')
				temp[i-j]=raw[i++];
			value = new String(temp,0,(i-j));
			comments.put(tag,value);
			i++;
			//System.out.println(tag+"="+value);
		}
		return comments;
	}
	
	 
	private int readUint4() throws IOException {
		int temp[] = new int[4];
		int i;
		for (i=0;i<4;i++) {
			temp[i] = in.readUnsignedByte();
		}
		return (temp[3] + (temp[2]<<8) + (temp[1]<<16)+(temp[0]<<24));
	}
	private int readUint2() throws IOException {
		int temp[] = new int[2];
		int i;
		for (i=0;i<2;i++) {
			temp[i] = in.readUnsignedByte();
		}
		return (temp[1]+(temp[0]<<8));
	}
	private short readSint2s( in) throws IOException {
		short temp[] = new short[2];
		int i;
		for (i=0;i<2;i++) {
			temp[i] = (short) in.readUnsignedByte();
		}
		return (short) (temp[1]+(temp[0]<<8));
		//return (short) in.readShort();
	}
	private short readUint1() throws IOException {
		return (short) in.readUnsignedByte();
	}
	private short readSint1() throws IOException {
		return (short) in.readByte();
	}
	private byte readByte() throws IOException {
		return in.readByte();
	}
 
	
	public boolean putData() throws Exception {
		DataOutputStream out;
		BufferedOutputStream buffer;
		Enumeration keys;
		String allcomments,key,value;
		short datatemp[][];
		
		if (filename == null)
			return false;
		comments.put("CLIP","LRQV1.0");
		comments.put("LQCE",Integer.toString(leftqualbase));
		comments.put("RQCB",Integer.toString(rightqualbase));
		comments.put("LVCE",Integer.toString(leftvecbase));
		comments.put("RVCB",Integer.toString(rightvecbase));
		allcomments = "";
		keys = comments.keys();
		while (keys.hasMoreElements()) {
			key = (String)keys.nextElement();
			value = (String)comments.get(key);
			allcomments += key+"="+value+"\n";
		}
		buffer = new BufferedOutputStream((new FileOutputStream(filename)),5120);
		out = new DataOutputStream(buffer);
		out.writeInt((int)SCF_MAGIC);
		out.writeInt(samples);
		out.writeInt(128);
		out.writeInt(numbases);
		out.writeInt(leftvecbase);
		out.writeInt(rightqualbase);
		out.writeInt(samples*4*2+128); //bases offset for sample_size = 2
		out.writeInt(allcomments.length()); //length of comments
		out.writeInt(numbases*12+samples*4*2+128); //comments offset
		out.writeBytes("3.00"); //version number
		out.writeInt(2); //byte size of sample data
		out.writeInt(0); //default code set (- instead of N)
		out.writeInt(private_size);
		out.writeInt(allcomments.length()+numbases*12+samples*4*2+128); //private offset
		for (i=0;i<18;i++)
			out.writeInt(spare[i]); //unused bytes
		datatemp = new short[4][samples];
		for (i=0;i<4;i++)
			System.arraycopy(data[i],0,datatemp[i],0,samples);
		for (i=0;i<4;i++) {
			p_delta = 0;
			for (j=0;j<samples;j++) {
				p_sample = datatemp[i][j];
				datatemp[i][j] = (short)(datatemp[i][j] - p_delta);
				p_delta = p_sample;
			}
			p_delta = 0;
			for (j=0;j<samples;j++) {
				p_sample = datatemp[i][j];
				datatemp[i][j] = (short)(datatemp[i][j] - p_delta);
				p_delta = p_sample;
			}
		}
		for (i=0;i<4;i++)
			for (j=0;j<samples;j++)
				out.writeShort(datatemp[i][j]);
		for (i=0;i<numbases;i++)
			out.writeInt(peaks[i]);
		for (i=0;i<numbases;i++)
			out.writeByte(prob_A[i]);
		for (i=0;i<numbases;i++)
			out.writeByte(prob_C[i]);
		for (i=0;i<numbases;i++)
			out.writeByte(prob_G[i]);
		for (i=0;i<numbases;i++)
			out.writeByte(prob_T[i]);
		for (i=0;i<numbases;i++)
			out.writeByte((byte)bases[peaks[i]]);
		for (j=0;j<3;j++)
			for (i=0;i<numbases;i++)
				out.writeByte(basespare[i][j]);
		out.writeBytes(allcomments);
		if (private_size > 0)
			out.writeBytes(String.valueOf(private_data));
		buffer.flush();
		buffer.close();
		return true;
	}

}
****/

class NotScfException extends IOException {
	public NotScfException(String msg) {
		super(msg);
	}
}
