package org.irisa.genouest.seqcrawler.index.storage;

import java.util.HashMap;
import java.util.Map;

import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.storage.mock.MockStorageManager;
import org.irisa.genouest.seqcrawler.index.storage.mongodb.MongoDBManager;
import org.irisa.genouest.seqcrawler.index.storage.riak.RiakManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage Handler to send and get raw data in a remote storage.
 * Used to get a {@link StorageManagerInterface} based on storage implementation and arguments
 * <br/>Example usage:<br/>    
 *      <p>StorageManager storageMngr = new StorageManager();</p>
 *		<p>HashMap<String,String> map = new HashMap<String,String>();</p>
 *		<p>map.put("host", host);</p>
 *		<p>storageMngr.setArgs(map);</p>	 
 *		<p>StorageManagerInterface storage = storageMngr.get(STORAGEIMPL.MOCK);</p>
 * 
 * @author osallou
 * 
 */
public class StorageManager {

	private Logger log = LoggerFactory.getLogger(StorageManager.class);
	
	
	public StorageManager() {
		
	}
	
	/**
	 * Main constructor, set args as available configuration items
	 * @param args
	 */
	public StorageManager(Map<String,String> args) {
		this.args = args;
	}
	

	Map<String,String> args = new HashMap<String,String>();
	
	public void setArgs(Map<String,String> args) {
		this.args = args;
	}

	/**
	 * Get a storage handler based on selected implementation
	 * @param storageImpl Selected implementation
	 * @return Interface on a storage manager
	 */
	public StorageManagerInterface get(Constants.STORAGEIMPL storageImpl) {
		StorageManagerInterface manager = null;
		switch (storageImpl) {
		case RIAK: {
			manager = new RiakManager(args);
			break;

		}
		case CASSANDRA: {
			//TODO add cassandra implementation
			log.error("Cassandra implementation is not yet available!");
			System.exit(0);
			break;
		}
		case MONGODB: {
			manager = new MongoDBManager(args);
			break;
		}
		case MOCK: {
			log.warn("Using MOCK implementation, this is a backend simulation only");
			manager = new MockStorageManager(args);
			break;
		}
		default: {
			log.warn("Using default storage implentation RIAK");
			manager = new RiakManager(args);
			break;
		}

		}
		return manager;
	}
	
	/**
	 * Returns storage implementation based on input storage name
	 * @param storage name of the implementation
	 * @return
	 */
	public static Constants.STORAGEIMPL getStorageImpl(String storage) {
		if(storage.equalsIgnoreCase(Constants.STORAGEIMPL.RIAK.toString())) {
			return Constants.STORAGEIMPL.RIAK;
		}
		if(storage.equalsIgnoreCase(Constants.STORAGEIMPL.CASSANDRA.toString())) {
			return Constants.STORAGEIMPL.CASSANDRA;
		}
		if(storage.equalsIgnoreCase(Constants.STORAGEIMPL.MONGODB.toString())) {
			return Constants.STORAGEIMPL.MONGODB;
		}
		if(storage.equalsIgnoreCase(Constants.STORAGEIMPL.MOCK.toString())) {
			return Constants.STORAGEIMPL.MOCK;
		}
		// Riak is default behavior
		return Constants.STORAGEIMPL.RIAK;
	}

}
