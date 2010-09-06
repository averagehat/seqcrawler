//flybase/ObjectComparator.java
//split4javac// flybase/SortVector.java date=04-Sep-1999

// dclap/util/sortvector.java


package flybase; //.util ?

import java.util.Vector;
import java.util.Enumeration;


//split4javac// flybase/SortVector.java line=18
public interface ObjectComparator {
	public int compareObjects(Object a, Object b);
}

