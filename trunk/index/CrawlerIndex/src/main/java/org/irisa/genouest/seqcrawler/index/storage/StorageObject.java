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
	
	private final static String[] jsonFields = { "metadata" , "shards", "content"};
	
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
}
