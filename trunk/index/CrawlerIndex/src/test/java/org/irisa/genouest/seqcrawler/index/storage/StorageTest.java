package org.irisa.genouest.seqcrawler.index.storage;

import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.irisa.genouest.seqcrawler.index.Constants.STORAGEIMPL;
import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;
import org.irisa.genouest.seqcrawler.tools.ToolsTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			 storageMngr.setArgs(map);
			 log.info("Using host "+host);
		 }
		 StorageManagerInterface storage = storageMngr.get(STORAGEIMPL.RIAK);

		 StorageObject stObj = new StorageObject();
		  stObj.setId("sampleid");
		  HashMap<String,String> list = new HashMap<String,String>();
		  list.put("meta1", "value1");
		  list.put("meta2", "value2");  
		  stObj.setMetadata(list);
		  String content = "abcde";
		  stObj.setContent(content);
		  try {
			storage.store(stObj);
			log.info("object stored");
			StorageObject storedObject = storage.get(stObj.id());
			log.info("got object back");
			assertEquals(storedObject.getContent(),stObj.getContent());
		  } catch (StorageException e) {
			  log.error("Storage error: "+e.getMessage());
			fail(e.getMessage());
		  }
		  finally {
			  log.info("delete object");
			  storage.delete(stObj.id());
		  }
	 }

}
