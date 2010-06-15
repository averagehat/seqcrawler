// flybase/Debug.java
// d.g.gilbert 


package flybase;


public final class Debug {
	public static boolean isOn = true;
	static { isOn= (System.getProperty("debug")!=null); }
	protected static int val;
	public final static int val() { return val; }
	public final static void setVal(int v) { val= v; isOn= (val!=0); }
	public final static void setState(boolean turnon) { isOn= turnon; }
	public final static void print(char c) { if (isOn) System.err.print(c); }
	public final static void print(String s) { if (isOn) System.err.print(s); }
	public final static void println(String s) { if (isOn) System.err.println(s); }
	public final static void println() { if (isOn) System.err.println(); }
};
