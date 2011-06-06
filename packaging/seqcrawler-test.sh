#!/bin/bash

# Regenerate yeast example
echo "Generating yeast example, this can take a few minutes..."

/usr/share/seqcrawler/solr/apache-solr-1.4.1/seqcrawler/solr/bin/crawler.sh -f /var/lib/seqcrawler/example/yeast.gff -sh /usr/share/seqcrawler/solr/apache-solr-1.4.1/seqcrawler/solr -sd /index/data -storage mongodb -store -stHost 127.0.0.1 > /var/log/seqcrawler/yeast_index.log

echo "Launch basic tests"
perl /usr/share/seqcrawler/bin/seqcrawler-test.pl
