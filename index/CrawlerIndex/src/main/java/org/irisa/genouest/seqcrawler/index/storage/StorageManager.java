package org.irisa.genouest.seqcrawler.index.storage;

import java.util.HashMap;
import java.util.Map;

import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.handlers.GFFHandler;
import org.irisa.genouest.seqcrawler.index.storage.riak.RiakManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage Handler to send and get raw data in a remote storage.
 * 
 * 
 * @author osallou
 * 
 */
public class StorageManager {

	private Logger log = LoggerFactory.getLogger(StorageManager.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}
	public StorageManager() {
		
	}
	
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
		default: {
			manager = new RiakManager(args);
			break;
		}

		}
		return manager;
	}

}