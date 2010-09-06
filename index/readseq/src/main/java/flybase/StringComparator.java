//flybase/StringComparator.java
//split4javac// flybase/SortVector.java date=04-Sep-1999

// dclap/util/sortvector.java


package flybase; //.util ?

import java.util.Vector;
import java.util.Enumeration;


//split4javac// flybase/SortVector.java line=22
public class StringComparator implements ObjectComparator 
{
	public int compareObjects( Object a, Object b) {
		return a.toString().compareTo( b.toString()); //? or cast (String) - safer or faster?
		//return ((String)a).compareTo((String)b); 
		}
}

