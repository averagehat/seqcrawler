package flybase;

import java.util.Enumeration;

public class SortedEnumeration {
	  FastVector vec;
	  ObjectComparator cmp;

		public SortedEnumeration( Enumeration en) {
			this( en, new StringComparator());
			}
			
		public SortedEnumeration( Enumeration en, ObjectComparator cmp) {
			this.vec= new FastVector();
			while (en.hasMoreElements()) vec.addElement( en.nextElement());
			resort( cmp);
	    }

		public FastVector vector() { return vec; }
		public Enumeration elements() { return vec.elements(); }
		
		public void resort( ObjectComparator cmp )	{
			this.cmp= cmp;
			quickr( 0, vec.size()-1);
			}
					
		protected final int compare(int a, int b) {
			//if (a < 0 || a >= vec.size()) return -1;
			//if (b < 0 || b >= vec.size()) return 1;
			return cmp.compareObjects( vec.elementAt(a), vec.elementAt(b));
			}

		protected final void swap(int a, int b) {
			//if (a < 0 || a >= vec.size() || b < 0 || b >= vec.size()) return;
			Object aob= vec.elementAt(a);
			vec.setElementAt(vec.elementAt(b), a);
			vec.setElementAt(aob, b);
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
