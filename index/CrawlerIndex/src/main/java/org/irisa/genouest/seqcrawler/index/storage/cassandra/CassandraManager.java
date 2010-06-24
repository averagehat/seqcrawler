package org.irisa.genouest.seqcrawler.index.storage.cassandra;

import java.util.List;

import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;
import org.irisa.genouest.seqcrawler.index.storage.StorageManagerInterface;
import org.irisa.genouest.seqcrawler.index.storage.StorageObject;

public class CassandraManager implements StorageManagerInterface {

	public void delete(String key) throws StorageException {
		// TODO Auto-generated method stub

	}

	public void deleteAll(String key) throws StorageException {
		// TODO Auto-generated method stub

	}

	public StorageObject get(String key) throws StorageException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> getKeys() throws StorageException {
		// TODO Auto-generated method stub
		return null;
	}

	public void store(StorageObject object) throws StorageException {
		// TODO Auto-generated method stub

	}

	public void setBucket(String bucket) {
		// TODO Auto-generated method stub
		
	}

}
