package flybase;

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