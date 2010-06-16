/**
 * 
 */
package org.irisa.genouest.seqcrawler.index.handlers.gff;


import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class representing a GFF v3 record
 * @author osallou
 *
 */
public class GFF3Record {

	private Logger log = LoggerFactory.getLogger(GFF3Record.class);
	
	@Field("chr")
	private String sequenceId;
	
	@Field("feature")
	private String type;

	@Field
	private Integer start;

	@Field
	private Integer end;
	
	private Properties annotations;

	@Field
	private String strand;

	@Field
	private String attributes;

	@Field
	private String bank;


	@Field
	public void setBank(String bank) {
		this.bank = bank;
	}


	public GFF3Record() {
		annotations = new Properties();
	}

	@Field("chr")
	public void setSequenceID(String group) {
		sequenceId = group;
		
	}

	@Field("feature")
	public void setType(String group) {
		type = group;
		
	}

	@Field
	public void setStart(Integer value) {
		start = value;
		
	}
	
	@Field
	public void setEnd(Integer value) {
		end = value;
		
	}

	public Properties getAnnotations() {
		return annotations;
	}

	
	@Field
	public void setStrand(String value) {
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

	public String getStrand() {
		return strand;
	}
	
	public String getBank() {
		return bank;
	}

	/**
	 * Index current GFF record in index server if not in debug mode
	 * @return Document to index
	 */
	public SolrInputDocument getDocument() {
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("bank", this.getBank());
		doc.addField("chr", this.getSequenceId());
		doc.addField("start", this.getStart());
		doc.addField("end", this.getEnd());
		doc.addField("feature", this.getType());
		doc.addField("attributes", this.getAttributes());
	    doc.addField("strand",this.getStrand());
		
		Enumeration<?> it = annotations.propertyNames();
		while(it.hasMoreElements()) {
			String key = (String)it.nextElement();			
			doc.addField(key.toLowerCase(), annotations.get(key));
			
		}
		
		return doc;
	}

	@Override
	public String toString() {
		return this.getSequenceId()+"\t"+this.getBank()+"\t"+this.getType()+"\t"+this.getStart()+"\t"+this.getEnd()+"\t.\t"+this.getStrand()+"\t.\t"+this.getAttributes();
	}
	

	public String getAttributes() {
		return attributes;
	}

	@Field
	public void setAttributes(String value) {
		this.attributes = value;
		
	}


}
