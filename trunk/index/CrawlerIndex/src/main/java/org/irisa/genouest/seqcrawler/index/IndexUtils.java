package org.irisa.genouest.seqcrawler.index;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;


public class IndexUtils {
	
	private static IndexUtils instance = null;
	
	public static IndexUtils getInstance() {
		if(instance==null) instance = new IndexUtils();
		return instance;
	}

	private Log log = LogFactory.getLog(IndexUtils.class);

	
	private static SolrServer server = null;
	
	public static SolrServer getServer() {
		return server;
	}

	public static void setServer(SolrServer remoteserver) {
		IndexUtils.server = remoteserver;
	}

	public void cleanIndex() throws SolrServerException, IOException {
		log.warn("Deleting all index");
	    server.deleteByQuery( "*:*" );// delete everything!
	    server.commit();
	}
	
	public void cleanIndexByBank(String bank) throws SolrServerException, IOException {
		log.warn("Deleting from index bank: "+bank);
	    server.deleteByQuery( "bank:"+bank );// delete everything!
	    server.commit();
	}
}
