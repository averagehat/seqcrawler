//flybase/ComparableVector.java
//split4javac// flybase/SortVector.java date=04-Sep-1999

// dclap/util/sortvector.java


package flybase; //.util ?

import java.util.Vector;
import java.util.Enumeration;


//split4javac// flybase/SortVector.java line=30
public interface ComparableVector {
	public int compareAt(int a, int b);
}

