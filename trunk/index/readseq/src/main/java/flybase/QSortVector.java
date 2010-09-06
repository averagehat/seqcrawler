//flybase/QSortVector.java
//split4javac// flybase/SortVector.java date=04-Sep-1999

// dclap/util/sortvector.java


package flybase; //.util ?

import java.util.Vector;
import java.util.Enumeration;


//split4javac// flybase/SortVector.java line=107
public class QSortVector extends SortVector 
{
	protected Comparator cmp;
	public QSortVector(Vector v, ComparableVector cv) { super(v,cv); }
	public QSortVector(FastVector v, ComparableVector cv) { super(v,cv); }
	public QSortVector(FastVector v, ComparableVector cv, Comparator cmp) { 
		super(v,cv); 
		this.cmp= cmp;
		}
	
	public void sort() { 
		if (cmp!=null) quickrc(0, size()-1);
		else quickr( 0, size()-1); 
		}

	protected void quickrc( int min, int max) 
 	{
		if (min < max) {
	     int lo = min; 
	     int hi = max;  
	     do {
	       while (lo < hi && cmp.compare( lo, max) <= 0) lo++;
	       while (hi > lo && cmp.compare( hi, max) >= 0) hi--;  
	       if (lo < hi) swap( lo, hi);
	     } while (lo < hi);
	     swap( lo, max);
	     if (lo - min < max - lo) {
	       quickrc( min, lo-1);
	       quickrc( lo+1, max);
	       }
	     else {
	       quickrc( lo+1, max);
	       quickrc( min, lo-1);
	    	}
			}
	}
	
	protected void quickr( int min, int max) 
 	{
		if (min < max) {
	     int lo = min; 
	     int hi = max;  
	     do {
	       while (lo < hi && compare( lo, max) <= 0) lo++;
	       while (hi > lo && compare( hi, max) >= 0) hi--;  
	       if (lo < hi) swap( lo, hi);
	     } while (lo < hi);
	     swap( lo, max);
	     if (lo - min < max - lo) {
	       quickr( min, lo-1);
	       quickr( lo+1, max);
	       }
	     else {
	       quickr( lo+1, max);
	       quickr( min, lo-1);
	    	}
			}
	}
    
}


