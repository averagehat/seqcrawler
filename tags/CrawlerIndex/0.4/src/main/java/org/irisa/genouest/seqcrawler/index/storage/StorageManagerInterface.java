package org.irisa.genouest.seqcrawler.index.storage;

import java.util.List;

import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;

public interface StorageManagerInterface {

	/**
	 * Store a {@link StorageObject} on remote backend with id as key
	 * @param object
	 * @throws StorageException
	 */
	public void store(StorageObject object) throws StorageException;
	/**
	 * Query storage backend to get a single {@link StorageObject} instance.
	 * @param key Unique key to query the backend as set in StorageObject
	 * @return A StorageObject instance filled with result values
	 * @throws StorageException
	 */
	public StorageObject get(String key) throws StorageException;
	/**
	 * Delete a {@link StorageObject} instance in storage backend using unique key. If object
	 * is sharded (cut in multiple instance), the function will not delete shards but only specified object.
	 * To delete all shards including current object see {@link #deleteAll}.
	 * @param key
	 * @throws StorageException
	 */
	public void delete(String key) throws StorageException;
	/**
	 * Delete instance of the object as well as all shards declared for this object.
	 * @param key Key of the object
	 * @throws StorageException
	 */
	public void deleteAll(String key) throws StorageException;
	/**
	 * Gets all the keys for the specified bucket
	 * @return List with all keys
	 * @throws StorageException
	 */
	public List<String> getKeys() throws StorageException ;
	/**
	 * Sets the bucket (implementation dependent) for the storage manager. Bucket can be seen as a namespace.
	 * @param bucket Name of the bucket.
	 */
	public void setBucket(String bucket);
}
