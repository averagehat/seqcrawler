// dclap/util/sortvector.java


package flybase; //.util ?

import java.util.Vector;


public abstract class SortVector  {
	protected Vector v;
	protected FastVector fv;
	protected ComparableVector cv;
	
	public SortVector(Vector v, ComparableVector cv) { 
		this.v= v; this.cv= cv;
		}
	public SortVector(FastVector v, ComparableVector cv) { 
		this.fv= v; this.cv= cv;
		}

  abstract public void sort();

	public void setVector(Vector v, ComparableVector cv) { 
		this.v= v; this.cv= cv;
		}
	public void setVector(FastVector v, ComparableVector cv) { 
		this.fv= v; this.cv= cv;
		}
		
	protected final int size() { return (fv!=null)?fv.size():v.size(); }

/**
	public int compare( CompareTo a, CompareTo b) {
		return a.compareTo(b);
			// subclass this
		if (a.hashCode() < b.hashCode()) return -1;
		if (a.hashCode() > b.hashCode()) return 1;
		return 0;	
		}
**/
		
	protected final int compare(int a, int b) {
		if (a < 0 || a >= size()) return -1;
		if (b < 0 || b >= size()) return 1;
		return cv.compareAt( a, b);
		//return compare( (CompareTo)v.elementAt(a), (CompareTo)v.elementAt(b));
		}
		
	protected void swap(int a, int b) {
		if (a < 0 || a >= size()
		 || b < 0 || b >= size()) 
		   return;
		if (fv!=null) {
			Object aob= fv.elementAt(a);
			Object bob= fv.elementAt(b);
			fv.setElementAt( bob, a);
			fv.setElementAt( aob, b);
			}
		else {
			Object aob= v.elementAt(a);
			Object bob= v.elementAt(b);
			v.setElementAt( bob, a);
			v.setElementAt( aob, b);
			}
		}
		
}



