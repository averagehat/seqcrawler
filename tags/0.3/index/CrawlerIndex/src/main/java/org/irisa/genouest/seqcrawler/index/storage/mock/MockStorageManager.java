package org.irisa.genouest.seqcrawler.index.storage.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;
import org.irisa.genouest.seqcrawler.index.storage.StorageManagerInterface;
import org.irisa.genouest.seqcrawler.index.storage.StorageObject;
import org.irisa.genouest.seqcrawler.tools.Tools;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockStorageManager implements StorageManagerInterface {

	static HashMap<String,String> map = new HashMap<String,String>();
	
	private Logger log = LoggerFactory.getLogger(MockStorageManager.class);
	
	public MockStorageManager(Map<String, String> args) {
		
	}
	
	public void delete(String key) throws StorageException {
		map.remove(key);
	}

	public void deleteAll(String key) throws StorageException {
		map.remove(key);
	}

	public StorageObject get(String key) throws StorageException {
		String content = map.get(key);
		StorageObject object = new StorageObject();
		object.setId(key);
		JSONObject json;
		try {
			json = new JSONObject(content);
			JSONObject metadataJson = json.getJSONObject("metadata");
			Map<String,String> metadata = Tools.Json2Map(metadataJson);
			object.setMetadata(metadata);
			JSONArray shardsJson = json.getJSONArray("shards");
			List<String> shards = Tools.Json2List(shardsJson);
			object.setShards(shards);
			object.setContent((String)json.get("content"));
		} catch (JSONException e) {
			log.error("Could not map json "+content);
			throw new StorageException(e.getMessage());
		}

		

		return object;
	}

	public List<String> getKeys() throws StorageException {
		Set<String> keys = map.keySet();
		List<String> list = new ArrayList<String>();
		for(String key : keys) {
			list.add(key);
		}
		return list;
	}

	public void setBucket(String bucket) {
		return;
	}

	public void store(StorageObject object) throws StorageException {
		map.put(object.id(), object.toString());

	}

}
