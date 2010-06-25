package org.irisa.genouest.seqcrawler.index.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StorageObject {
	
	private Logger log = LoggerFactory.getLogger(StorageObject.class);
	
	//private final static String[] jsonFields = { "metadata" , "shards", "content"};
	
	/**
	 * Unique id of the element
	 */
	protected String id = null;
	

	public void setId(String id) {
		this.id = id;
	}
	
	public String id() {
		return id;
	}
	
	/**
	 * Object metadata to store with the object content
	 */
	Map<String,String> metadata= new HashMap<String,String>();

	public Map<String,String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String,String> metadata) {
		this.metadata = metadata;
	}
	
	/**
	 * Shard ids of the object when the object is splitted
	 */
	List<String> shards = new ArrayList<String>();

	public List<String> getShards() {
		return shards;
	}

	public void setShards(List<String> shards) {
		this.shards = shards;
	}

	/**
	 * Raw content to be stored	
	 */
	String content=null;
	
	
	
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public StorageObject() {
		
	}
	
	@Override
	public String toString() {
		JSONObject json = new JSONObject(this);
		return json.toString();
	}

	public StorageObject[] split(long max) {
		int length = this.content.length();
		if(this.content.length()<max) { 
			metadata.put("start", "0");
			metadata.put("stop",String.valueOf(length));
			return new StorageObject[] { this };
		}
		// If size > max, split current
		
		int nbshards = (int) Math.ceil(length / max) ;
		StorageObject[] list = new StorageObject[nbshards];
		int start = 0;
		int size = (int) (length/nbshards);
		log.debug("Split in "+nbshards+" of size "+size);
		for(int i=0;i<nbshards;i++) {
			int end = start+size;
			if(end>length) end=length-1;
			String shardContent = content.substring(start, end);
			log.debug("CONTENT: "+shardContent);
			StorageObject object = new StorageObject();
			metadata.put("start", String.valueOf(start));
			metadata.put("stop",String.valueOf(end));
			if(i==0) {
				// For first shard, keep original name
				object.setId(id);
				object.setMetadata(metadata);
				// Set all shards
				for(int s=1;s<nbshards;s++) {
					this.shards.add(id+".shard"+s);
				}
				object.setShards(shards);
			}
			else {
				object.setMetadata(metadata);
				object.setId(id+".shard"+i);
			}
			object.setContent(shardContent);
			list[i] = object;
			start+= size;		
		}
		
		return list;
	}
}
