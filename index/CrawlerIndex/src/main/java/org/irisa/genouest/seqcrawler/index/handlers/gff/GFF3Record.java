/**
 * 
 */
package org.irisa.genouest.seqcrawler.index.handlers.gff;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.irisa.genouest.seqcrawler.index.App;
import org.irisa.genouest.seqcrawler.index.IndexUtils;

/**
 * Class representing a GFF v3 record
 * @author osallou
 *
 */
public class GFF3Record {

	private Log log = LogFactory.getLog(GFF3Record.class);
	
	public enum StrandedFeature {
		NEGATIVE,POSITIVE,UNKNOWN;
	};
	
	private String sequenceId;
	

	@Field("feature")
	private String type;

	@Field
	private Integer start;

	@Field
	private Integer end;
	
	private Properties annotations;

	@Field
	private StrandedFeature strand;

	@Field
	private String attributes;

	@Field
	private String bank;


	public void setBank(String bank) {
		this.bank = bank;
	}


	public GFF3Record() {
		annotations = new Properties();
	}

	public void setSequenceID(String group) {
		sequenceId = group;
		
	}

	public void setType(String group) {
		type = group;
		
	}

	public void setStart(Integer value) {
		start = value;
		
	}

	public void setEnd(Integer value) {
		end = value;
		
	}

	public Properties getAnnotations() {
		return annotations;
	}

	public void setStrand(StrandedFeature value) {
		strand = value;
		
	}
	

	public String getSequenceId() {
		return sequenceId;
	}

	public String getType() {
		return type;
	}

	public Integer getStart() {
		return start;
	}

	public Integer getEnd() {
		return end;
	}

	public StrandedFeature getStrand() {
		return strand;
	}
	
	public String getBank() {
		return bank;
	}

	/**
	 * Index current GFF record in index server if not in debug mode
	 */
	public void index() {
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("bank", this.getBank());
		doc.addField("chr", this.getSequenceId());
		doc.addField("start", this.getStart());
		doc.addField("end", this.getEnd());
		doc.addField("feature", this.getType());
		doc.addField("attributes", this.getAttributes());
		if(this.getStrand().equals(StrandedFeature.POSITIVE)) {
			doc.addField("strand","+");
		}
		else if(this.getStrand().equals(StrandedFeature.NEGATIVE)) {
			doc.addField("strand","-");
		}
		else {
			doc.addField("strand",".");
		}
		Enumeration it = annotations.propertyNames();
		while(it.hasMoreElements()) {
			String key = (String)it.nextElement();			
			doc.addField(key.toLowerCase(), annotations.get(key));
			
		}

		try {
			this.log.debug("Index new GFF record "+doc.toString());
			if(App.debug()==false) IndexUtils.getServer().add(doc);			
		} catch (SolrServerException e) {
			this.log.error(e.getMessage());
		} catch (IOException e) {
			this.log.error(e.getMessage());
		}

		
	}


	private Object getAttributes() {
		return attributes;
	}


	public void setAttributes(String value) {
		this.attributes = value;
		
	}


}
