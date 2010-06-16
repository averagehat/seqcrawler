#!/bin/bash

export INDEXHOME=/opt/solr/apache-solr-1.4.0/seqcrawler/indexer

echo "Merging operation"

#java -Xmx3000M -cp $INDEXHOME/lib/lucene-core-2.9.1.jar:$INDEXHOME/lib/lucene-misc-2.9.1.jar org/apache/lucene/misc/IndexMergeTool $@
java -Xmx3000M -cp $INDEXHOME/CrawlerIndex-0.1-with-dependencies.jar org/apache/lucene/misc/IndexMergeTool $@



echo "Done"