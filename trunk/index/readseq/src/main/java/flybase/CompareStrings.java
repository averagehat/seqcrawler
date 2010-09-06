//flybase/CompareStrings.java
//split4javac// flybase/SortVector.java date=04-Sep-1999

// dclap/util/sortvector.java


package flybase; //.util ?

import java.util.Vector;
import java.util.Enumeration;


//split4javac// flybase/SortVector.java line=34
public class CompareStrings implements ComparableVector 
{
	// a basic implementation
  FastVector fv;
  public CompareStrings(FastVector stringVector) { this.fv= stringVector; }
	public int compareAt(int a, int b) {
		String sa= (String) fv.elementAt(a);
		String sb= (String) fv.elementAt(b);
		return sa.compareTo(sb);
		}
};

 
