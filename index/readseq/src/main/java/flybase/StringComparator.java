package flybase;

public class StringComparator implements ObjectComparator 
{
	public int compareObjects( Object a, Object b) {
		return a.toString().compareTo( b.toString()); //? or cast (String) - safer or faster?
		//return ((String)a).compareTo((String)b); 
		}
}