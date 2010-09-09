#!/bin/bash

export INDEXHOME=/opt/solr/apache-solr-1.4.1/seqcrawler/solr/bin

echo "List fields operation"
echo "Usage: listfields.sh -file outputfilename"
echo ""

java -cp $INDEXHOME/CrawlerIndex-0.1-jar-with-dependencies.jar  org.irisa.genoue
st.seqcrawler.index.utils.Utils -listfields $@

echo "Over"