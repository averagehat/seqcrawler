package org.irisa.genouest.seqcrawler.index.storage.riak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;
import org.irisa.genouest.seqcrawler.index.storage.StorageManagerInterface;
import org.irisa.genouest.seqcrawler.index.storage.StorageObject;
import org.irisa.genouest.seqcrawler.tools.Tools;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.basho.riak.client.RiakBucketInfo;
import com.basho.riak.client.RiakClient;
import com.basho.riak.client.RiakObject;
import com.basho.riak.client.response.BucketResponse;
import com.basho.riak.client.response.FetchResponse;
import com.basho.riak.client.response.StoreResponse;

/**
 * Storage implementation on a RIAK backend
 * <br/>Riak Quick start<br/>
 *
 * <p>Connect to Riak:</p>
 *
 *  <p> RiakClient riak = new RiakClient("http://localhost:8098/riak"); </p>
 *
 * <p>Build an object:</p>
 *
 * <p>  RiakObject o = new RiakObject("bucket", "key", "value");</p>
 *
 * <p>Store it:</p>
 *
 * <p>    riak.store(o);</p>
 *
 * <p>Retrieve it:</p>
 *
 * <p>   FetchResponse r = riak.fetch("bucket", "key");</p>
 * <p>   if (r.hasObject())</p>
 * <p>       o = r.getObject();</p>
 *
 * <p>Update it:</p>
 *
 * <p>   o.setValue("foo");</p>
 * <p>   riak.store(o);</p>
 * 
 * @author osallou
 *
 */
public class RiakManager implements StorageManagerInterface{

	private Logger log = LoggerFactory.getLogger(RiakManager.class);
	
	private static final String BUCKET = "bank";
	
	
	public static String getBucket() {
		return BUCKET;
	}

	private String host = "localhost";
	private String port = "8098";
	
	/**
	 * Maximum size of shards
	 * TODO cut object if too large for storage
	 */
    private long max = 10000000L;
    
    RiakClient riak = null;
	
	public RiakManager(Map<String, String> args) {
		if(args==null || args.size()==0) {
			log.warn("No property set, using defaults");
		}
		for(Entry<String, String> arg: args.entrySet()) {
			if(arg.getKey().equals("host")) {
				log.debug("Setting host "+arg.getValue());
				this.host = arg.getValue();
			}
			if(arg.getKey().equals("port")) {
				log.debug("Setting port "+arg.getValue());
				this.port = arg.getValue();
			}
			if(arg.getKey().equals("max")) {
				log.debug("Setting shard max size "+arg.getValue());
				try {
					this.max = Tools.getSize(arg.getValue());
				} catch (Exception e) {
					log.info(e.getMessage()+"\nUsing defaults");
				}
			}
		}
		riak = new RiakClient("http://"+host+":"+port+"/riak");
		log.debug("New connection: "+riak.getConfig().getUrl());
	}

	
		
	/**
	 * Fetch an object from remote storage. Object may contain shards, it does not retreive
	 * the whole object content with its shards but only referenced key.
	 * @param key id of the object to retreive
	 * @return StorageObject with object data and metadata
	 */

	public StorageObject get(String key) throws StorageException {
		
		log.debug("fetch object "+key);
		StorageObject object = new StorageObject();
		FetchResponse response = riak.fetch(BUCKET, key);
		log.debug(response.getBody());
		RiakObject ro =null;
		if (response.hasObject()) {
		  ro = response.getObject();
		  String id = ro.getKey();
		  object.setId(id);
		  String value = ro.getValue();
		  try {
			JSONObject json = new JSONObject(value);
			JSONObject metadataJson = json.getJSONObject("metadata");
			Map<String,String> metadata = Tools.Json2Map(metadataJson);
			object.setMetadata(metadata);
			JSONArray shardsJson = json.getJSONArray("shards");
			List<String> shards = Tools.Json2List(shardsJson);
			object.setShards(shards);
			object.setContent((String)json.get("content"));
		} catch (JSONException e) {
			log.error("Could not map json "+value);
			throw new StorageException(e.getMessage());
		}
		
		}
		return object;
	}
	
	/**
	 * Retreives all keys of bucket
	 * @return A List with all saved keys
	 * @throws StorageException 
	 */
	public List<String> getKeys() throws StorageException {
		List<String> resultList = new ArrayList<String>();
		 BucketResponse r = riak.listBucket(BUCKET);
		    if (r.isSuccess()) {
		        RiakBucketInfo info = r.getBucketInfo();
		        Collection<String> keys = info.getKeys();
		        for(String key : keys) {
		        	log.debug("found key: "+key);
		        	resultList.add(key);
		        }
		    }
		    else {
		    	log.error("Error while retreiving keys from server");
		    	throw new StorageException("Could not retreive keys from storage");
		    }
		    return resultList;
		
	}

	/**
	 * Store an object in a remote Riak server.
	 * @param object StorageObject to store.
	 * @throws StorageException 
	 */
	public void store(StorageObject object) throws StorageException {
		StorageObject[] sobjects = object.split(max);
		 for(StorageObject sobject : sobjects) {
		 log.debug("Add "+sobject.id()+" , "+sobject.toString());
	     RiakObject ro = new RiakObject(BUCKET, sobject.id(), sobject.toString());
		 ro.setContentType("application/json");
		 StoreResponse response = riak.store(ro);
		 if(response.isError()) {
			 throw new StorageException("Error when sending data "+response.getBody());
		 }
		 else {
			 log.info("New object "+sobject.id()+" in Storage OK");
		 }
		 }
	}

	/**
	 * Deletes an object from backend ith its shards
	 */
	public void deleteAll(String key) throws StorageException {
		StorageObject object = get(key);
		delete(key);	
		log.debug("shards? "+object.getShards());
		if(object.getShards()!=null && object.getShards().size()>0) {
			for(String shardKey : object.getShards()) {
				log.debug("deleting shard "+shardKey);
				delete(shardKey);
			}
		}
		
	}
	
	/**
	 * Deletes a single object from backend
	 * @param key
	 * @throws StorageException
	 */
	public void delete(String key) throws StorageException {
		riak.delete(BUCKET, key);
	}
}
