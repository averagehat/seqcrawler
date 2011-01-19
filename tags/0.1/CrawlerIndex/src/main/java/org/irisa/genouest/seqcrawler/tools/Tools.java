package org.irisa.genouest.seqcrawler.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils class
 */
public class Tools {

	private static Logger log = LoggerFactory.getLogger(Tools.class);
	
	/**
	 * Calculate size based on input description.
	 * Valid values (integers) are:
	 * <ul><li>10</li>
	 * <li>10k e.g. 10.000</li>
	 * <li>10m e.g. 10 mega</li>
	 * <li>10g e.g. 10 giga</li>
	 * </ul>
	 * @param size input description
	 * @return result size
	 * @throws Exception
	 */
	public static long getSize(String size) throws Exception {
		long value = 0;
		if(size==null || size.length()==0) throw new Exception("Size value is null");
		char last = size.charAt(size.length()-1);
		if(Character.isLetter(last)) {
			String lastChar =String.valueOf(last);
			String initialValue = size.substring(0, size.length()-1);
			try {
				value = Long.valueOf(initialValue);
				}
				catch(NumberFormatException e) {
					throw new Exception("Error, cannot interpret size "+initialValue);
				}
			if(lastChar.equalsIgnoreCase("k")) {
				value = value*1000;
			}
			else if(lastChar.equalsIgnoreCase("m")) {
				value = value*1000*1000;
			}
			else if(lastChar.equalsIgnoreCase("g")) {
				value = value*1000*1000*1000;
			}
			else throw new Exception("Error cannot interpret last character value "+lastChar+". Expected value is k,m,g");
			
		}
		else {
			try {
			value = Long.valueOf(size);
			}
			catch(NumberFormatException e) {
				throw new Exception("Error, cannot interpret size "+size);
			}
		}
		
		return value;
	}

	/**
	 * Convert a Json structure to a Java MAP
	 * @param metadataJson Input json such as key : value , key : value , ...
	 * @return The corresponding map
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> Json2Map(JSONObject metadataJson) {
		HashMap<String,String> map = new HashMap<String,String>();
		if(metadataJson==null || metadataJson.length()==0) return null;
		Iterator<String> it = metadataJson.keys();
		while(it.hasNext()) {
			String key = (String) it.next();
			try {
				map.put(key, metadataJson.getString(key));
			} catch (JSONException e) {
				log.error(e.getMessage());
			}			
		}
		return map;
	}

	/**
	 * Converts a JSON array to a Java List
	 * @param shardsJson Input array
	 * @return List of String from array
	 */
	public static List<String> Json2List(JSONArray shardsJson) {
		ArrayList<String> list = new ArrayList<String>();
		if(shardsJson==null || shardsJson.length()==0) return null;
		for(int i=0;i<shardsJson.length();i++) {
			try {
				list.add(shardsJson.getString(i));
			} catch (JSONException e) {
				log.error(e.getMessage());
			}
		}
		return list;
	}
}
