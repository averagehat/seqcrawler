package iubio.readseq;

class NumSppBases {
	boolean good;
	int nospp, baselen;
	NumSppBases() { this(0,0); }
	NumSppBases(int nospp, int baselen) { 
		this.nospp= nospp; this.baselen= baselen;
		good= (nospp>0 && baselen>0);
		}
	void init() { nospp= baselen= 0; good= false; }
}