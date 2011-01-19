//flybase/OpenString.java
//split4javac// flybase/OpenString.java date=24-Mar-2001

// flybase.store.OpenString
// modified from java.lang.String to allow access to buffer and substring indices !
/* @(#)String.java	1.83 97/07/25
 * 
 * Copyright (c) 1995, 1996 Sun Microsystems, Inc. All Rights Reserved.
 */

package flybase;

import java.util.Hashtable;
import java.util.Locale;
import sun.io.ByteToCharConverter;
import sun.io.CharToByteConverter;
import java.io.CharConversionException;
import java.io.UnsupportedEncodingException;


public 
class OpenString implements java.io.Serializable {
    /** The value is used for character storage. */
    public char value[];

    /** The offset is the first index of the storage that is used. */
    public int offset;

    /** The count is the number of characters in the OpenString. */
    public int count;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -6849794470754667710L;

		private static boolean v11hash; // mar01 cure for j1.2 problems
		static { 
			String v= System.getProperty("java.version","1.0.0"); 
			v11hash= (v.startsWith("1.1") || v.startsWith("1.0")); 
			//System.err.println("OpenString java.version="+v); // tests ok
			}
		
    public OpenString() {
			value = new char[0];
    	}

    public OpenString(OpenString value) {
    	//? do we want copy or use ref to value.value ?
			this( value.offset, value.count, value.value);
			//count = value.length();
			//this.value = new char[count];
			//value.getChars(0, count, this.value, 0);
			}

    public OpenString( String value) {
			count = value.length();
			this.value = new char[count];
			value.getChars(0, count, this.value, 0);
			}

    public OpenString(char value[]) {
			this.count = value.length;
			this.value = value; //new char[count];
			//System.arraycopy(value, 0, this.value, 0, count);
    }

    public OpenString(char value[], int offset, int count) {
			if (offset < 0) {
			    throw new StringIndexOutOfBoundsException(offset);
			}
			if (count < 0) {
			    throw new StringIndexOutOfBoundsException(count);
			}
			// Note: offset or count might be near -1>>>1.
			if (offset > value.length - count) {
			    throw new StringIndexOutOfBoundsException(offset + count);
			}

			this.value = value; //new char[count];
			this.count = count;
			this.offset= offset;
			//System.arraycopy(value, offset, this.value, 0, count);
    }


    public OpenString(byte ascii[], int hibyte, int offset, int count) {
			if (offset < 0) {
			    throw new StringIndexOutOfBoundsException(offset);
			}
			if (count < 0) {
			    throw new StringIndexOutOfBoundsException(count);
			}
			// Note: offset or count might be near -1>>>1.
			if (offset > ascii.length - count) {
			    throw new StringIndexOutOfBoundsException(offset + count);
			}

			char value[] = new char[count];
			this.count = count;
			this.value = value;

			if (hibyte == 0) {
			    for (int i = count ; i-- > 0 ;) {
				value[i] = (char) (ascii[i + offset] & 0xff);
			    }
			} else {
			    hibyte <<= 8;
			    for (int i = count ; i-- > 0 ;) {
				value[i] = (char) (hibyte | (ascii[i + offset] & 0xff));
			    }
			}
    }

    public OpenString(byte ascii[], int hibyte) {
			this(ascii, hibyte, 0, ascii.length);
    }

    protected OpenString(byte bytes[], int offset, int length,
		   ByteToCharConverter btc)
    {
			int estCount = btc.getMaxCharsPerByte() * length;
			value = new char[estCount];

		        try {
			    count = btc.convert(bytes, offset, offset+length,
						value, 0, estCount);
			    count += btc.flush(value, btc.nextCharIndex(), estCount);
			}
			catch (CharConversionException x) {
			    count = btc.nextCharIndex();
			}

			if (count < estCount) {
			    // A multi-byte format was used:  Trim the char array.
			    char[] trimValue = new char[count];
			    System.arraycopy(value, 0, trimValue, 0, count);
			    value = trimValue;
			}
    }

    public OpenString(byte bytes[], int offset, int length, String enc)
			throws UnsupportedEncodingException
    {
			this(bytes, offset, length, ByteToCharConverter.getConverter(enc));
    }

    public OpenString(byte bytes[], String enc)
			throws UnsupportedEncodingException
    {
			this(bytes, 0, bytes.length, enc);
    }

    public OpenString(byte bytes[], int offset, int length) {
			this(bytes, offset, length, ByteToCharConverter.getDefault());
    	}

    public OpenString(byte bytes[]) {
			this(bytes, 0, bytes.length, ByteToCharConverter.getDefault());
   	 	}

    /*public OpenString (StringBuffer buffer) { 
			synchronized(buffer) { 
			    buffer.setShared();
			    this.value = buffer.getValue();
			    this.offset = 0;
			    this.count = buffer.length();
			}
    }*/
    
    // (formerly) Private constructor which shares value array for speed.
    public OpenString(int offset, int count, char value[]) {
			this.value = value;
			this.offset = offset;
			this.count = count;
	    }

    public final int length() { return count; }

				// accessors for formerly private storage - dgg
		public char[] getValue() { return value; }
		public void setValue(char[] val) { value= val; }
		public final int getOffset() { return offset; }
		public final void setOffset( int off) { offset= off; }
		public final void setLength( int len) { count= len; }
		
			// hacky - change our data in midstream...
    public  void setCharAt(int index, char toc) {
			if ((index < 0) || (index >= count)) {
			    throw new StringIndexOutOfBoundsException(index);
			}
			value[index + offset]= toc;
    }

    public char charAt(int index) {
			if ((index < 0) || (index >= count)) {
			    throw new StringIndexOutOfBoundsException(index);
			}
			return value[index + offset];
    }

    public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
			if (srcBegin < 0) {
			    throw new StringIndexOutOfBoundsException(srcBegin);
			} 
			if (srcEnd > count) {
			    throw new StringIndexOutOfBoundsException(srcEnd);
			} 
			if (srcBegin > srcEnd) {
			    throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
			}
			System.arraycopy(value, offset + srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }


    public void getBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin) {
			if (srcBegin < 0) {
			    throw new StringIndexOutOfBoundsException(srcBegin);
			} 
			if (srcEnd > count) {
			    throw new StringIndexOutOfBoundsException(srcEnd);
			} 
			if (srcBegin > srcEnd) {
			    throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
			}
		 	int j = dstBegin;
		 	int n = offset + srcEnd;
		 	int i = offset + srcBegin;
		 	while (i < n) {
		 	    dst[j++] = (byte)value[i++];
		 	}
    }

    protected byte[] getBytes(CharToByteConverter ctb) {
			ctb.reset();
			int estLength = ctb.getMaxBytesPerChar() * count;
			byte[] result = new byte[estLength];
			int length;

			try {
			    length = ctb.convert(value, offset, offset + count,
						 result, 0, estLength);
			    length += ctb.flush(result, ctb.nextByteIndex(), estLength);
			} catch (CharConversionException e) {
			    length = ctb.nextByteIndex();
			}

			if (length < estLength) {
			    // A short format was used:  Trim the byte array.
			    byte[] trimResult = new byte[length];
			    System.arraycopy(result, 0, trimResult, 0, length);
			    return trimResult;
			}
			else {
			    return result;
			}
    }

    public final byte[] getBytes(String enc)
			throws UnsupportedEncodingException
    {
			return getBytes(CharToByteConverter.getConverter(enc));
    }

    public final byte[] getBytes() {
			return getBytes(CharToByteConverter.getDefault());
    }

    public boolean equals(Object anObject) {
    	if (anObject == null) return false;
    	if (anObject instanceof String) anObject= new OpenString((String)anObject);//?
			if (anObject instanceof OpenString) {
			    OpenString anotherString = (OpenString)anObject;
			    int n = count;
			    if (n == anotherString.count) {
						char v1[] = value;
						char v2[] = anotherString.value;
						int i = offset;
						int j = anotherString.offset;
						while (n-- != 0) {
						    if (v1[i++] != v2[j++]) {
							return false;
						    }
						}
						return true;
			    }
			}
			return false;
    }

    public final boolean equalsIgnoreCase(OpenString anotherString) {
			return (anotherString != null) && (anotherString.count == count) &&
				regionMatches(true, 0, anotherString, 0, count);
    }
    
    public final boolean equalsIgnoreCase( String anotherString) {
			return (anotherString != null) && (anotherString.length() == count) &&
				regionMatches(true, 0, anotherString, 0, count);
    }

    public int compareTo(OpenString anotherString) {
			int len1 = count;
			int len2 = anotherString.count;
			int n = Math.min(len1, len2);
			char v1[] = value;
			char v2[] = anotherString.value;
			int i = offset;
			int j = anotherString.offset;

			while (n-- != 0) {
			    char c1 = v1[i++];
			    char c2 = v2[j++];
			    if (c1 != c2) {
				return c1 - c2;
			    }
			}
			return len1 - len2;
    }

			// dgg added
    public int compareTo(OpenString anotherString, boolean ignoreCase) {
			int len1 = count;
			int len2 = anotherString.count;
			int n = Math.min(len1, len2);
			char v1[] = value;
			char v2[] = anotherString.value;
			int i = offset;
			int j = anotherString.offset;

			while (n-- != 0) {
			    char c1 = v1[i++];
			    char c2 = v2[j++];
			    if (ignoreCase) {
			    	c1= Character.toUpperCase(c1);
			    	c2= Character.toUpperCase(c2);
			    	}
			    if (c1 != c2)  return c1 - c2;
				}
			return len1 - len2;
    }
    
    public int dictionaryCompareTo(OpenString anotherString, boolean ignoreCase) {
    	// ignore non-alphanums
			int len1 = count;
			int len2 = anotherString.count;
			int n = Math.min(len1, len2);
			char v1[] = value;
			char v2[] = anotherString.value;
			int i = offset;
			int j = anotherString.offset;

			while (n-- != 0) {
			    char c1 = 0; 
			    while (i<len1 && !Character.isLetterOrDigit(c1)) c1= v1[i++];
			    char c2 = 0; 
			    while (j<len2 && !Character.isLetterOrDigit(c2)) c2= v2[j++];
			    
			    if (ignoreCase) {
			    	c1= Character.toUpperCase(c1);
			    	c2= Character.toUpperCase(c2);
			    	}
			    if (c1 != c2)  return c1 - c2;
				}
			return len1 - len2;
    }


    public boolean regionMatches(int toffset, OpenString other, int ooffset, int len) {
			char ta[] = value;
			int to = offset + toffset;
			int tlim = offset + count;
			char pa[] = other.value;
			int po = other.offset + ooffset;
			// Note: toffset, ooffset, or len might be near -1>>>1.
			if ((ooffset < 0) || (toffset < 0) || (toffset > count - len) || (ooffset > other.count - len)) {
			    return false;
			}
			while (len-- > 0) {
			    if (ta[to++] != pa[po++]) {
			        return false;
			    }
			}
			return true;
    }

    public boolean regionMatches(int toffset, String other, int ooffset, int len) {
			int otherlen= other.length();
			// Note: toffset, ooffset, or len might be near -1>>>1.
			if ((ooffset < 0) || (toffset < 0) || (toffset > count - len) || (ooffset > otherlen - len)) {
			    return false;
			}
			//char pa[] = other.value;
			char ta[] = value;
			int to = offset + toffset;
			int po = ooffset;
			while (len-- > 0) {
			    if (ta[to++] != other.charAt(po++)) {
			        return false;
			    }
			}
			return true;
    }
    
    
    public boolean regionMatches(boolean ignoreCase,
				         int toffset, OpenString other, int ooffset, int len) {
			char ta[] = value;
			int to = offset + toffset;
			int tlim = offset + count;
			char pa[] = other.value;
			int po = other.offset + ooffset;
			// Note: toffset, ooffset, or len might be near -1>>>1.
			if ((ooffset < 0) || (toffset < 0) || (toffset > count - len) || (ooffset > other.count - len)) {
			    return false;
			}
			while (len-- > 0) {
			    char c1 = ta[to++];
			    char c2 = pa[po++];
			    if (c1 == c2)
				continue;
			    if (ignoreCase) {
				// If characters don't match but case may be ignored,
				// try converting both characters to uppercase.
				// If the results match, then the comparison scan should
				// continue. 
				char u1 = Character.toUpperCase(c1);
				char u2 = Character.toUpperCase(c2);
				if (u1 == u2)
				    continue;
				// Unfortunately, conversion to uppercase does not work properly
				// for the Georgian alphabet, which has strange rules about case
				// conversion.  So we need to make one last check before 
				// exiting.
				if (Character.toLowerCase(u1) == Character.toLowerCase(u2))
				    continue;
			    }
			    return false;
			}
			return true;
    }

    public boolean regionMatches(boolean ignoreCase,
				         int toffset, String other, int ooffset, int len) {
			// Note: toffset, ooffset, or len might be near -1>>>1.
			if ((ooffset < 0) || (toffset < 0) || (toffset > count - len) || (ooffset > other.length() - len)) {
			    return false;
			}
			char ta[] = value;
			int to = offset + toffset;
			int tlim = offset + count;
			//char pa[] = other.value;
			int po = ooffset; // other.offset + 
			while (len-- > 0) {
			    char c1 = ta[to++];
			    char c2 = other.charAt(po++); //pa[po++];
			    if (c1 == c2)
				continue;
			    if (ignoreCase) {
				// If characters don't match but case may be ignored,
				// try converting both characters to uppercase.
				// If the results match, then the comparison scan should
				// continue. 
				char u1 = Character.toUpperCase(c1);
				char u2 = Character.toUpperCase(c2);
				if (u1 == u2)
				    continue;
				// Unfortunately, conversion to uppercase does not work properly
				// for the Georgian alphabet, which has strange rules about case
				// conversion.  So we need to make one last check before 
				// exiting.
				if (Character.toLowerCase(u1) == Character.toLowerCase(u2))
				    continue;
			    }
			    return false;
			}
			return true;
    }
 
		public boolean startsWith(OpenString prefix, int toffset) {
			char ta[] = value;
			int to = offset + toffset;
			int tlim = offset + count;
			char pa[] = prefix.value;
			int po = prefix.offset;
			int pc = prefix.count;
			// Note: toffset might be near -1>>>1.
			if ((toffset < 0) || (toffset > count - pc)) {
			    return false;
			}
			while (--pc >= 0) {
			    if (ta[to++] != pa[po++]) {
			        return false;
			    }
			}
			return true;
    }


		public final boolean startsWith( OpenString prefix) {
			return startsWith(prefix, 0);
    }
    
 		public final boolean startsWith( String prefix, int toffset) {
			return startsWith( new OpenString(prefix),toffset);
    }
    
		public final boolean startsWith( String prefix) {
			return startsWith( new OpenString(prefix), 0);
    }

 
		public final boolean endsWith( OpenString suffix) {
			return startsWith(suffix, count - suffix.count);
    }


		transient int hashcode;
		
    public int hashCode() {
    	if (hashcode!=0) return hashcode;
			if (v11hash) hashcode= hashCode11(); else hashcode= hashCode12();
			return hashcode;
		}
		
    // java 1.1 String.hashCode()
    public int hashCode11() {
    	//if (hashcode!=0) return hashcode;
			int h = 0;
			int off = offset;
			char val[] = value;
			int len = count;

			if (len < 16) {
		 	    for (int i = len ; i > 0; i--) {
		 		h = (h * 37) + val[off++];
		 	    }
		 	} else {
		 	    // only sample some characters
		 	    int skip = len / 8;
		 	    for (int i = len ; i > 0; i -= skip, off += skip) {
		 		h = (h * 39) + val[off];
		 	    }
		 	}
			//hashcode= h;
			return h;
    }
				// java 1.2 hashcode - better than skipping chars as j1.1 version 
				// need compatibility w/ 1.1 and 1.2 :   String.hashCode == OpenString.hashCode
   public int hashCode12() {
    	//if (hashcode!=0) return hashcode;
			int h = 0;
			int off = offset;
			char val[] = value;
			int len = count;
			for (int i = 0; i < len; i++) h = 31*h + val[off++];
			//hashcode= h;
			return h;
    }

 
		public final int indexOf(int ch) {
			return indexOf(ch, 0);
    }


    public int indexOf(int ch, int fromIndex) {
			int max = offset + count;
			char v[] = value;

			if (fromIndex < 0) {
			    fromIndex = 0;
			} else if (fromIndex >= count) {
			    // Note: fromIndex might be near -1>>>1.
			    return -1;
			}
			for (int i = offset + fromIndex ; i < max ; i++) {
			    if (v[i] == ch) {
				return i - offset;
			    }
			}
			return -1;
    }


    public final int lastIndexOf(int ch) {
			return lastIndexOf(ch, count - 1);
    }


    public int lastIndexOf(int ch, int fromIndex) {
			int min = offset;
			char v[] = value;
			
			for (int i = offset + ((fromIndex >= count) ? count - 1 : fromIndex) ; i >= min ; i--) {
			    if (v[i] == ch) {
				return i - offset;
			    }
			}
			return -1;
    }


    public final int indexOf( String str) {
			return indexOf(str, 0);
    }

    public final int indexOf( String str, int fromIndex) {
			return indexOf( new OpenString(str), fromIndex);
			}

    public final int indexOf( OpenString str) {
			return indexOf(str, 0);
    }
			
    public int indexOf( OpenString str, int fromIndex) {
    	char v1[] = value;
    	char v2[] = str.value;
    	int max = offset + (count - str.count);
    	if (fromIndex < 0) {
    	    fromIndex = 0;
    	} else if (fromIndex >= count) {
    	    /* Note: fromIndex might be near -1>>>1 */
    	    return -1;
    	}
			if (str.count == 0) {
			    return fromIndex;
			}

    	int str_offset = str.offset;
        char first  = v2[str_offset];
        int i = offset + fromIndex;
    test:
        while (true) {

	    /* Look for first character */
	    while (i <= max && v1[i] != first) i++;
	    if (i > max) return -1;

	    /* Found first character, now look for remainder of v2 */
	    int j = i + 1;
	    int end = j + str.count - 1;
	    int k = str_offset + 1;
	    while (j < end) {
				if (v1[j++] != v2[k++]) {
				    i++;
				    continue test;
				}
			    }

	    return i - offset;	/* Found whole string */
        }
    }


    public final int lastIndexOf( String str) {
			return lastIndexOf(str, count);
    }

    public final int lastIndexOf( String str, int fromIndex) {
			return lastIndexOf( new OpenString(str), fromIndex);
    }

    public final int lastIndexOf( OpenString str) {
			return lastIndexOf(str, count);
    }

    public int lastIndexOf( OpenString str, int fromIndex) {

			/* Check arguments; return immediately where possible.
			 * Deliberately not checking for null str, to be consistent with
			 * with other OpenString methods.
			 */
			if (fromIndex < 0) {
			    return -1;
			} else if (fromIndex > count - str.count) {
			    fromIndex = count - str.count;
			}

			/* Empty string always matches. */
			if (str.count == 0) {
			    return fromIndex;
			}

			/* Find the rightmost substring match. */
			char v1[] = value;
			char v2[] = str.value;

			for (int i = offset + fromIndex; i >= offset; --i) {
			    int n = str.count;
			    int thisIndex = i;
			    int strIndex = str.offset;
			    while (v1[thisIndex++] == v2[strIndex++]) {
				if (--n <= 0) {
				    return i - offset;
				}
			    }
			}
			return -1;
    }


    public final OpenString substring(int beginIndex) {
			return substring(beginIndex, length());
    }


    public OpenString substring(int beginIndex, int endIndex) {
			if (beginIndex < 0) {
			    throw new StringIndexOutOfBoundsException(beginIndex);
			} 
			if (endIndex > count) {
			    throw new StringIndexOutOfBoundsException(endIndex);
			}
			if (beginIndex > endIndex) {
			    throw new StringIndexOutOfBoundsException(endIndex - beginIndex);
			}
			return ((beginIndex == 0) && (endIndex == count)) ? this :
			    new OpenString(offset + beginIndex, endIndex - beginIndex, value);
    }


    public OpenString concat(OpenString str) {
			int otherLen = str.length();
			if (otherLen == 0) {
			    return this;
			}
			char buf[] = new char[count + otherLen];
			getChars(0, count, buf, 0);
			str.getChars(0, otherLen, buf, count);
			return new OpenString(0, count + otherLen, buf);
    }


    public OpenString replace(char oldChar, char newChar) {
			if (oldChar != newChar) {
			    int len = count;
			    int i = -1;
			    while (++i < len) {
				if (value[offset + i] == oldChar) {
				    break;
				}
			    }
			    if (i < len) {
				char buf[] = new char[len];
				for (int j = 0 ; j < i ; j++) {
				    buf[j] = value[offset+j];
				}
				while (i < len) {
				    char c = value[offset + i];
				    buf[i] = (c == oldChar) ? newChar : c;
				    i++;
				}
				return new OpenString(0, len, buf);
			    }
			}
			return this;
    }

    public void lowerCase() {
			hashcode= 0;
      int i= count;  
      while ( i-- > 0)
     		value[offset+i]= Character.toLowerCase(value[offset+i]);
	    }

    public void upperCase() {
			hashcode= 0;
      int i= count;  
      while ( i-- > 0)
     		value[offset+i]= Character.toUpperCase(value[offset+i]);
	    }

    public String toLowerCase() {
        StringBuffer result = new StringBuffer();
        int i;
        int len = count;
        for (i = 0; i < len; ++i) {
            result.append(Character.toLowerCase(value[offset+i]));
        }
        return result.toString();
    }

    public String toUpperCase() {
        StringBuffer result = new StringBuffer();
        int i;
        int len = count;
        for (i = 0; i < len; ++i) {
            result.append(Character.toUpperCase(value[offset+i]));
        }
        
        return result.toString();
    }

    public OpenString trim() {
			int len = count;
			int st = 0;
			while ((st < len) && (value[offset + st] <= ' ')) {
			    st++;
			}
			while ((st < len) && (value[offset + len - 1] <= ' ')) {
			    len--;
			}
			return ((st > 0) || (len < count)) ? substring(st, len) : this;
    }


    public final String toString() {
			return new String( value, offset, count);
    }


    public char[] toCharArray() {
			int i, max = length();
			char result[] = new char[max];
			getChars(0, max, result, 0);
			return result;
    }


    public final static OpenString valueOf(char data[]) {
			return new OpenString( 0, data.length, data);//? don't copy but store?
    }


    public final static OpenString valueOf(char data[], int offset, int count) {
			return new OpenString(offset, count, data);//? don't copy but store?
    }
    

    public final static OpenString copyValueOf(char data[], int offset, int count) {
			// All public OpenString constructors now copy the data.
			return new OpenString(data, offset, count);
    }


    public final static OpenString copyValueOf(char data[]) {
			return copyValueOf(data, 0, data.length);
    }


   /*
    public static String valueOf(Object obj) {
			return (obj == null) ? "null" : obj.toString();
    }

    public static String valueOf(boolean b) {
			return b ? "true" : "false";
    }

    public static String valueOf(char c) {
			char data[] = {c};
			return new String( data, 0, 1);
    }

    public static String valueOf(int i) {
      return Integer.toString(i, 10);
    }

    public static String valueOf(long l) {
        return Long.toString(l, 10);
    }

    public static String valueOf(float f) {
			return Float.toString(f);
    }


    public static String valueOf(double d) {
			return Double.toString(d);
    }
		*/
 
 		//public native String intern(); 

    //int utfLength() {}

}
//split4javac// flybase/OpenString.java line=872

