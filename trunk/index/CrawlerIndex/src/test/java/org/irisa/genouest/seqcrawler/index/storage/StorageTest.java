package org.irisa.genouest.seqcrawler.index.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.irisa.genouest.seqcrawler.index.Constants.STORAGEIMPL;
import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;
import org.irisa.genouest.seqcrawler.index.storage.mongodb.MongoDBManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class StorageTest extends TestCase {

	private Logger log = LoggerFactory.getLogger(StorageTest.class);

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public StorageTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(StorageTest.class);
	}
	
	 public void testJsonObject() {
		  StorageObject stObj = new StorageObject();
		  stObj.setId("sampleId");
		  HashMap<String,String> list = new HashMap<String,String>();
		  list.put("meta1", "value1");
		  list.put("meta2", "value2");  
		  stObj.setMetadata(list);
		  String content = "abcde";
		  stObj.setContent(content);
		  log.info(stObj.toString());
		  try {
			JSONObject json = new JSONObject(stObj.toString());
			assertEquals((String)json.get("content"),content);
		} catch (JSONException e) {
			fail(e.getMessage());
		}
	 }
	 
	 public void testStorage() throws StorageException {
		 StorageManager storageMngr = new StorageManager();
		 String host = System.getProperty("storageHost");
		 if(host!=null) {
			 HashMap<String,String> map = new HashMap<String,String>();
			 map.put("host", host);
			 map.put("max", "10");
			 storageMngr.setArgs(map);
			 log.info("Using host "+host);
		 }
		 StorageManagerInterface storage = storageMngr.get(STORAGEIMPL.MOCK);
		 
		 StorageObject stObj = new StorageObject();
		  stObj.setId("sampleid");
		  HashMap<String,String> list = new HashMap<String,String>();
		  list.put("meta1", "value1");
		  list.put("meta2", "value2");  
		  stObj.setMetadata(list);
		  String content = "abcdeaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabba";
		  stObj.setContent(content);
		  try {
			storage.store(stObj);
			StorageObject storedObject = storage.get(stObj.id());
			assertTrue(stObj.getContent().startsWith(storedObject.getContent()));
		  } catch (StorageException e) {
			  log.error("Storage error: "+e.getMessage());
			fail(e.getMessage());
		  }
		  finally {
			  log.info("delete object");
			  storage.deleteAll(stObj.id());
		  }
	 }
	 
	 public void testMongoDBConversion() {
		 StorageObject object = new StorageObject();
	    	object.setId("sample");
	    	object.setContent("sampleData");
	    	HashMap<String, String> map = new HashMap<String,String>();
	    	map.put("key1","value1");
	    	map.put("key2","value2");
	    	object.setMetadata(map);
	    	List<String> shards = new ArrayList<String>();
	    	shards.add("shard1");
	    	shards.add("shard2");
	    	object.setShards(shards);
	    	
			DBObject mongoObject = new BasicDBObject();
			mongoObject.put("seqid", object.id());
			mongoObject.put("content", object.getContent());
			mongoObject.put("metadata", object.getMetadata());
			mongoObject.put("shards", object.getShards());			
			
			MongoDBManager manager = new MongoDBManager(null);
			
			StorageObject stobj = manager.getStorageObject(mongoObject);
			assertEquals(object.getContent(),stobj.getContent());
			assertEquals(object.getMetadata(),stobj.getMetadata());
			assertEquals(object.getShards(),stobj.getShards());
			assertEquals(object.id(),stobj.id());
	 }

}
