package org.irisa.genouest.seqcrawler.index.storage;

import java.util.List;

import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;

public interface StorageManagerInterface {

	public void store(StorageObject object) throws StorageException;
	public StorageObject get(String key) throws StorageException;
	public void delete(String key) throws StorageException;
	public void deleteAll(String key) throws StorageException;
	public List<String> getKeys() throws StorageException ;
	public void setBucket(String bucket);
}
