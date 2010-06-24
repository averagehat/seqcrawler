// SeqRange.java
// sequence location parsing
// d.g.gilbert, 1997++


package iubio.bioseq;



/**
 parse, hold and edit biosequence feature locations <pre>
 examples to parse 
 258
 255..457
 105^106
 order(M55673:2559..>3688,<1..254)
 join(M55673:1820..2274,M55673:2378..2558,255..457)
 </pre>
*/

//public interface DSeqChanges1 { // dup of biosequence.java iface !
// 	public final static int kDelete = 1, kInsert = 2, kReorder = 4, kChange = 8;
//};


public class SeqRange 
	implements Cloneable
	//, DSeqChanges1
{
 	public final static int kDelete = 1, kInsert = 2, kReorder = 4, kChange = 8; // was interface DSeqChanges1
 	public final static int kNoValue = Integer.MIN_VALUE;
	public final static int kZero = 1;  // feature table vals are 1 based, want 0 base for useage here?
	public final static int // uncertainty flags
		kStartless = 1, kStartmore = 2, kEndless= 4, kEndmore= 8, kBetween= 16, 
		kStartMatchend= 32, kEndMatchend = 64;  // special 'end' values, as in '1..end', 'end..10'
	
	public final static String[] sOperators = {
		"join","complement","order","group", "one-of", "", ""   // location operators 
		};	
	public final static byte //op vals
		opJoin = 0, opComplement = 1, opOrder= 2, opGroup= 3, opOneof= 4, opMax= 5, opNull= 6;
					
	public static boolean joinspace= false;
						
	protected int start, nbases;  // use 1 base or 0 base !?
	protected int myZero= kZero;
	protected byte uncertain;
	protected byte operation= opNull; 	// sOperators[x] 
	protected SeqRange next; 		// next item in list
	protected String refid; 			// for join reference to "remote" accession# data
	
	public SeqRange() {}
	public SeqRange( int start, int nbases) { 
		set(start, nbases); 
		}
	public SeqRange( int start, int nbases, int uncertain) { 
		this(start, nbases, uncertain, null, opNull, null); 
		}
	public SeqRange( int start, int nbases, int uncertain, String refid) { 
		this(start, nbases, uncertain, refid, opNull, null); 
		}
	public SeqRange( int start, int nbases, int uncertain, 
		String refid, String soperation, SeqRange next) { 
		this(start, nbases, uncertain, refid, opNull, next); 
		if (soperation!=null) {
			if ("jo".equals(soperation)) this.operation= opJoin;
			else if ("co".equals(soperation)) this.operation= opComplement;
		 	else for (byte op= opJoin; op < opMax; op++)  
				if (sOperators[op].equals(soperation)) { this.operation= op; break; }
		 }
		}

	public SeqRange(String range) throws SeqRangeException {
		this.parse1( range );
		}
	
	protected SeqRange( int start, int nbases, int uncertain, 
		String refid, byte oper, SeqRange next) { 
		this.start= start; this.nbases= nbases; 
		this.uncertain= (byte)uncertain;
		this.refid= refid; 
		this.next= next;
		this.operation= oper;
		}


	public final void set( int start, int nbases) { this.start= start; this.nbases= nbases; }

	public final int start() { return start; }
	public final int nbases() { return nbases; }
	public final int stop() { return start + nbases - 1; } 
			//! may01, was start+nbases; ^^ stop() should be start + nbases - 1 to index last base
	public final int uncertain() { return (int)uncertain; }
	public final SeqRange next() { return next; }
	public final String operation() { return (operation>=opMax) ? "" : sOperators[operation]; }
	public final int opint() { return (int)operation; }
	public final String remoteSeq() { return refid; }
	public final boolean isRemote() { return (refid!=null); }
	public final boolean isEmpty() { return (start==0 && nbases==0 && next==null); }
	public final boolean isComplex() { return (operation < opMax || next!=null); }
	public final boolean isComplement() { return (operation == opComplement); }
	public final int origin() { return myZero; }
		// for subrange math:
	public final boolean stopIsEnd()  { return ( (uncertain & kEndMatchend) != 0); }
	public final boolean startIsEnd() { return ( (uncertain & kStartMatchend) != 0 ); }
	 
	public final void setStart(int start) { this.start= start; }
	public final void setNbases(int nbases) { this.nbases= nbases; }
	
	public int setDisplayOrigin(int zerobase) { 
		int savez= myZero;
		myZero= zerobase; 
		for (SeqRange nx= next; nx!=null; nx= nx.next()) nx.myZero= zerobase;
		return savez;
		}
	
	public int max() {
		int max= stop();
		for (SeqRange nx= next; nx!=null; nx= nx.next()) max= Math.max( max, nx.stop());
		return max;
		}
		
	public void copy( SeqRange sr) {
		start= sr.start;
		nbases= sr.nbases;
		uncertain= sr.uncertain;
		operation= sr.operation;
		refid= sr.refid;
		next= sr.next; //? or clone
		}
		
	public void add( SeqRange sr) { // == append(sr)
		if (sr!=null && sr.nbases > 0) {
			if ( nbases==0 ) copy(sr); // && start == 0 ??
			else {
				SeqRange nr= this;
				while (nr.next!=null) nr= nr.next;
				// patch here to check that sr.start >= nr.stop
				if (sr.start < nr.stop()) {
					if (sr.stop() < nr.stop()) return;
				  sr.nbases += sr.start - nr.stop();
				  sr.start= nr.stop();
					}
				nr.next= sr;
				if (operation==opNull) operation= opJoin;
				}
			}
		}
		
	public Object clone() {
		try {
			SeqRange sr= (SeqRange) super.clone();
			if (next!=null) sr.next= (SeqRange) next.clone(); //!? recursion okay?
		 	return sr;
			}
		catch(CloneNotSupportedException ex) { throw new Error(ex.toString()); }
		}


	public final boolean updateRange(int changeflags, int astart, int length) 
	{ return updateRange(changeflags, astart, length, null); }
	
	public boolean updateRange(int changeflags, int astart, int length, byte[] basechanges) 
	{
		//int astop= astart + length;
		if (astart > stop()) return false; //astop < start || 
		int oldstart= start, oldlen= nbases;
		int istop= stop();
		if (basechanges != null) {
			for (int i=0; i<basechanges.length; i++) {
				int at= i+astart;
				if (at > istop) break;
				switch (basechanges[i]) {
					case kDelete: if (at < start) start--; else if (at <= istop) nbases--; break;
					case kInsert: if (at < start) start++; else if (at <= istop) nbases++; break;
					//case kReorder: break;// ? move start?
					//case kChange: break;// ? 
					}
				}
			}
		else {
					// does changeflags == (kInsert | kDelete) make any sense? - same length +/-
			if ((changeflags & kInsert) != 0) {
				if (astart < start) start += length; else if (astart <= istop) nbases += length;
				}
			else if ((changeflags & kDelete) != 0) {
				if (astart < start) start -= length; else if (astart <= istop) nbases -= length;
				}
			//if ((changeflags & kReorder) != 0) {} // ? move start?
			//if ((changeflags & kChange) != 0) { }
			}
		if (start<0) start= 0;
		if (nbases<0) nbases= 0;
		
		boolean changedme= (oldstart != start || oldlen != nbases);
		boolean changednext= false;
		if (next!=null)  // recursively change all ranges till > next.stop()
			changednext= next.updateRange( changeflags, astart,  length, basechanges);
			
		return (changedme || changednext);
	}
			
				
  public String toString() {
  	StringBuffer sb= new StringBuffer();
  	//if (next!=null) {
		if (operation<opMax) {
			sb.append(sOperators[operation]);  
  		sb.append('(');
			}
  		toBuf(sb);
		if (operation<opMax) sb.append( ')');
		//}
  	//else toBuf(sb);
  	return sb.toString();
  	}

	public boolean equals(Object ob) {
		if (ob instanceof SeqRange) {
			SeqRange sr= (SeqRange) ob;
			if (start == sr.start && nbases == sr.nbases)
				return (next==null || next.equals(sr.next));
			}
		return false;
		}

	public boolean contains(SeqRange sr) {
		if (sr==null) return false;
		else if (start <= sr.start() && max() >= sr.max()) return true;
		else return false;
		}
		
	public boolean intersects(SeqRange sr) {
		if (sr==null) return false;
		else if (start <= sr.stop() && stop() >= sr.start) return true;
		else if (next!=null && next.intersects(sr)) return true;
		else if (sr.next!=null && intersects(sr.next)) return true;
		else return false;
		}

	public boolean intersectsMax(SeqRange sr) {
		if (sr==null) return false;
		else if (start <= sr.max() && max() >= sr.start) return true;
		else return false;
		}

	public SeqRange intersection0(SeqRange sr) {
		if (sr==null) return null; //new SeqRange(); // or null?
		if (start <= sr.max() && max() >= sr.start) {
			// simple case for max range, ...
			int b= Math.max(start, sr.start());
			int e= Math.min(max(), sr.max());
			return newRange(b, e-b+1 );
			}
		return null;
		}
		
	// ^^ revise intersection to return contained range
	
	public SeqRange intersection( SeqRange sr)
	{
		if (sr==null) return this;
		//if (this.contains(sr)) isect= sr;
		//else if (sr.contains(this)) isect= this;
		if ( sr.start() <= start && sr.max() >= max()) return this;
		else if (start <= sr.start() && max() >= sr.max()) return sr;
		else {
			// intersection drops 'complement(join(...))' parts
			return this.intersection0(sr); 	 
			}
	}

	public int compareTo(SeqRange sr) {
		if (sr==null) return -1;
		else if (start < sr.start()) return -1;
		else if (start > sr.start()) return 1;
		else {
		 	boolean myun= ((uncertain & kStartless) != 0);
		 	boolean srun= ((sr.uncertain & kStartless) != 0);
		 	if (myun && !srun) return -1;
		 	else if (!myun && srun) return 1;
			int mx= max();
			int srmax= sr.max();
			if (mx < srmax) return -1;
			else if (mx > srmax) return 1;
		 	myun= ((uncertain & kStartmore) != 0) ;
			srun= ((sr.uncertain & kStartmore) != 0);  
		 	if (myun && !srun) return 1;
		 	else if (!myun && srun) return -1;
			else return 0;
			}
		}

	public SeqRange invert(int maxlen)  
	{
		SeqRange invsr= newRange();
		int at= 0; //? or this.start ?
		for (SeqRange sr= this; sr!=null && at<maxlen; sr= sr.next) {
			if (sr.isRemote()) continue; //?
			if (sr.start > at) invsr.add( newRange(at, sr.start-at) );
			at= sr.stop()+1; //- NO - off by 1? fixed may01
			}
		if (at < maxlen) invsr.add( newRange(at, maxlen-at) );
		return invsr;
	}
	
	public SeqRange joinRange(SeqRange sr)  
	{
		SeqRange unionsr= null;
			//? implement only for join()s? - can assume ascending order for each sr
			//? skip refids 
			//? find overall min/max range, then handle holes in range?
		//int min= Math.min( start, sr.start);
		//int max= Math.max( max(), sr.max());
		//unionsr= new SeqRange( min, max-min, uncertain, refid, sOperators[0], null);

		unionsr= newRange();
		addRange( unionsr, sr);
		//if (unionsr.next!=null) 
		if (unionsr.next!=null && unionsr.operation==opNull) 
			unionsr.operation= opJoin;  
		return unionsr;
	}


	protected void addRange(SeqRange unionsr, SeqRange sr) 
	{
			// need to screen out sr/this with refid ! for getFeatureRanges/extractFeatureBases
		while (sr!=null && sr.isRemote()) sr= sr.next;
		if (sr==null) {
			if (!this.isRemote())
				unionsr.add( new SeqRange(start, nbases, uncertain, refid) );
			if (next!=null) next.addRange( unionsr, sr);
			}
		else {
			if (start == 0 && nbases == 0) { // empty this
				unionsr.add( new SeqRange( sr.start, sr.nbases, sr.uncertain, sr.refid) );
				addRange( unionsr, sr.next);
				}
			else if (!this.isRemote() && start <= sr.stop() && stop() >= sr.start) {
				int min= Math.min( start, sr.start );
				int max= Math.max( stop(), sr.stop() );
				unionsr.add( new SeqRange(min, max-min+1, uncertain | sr.uncertain) );
				if (next!=null) next.addRange( unionsr, sr.next);
				else if (sr.next!=null) sr.next.addRange( unionsr, null);
				}
			else if (sr.start < start || this.isRemote()) {
				unionsr.add( new SeqRange( sr.start, sr.nbases, sr.uncertain, sr.refid) );
				addRange( unionsr, sr.next);
				}
			else {
				unionsr.add( new SeqRange(start, nbases, uncertain, refid) );
				sr.addRange( unionsr, next);
				}
			}
	}
		
	public SeqRange subrange(SeqRange subr)  
	{
		if (subr==null || subr.isEmpty()) return this; // new Subrange(this) ??
		SeqRange unionsr= newRange();
		SeqRange thisr= this;
		if (isComplement()) thisr= this.reverse();  
		
		for (SeqRange sr= subr; sr!=null; sr= sr.next) {
			int rstop;
			//if (sr.nbases<0) rstop= thisr.max(); //? not valid
		  //else
			if (sr.stopIsEnd()) rstop= thisr.max() + sr.stop(); 
			else rstop= thisr.start() + sr.stop();  

			int rstart; 			
			if (sr.startIsEnd()) rstart= thisr.max() + sr.start();
			else rstart= thisr.start() + sr.start();

			if (rstop <= thisr.start()) {  
				unionsr.add( newRange( rstart, rstop-rstart+1));
				}
			else if (rstart >= thisr.max()) {  
				unionsr.add( newRange( rstart, rstop-rstart+1));
				}
			else	
				thisr.subRange( unionsr, sr, rstart, rstop);
			}
		
		if (isComplement())	{
			unionsr= unionsr.reverse();
			unionsr.operation= this.operation;  
			}
		else	
		if (unionsr.next!=null && unionsr.operation==opNull) 
			unionsr.operation= opJoin;  
		return unionsr;
	}

	protected void subRange(SeqRange unionsr, SeqRange sr, int srstart, int srstop) 
	{
		if (sr==null) { }
		else if (start == 0 && nbases == 0) {  
			if (next!=null) next.subRange( unionsr, sr, srstart, srstop);
			}
		else {
		
			if (srstop < start) {  
				if (unionsr.isEmpty()) unionsr.add( newRange( srstart, srstop-srstart+1 ));
				return;	
				}
			else if (srstart > stop()) {  
				if (next!=null) next.subRange( unionsr, sr, srstart, srstop);
				if (unionsr.isEmpty()) unionsr.add( newRange( srstart, srstop-srstart+1 ));
				return;	
				}
			else {
				int b= start, e= stop();
				if (srstart < start) {
					if (unionsr.isEmpty()) b= srstart;
					else b= start;
					}
				else 
					b= Math.max(srstart, start());
				if (srstop > stop()) {
					if (next==null) e= srstop;
					else e= stop();
					}
				else 
					e= Math.min(srstop, stop());
				
				SeqRange addr= newRange( b, e-b+1 );
				unionsr.add( addr);
				if (next!=null) next.subRange( unionsr, sr, srstart, srstop);
				}
				
  		//if (sr.next!=null) subRange( unionsr, sr.next, srstart, srstop);
			}	
	}	 
	
	/*
	public SeqRange subrange0(SeqRange subr)  
	{
		SeqRange unionsr= newRange();
		if (isComplement()) {
			SeqRange rev= this.reverse(); // need to test !
			rev.subRange( unionsr, subr);
			unionsr= unionsr.reverse();
			unionsr.operation= this.operation;  
			}
		else
			subRange( unionsr, subr);
			
		if (unionsr.next!=null && unionsr.operation==opNull) 
			unionsr.operation= opJoin;  
		return unionsr;
	}
	
	protected void subRange(SeqRange unionsr, SeqRange sr) 
	{
				// sr= (-100..-10) means unionsr == this.start - 100 .. this.start - 10
				// sr= (-10..10,50..100) means this.start - 10 .. this.start+10, this.start+50..this.start+100
				// sr= complement(-100..-10) means this.max + 10 .. this.max + 100 
				// ?? extract sr from each this.next ? or compare sr + vals to this/next.start,stop ?

				// this() + sr(-10) means -10 before this.start; 
					// ie 10 before start
				// this.compl() + sr(-100..-10) means compl(+10..+100 after this.stop); 
					// ie, 100..10 before start after revcomp
				// this() + sr.compl(-100..-10) means +10..+100 after this.stop
					// ie, 10..100 after end 
				// this.compl() + sr.compl(-10) means compl(-10 before this.start) ??
				   // i.e., 10 after end after revcompl
				   
		if (sr==null) {
			//! add nothing !
			//if (next!=null) next.subRange( unionsr, sr);
			}
		else if (start == 0 && nbases == 0) { // empty this -- skip 
			if (next!=null) next.subRange( unionsr, sr);
			}
		else {
			byte unop= opNull;
			int rbases;
			if (sr.nbases<0) rbases= this.nbases;
			else if (sr.stopIsEnd()) rbases= this.nbases + sr.nbases; 
			else rbases= sr.nbases; 
			int rstart; //? do we care if sr.nbases don't intersect this.nbases?
				// handle stopIsEnd() and startIsEnd()
			
				//? ignore complement here? (add. problem), put in end?
			  
		///	if (isComplement()) {
		//		unop= opComplement;
		//		//if (sr.operation == opComplement) rstart= start + sr.start();
		//		//else 
		//		if (sr.startIsEnd())  
		//			rstart= start() + sr.stop();
		//		else  
		//			rstart= stop() - sr.stop();
		//		}
		//	else 
		 
			{
				//if (sr.operation == opComplement) rstart= stop() - sr.stop();
				//else 
				if (sr.startIsEnd())  
					rstart= stop() + sr.start();
				else  
					rstart= start + sr.start();
			}
			SeqRange addr= new SeqRange( rstart, rbases, uncertain, null, unop, null);
			unionsr.add( addr);
 			if (sr.next!=null) subRange( unionsr, sr.next);
			if (next!=null) next.subRange( unionsr, sr);
			}	
		 
	}
	*/
	
	
	public SeqRange reverse()  
	{
		SeqRange revr= newRange(); 
		reverseRange( revr);
		revr.operation= this.operation; //?
		//if (revr.next!=null && revr.operation==opNull) revr.operation= opJoin;  
		return revr;
	}

	protected void reverseRange(SeqRange revr) {
		// need for subrange() of complement, others?
		// make stop->start, start->stop, ? use origin/myZero to make reversible ?
		// 4..5,20..40,55..60 => origin=-60?  0..5,20..40,55..56  
		// ==>(re-reverse after subrange)  +origin= -60..-55,-40..-20,-5..-4
		
		if (next!=null) next.reverseRange(revr); 
		SeqRange rev1= new SeqRange( -start-nbases, nbases, uncertain, null, operation, null);
		revr.add( rev1);
		}
		
	protected void toBuf(StringBuffer sb) {
	 	if (refid!=null) { sb.append(refid); sb.append(':'); }
	 	if ((uncertain & kStartless) != 0) sb.append( '<');
	 	else if ((uncertain & kStartmore) != 0) sb.append( '>');
		if ((uncertain & kStartMatchend) != 0) sb.append( "end");
	 	sb.append( start+myZero);
	 	boolean do2= ((uncertain & (kBetween|kEndless|kEndmore|kEndMatchend)) != 0 );
  	if ( do2 || nbases>1 ) { //? or do if >0 ?
			if ((uncertain & kBetween) != 0) sb.append('^'); //? or just test if (start+1 == stop())
			else sb.append("..");
		 	if ((uncertain & kEndless) != 0) sb.append( '<');
		 	else if ((uncertain & kEndmore) != 0) sb.append( '>');
			if ((uncertain & kEndMatchend) != 0) sb.append( "end");
			sb.append( stop() + myZero); // was - 1 !  fix may01
			}
		if (next!=null) {
			sb.append( ',');
			if (joinspace) sb.append(' ');
			next.toBuf(sb);
			}
		}

		//
		// parsing
		//
  	
	protected final int getMyMark( String s)  {	
  	int at= s.indexOf(':');
  	if (at>0) { refid= s.substring(0,at); s= s.substring(at+1); }
  	return getMyMark( s, kStartless, kStartmore, kStartMatchend); 
  	}
  	
  protected final int getMyEndMark( String s)  {	
  	return getMyMark( s, kEndless, kEndmore, kEndMatchend); 
  	}
  
  protected int getMyMark( String s, int lessflag, int moreflag, int endflag) 
  {
  		// note: Feature Table numbers are +1 of our 0 based indexing
		boolean minus= false;
  	int val= kNoValue, b= -1, e = -1;
  	int slen= s.length();
		for (int i= 0; i<slen; i++) {
			char c= s.charAt(i);
			if (c <= ' ') continue; // eat whitespace
			else if (c>='0' && c<='9') {
				e= i; if (b == -1) b= i;
				}
			else if (b >= 0) { break; } //e= i; 
			else switch (c) {  // these only lead numbers
				case '<': uncertain |= lessflag; break; // kEndless for end
				case '>': uncertain |= moreflag; break; // kEndmore for end
				case '^': uncertain |= kBetween; break;  
				case '-': minus= true; break;
				case '+': minus= false; break;
				case 'e': { 
						// "end" special case - allow: 1..end-10, 1..end+10, end-10..end+10, end+10..end+100
					if (i+1<slen && s.charAt(i+1)=='n') i++;
					if (i+1<slen && s.charAt(i+1)=='d') i++;
					uncertain |= endflag;
					val= 0; // a following number will change
					break;
					} 
				}
  		}
  		// need to distinguish no val and -1 val
  	if (b<0) return val; //kNoValue is bad value
  	try { val= Integer.parseInt( s.substring(b,e+1) ); }
  	catch (Exception ex) { return kNoValue; }
  	if (minus) val= -val;
  	val -= myZero; // change to 0 based
  	return val;
	}
  	
  protected boolean getMyRange(String s) 
  {
		int sepwid= 2;
		int e= s.indexOf("..");
		if (e<0) {
		  e= s.indexOf('^'); 
			if (e>0) { uncertain |= kBetween; sepwid= 1; }
			}
		if (e>=0) { 
			int start= getMyMark( s.substring(0,e)); // if bad?
			int stop = getMyEndMark( s.substring(e+sepwid));// if bad?
			if (start==kNoValue) {
				if (stop==kNoValue) return false;
				else set(stop, 1);
				}
			else if (stop==kNoValue) set(start, 1);
			else set( start, stop - start + 1);
			}
		else {
			int start= getMyMark( s); // if bad?
			if (start==kNoValue) return false;
			else set( start, 1); // this is bad for s==""
			}
		return true;
	}
  	
	protected boolean getNextJoin(String s) 
	{
		if (s.length()==0) return false;
  	int c= s.indexOf(',');	
		if (c<0) return getMyRange( s);
		else {
			if (!getMyRange( s.substring(0,c))) 
				return this.getNextJoin( s.substring(c+1) );
			else {
				SeqRange newnext= newRange();
				boolean ok= newnext.getNextJoin( s.substring(c+1)); //? should go to parse2() to check for oper?
			  if (ok)	this.next= newnext;
			  return ok;
			  }
			}
  }

	public SeqRange newRange() {
		SeqRange sr= new SeqRange();
		sr.myZero= myZero;
		return sr;
		}
	public SeqRange newRange( int start, int nbases) {
		SeqRange sr= new SeqRange( start, nbases);
		sr.myZero= myZero;
		return sr;
		}
		
	public void parse1(String s) throws SeqRangeException
	{
  	try {
	  	operation= opNull; 
			s= s.trim(); //.toLowerCase()? -- no, ops are always lc & lc mangles refid
 	  	if (s.length()==0) return;
			else if (s.startsWith("jo(")) this.operation= opJoin;
			else if (s.startsWith("co(")) this.operation= opComplement;
	  	else for (byte op= opJoin; op < opMax; op++)  
	  		if (s.startsWith( sOperators[op])) {
	  			operation= op; //sOperators[i];
	  			break;
	  			}
	  	if (operation!=opNull) {  //? do even if opNull
	  		int e = 0;
	  		int i= s.indexOf('(');
	  		if (i>=0) e= s.indexOf(')', i);
	  		if (e<0) e= s.length();
	  		
	  		// is oper(loc,loc), oper2(loc,loc),... possible?
	  		// or oper( oper2(loc,loc), oper2(loc,loc) ); ?? YES
	  		//! need to parse these:
	  		//  co(jo(1..2,3..4)) ; jo(co(1..2),co(3..4))
	  		
	  		if (i>0) s= s.substring( i+1, e); //? check for anything after ')'? 
	  		else s= s.substring( sOperators[operation].length() ); //? error
	  		}
	  		
	  	getNextJoin( s); // always - does ',' test
	  	//int c= s.indexOf(','); if (c>0) getNextJoin( s); else getMyRange( s);
	  	}
  	catch (Exception e) {
  		//System.err.println("SeqRange error parsing '"+s+"'");
  		throw new SeqRangeException("SeqRange error parsing '"+s+"'");
  		}
 	}
 	
	public static SeqRange parse(String s) throws SeqRangeException
  {
  	if (s==null) return null; //? or empty ?
		SeqRange sr= new SeqRange();
		sr.parse1( s );
		return sr;
	}
	
	
}


