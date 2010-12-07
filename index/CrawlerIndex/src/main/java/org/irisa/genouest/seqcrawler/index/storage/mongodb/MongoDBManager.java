package org.irisa.genouest.seqcrawler.index.storage.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;
import org.irisa.genouest.seqcrawler.index.storage.StorageManagerInterface;
import org.irisa.genouest.seqcrawler.index.storage.StorageObject;
import org.irisa.genouest.seqcrawler.tools.Tools;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBAddress;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

/**
 * MongoDB manager
 * TODO to be tested
 * @author osallou
 *
 */
public class MongoDBManager implements StorageManagerInterface {

	private Logger log = LoggerFactory.getLogger(MongoDBManager.class);
	
	private static final String ID = "_id";
	private static final String METADATA = "metadata";
	private static final String SHARDS = "shards";
	private static final String CONTENT = "content";
	
	private String BUCKET = "bank";

	private String host = "localhost";
	private String port = "27017";
	
	private static final String DB =" seqcrawler";
	
	/**
	 * Maximum size of shards
	 * cut object if too large for storage
	 */
    private long max = 10000000L;
    
    Mongo mongo = null;
    DB db  =null;
	
    
	public MongoDBManager(Map<String, String> args) {
		if(args==null || args.size()==0) {
			log.warn("No property set, using defaults");
		}
		else {
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
		}
		
		
		DBAddress addr=null;
		try {
			addr = new DBAddress(host,port);
		} catch (UnknownHostException e) {
			log.error(e.getMessage());
			System.exit(0);
		}
		mongo = new Mongo(addr);
		
		String bucket = System.getProperty("storageBucket");
		 if(bucket!=null) {
			 log.debug("Using bucket "+bucket+" as set in system environment");
		     this.setBucket(bucket); 
		 }
		 
		 db = mongo.getDB(DB);
		
		log.debug("New connection: "+db.toString());
	}
	
	public void delete(String key) throws StorageException {
		DBCollection coll = db.getCollection(BUCKET);
		coll.remove(new BasicDBObject(ID,key));
	}

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

	public StorageObject get(String key) throws StorageException {
		DBCollection coll = db.getCollection(BUCKET);
		BasicDBObject query = new BasicDBObject();

        query.put(ID, key);

        DBCursor cur = coll.find(query);
        DBObject object = null;
        //Expect one only
        if(cur.hasNext()) {
            object = cur.next();
        }
        

		return getStorageObject(object);
	}

	public List<String> getKeys() throws StorageException {
		List<String> list = new ArrayList<String>();
		DBCollection coll = db.getCollection(BUCKET);
		DBCursor cur = coll.find();
        while(cur.hasNext()) {
            DBObject object = cur.next();
            String id = (String)object.get(ID);
            if(id!=null) {
            	list.add(id);
            }
        }
		return list;
	}

	public void setBucket(String bucket) {
		BUCKET = bucket;

	}

	public void store(StorageObject object) throws StorageException {
		StorageObject[] sobjects = object.split(max);
		 for(StorageObject sobject : sobjects) {
			log.debug("Add "+sobject.id()+" , "+sobject.toString());
			DBCollection coll = db.getCollection(BUCKET);
			BasicDBObject mongoObject = new BasicDBObject();
			mongoObject.put(ID, object.id());
			mongoObject.put(CONTENT, object.getContent());
			mongoObject.put(METADATA, object.getMetadata());
			mongoObject.put(SHARDS, object.getShards());
			coll.insert(mongoObject);
		 }

	}
	
	public StorageObject getStorageObject(DBObject mongoObject) {
		StorageObject object = new StorageObject();
		JSONObject json;
		try {
			json = new JSONObject(mongoObject.toString());
			object.setId((String)json.get(ID));
			JSONObject metadataJson = json.getJSONObject(METADATA);
			Map<String,String> metadata = Tools.Json2Map(metadataJson);
			object.setMetadata(metadata);
			JSONArray shardsJson = json.getJSONArray(SHARDS);
			List<String> shards = Tools.Json2List(shardsJson);
			object.setContent((String)json.get(CONTENT));
			object.setShards(shards);
		} catch (JSONException e) {
			log.error(e.getMessage());
			return null;
		}
		
		return object;
	}


}
