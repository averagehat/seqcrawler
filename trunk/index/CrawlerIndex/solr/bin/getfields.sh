#!/bin/bash

. env.sh

echo "List fields operation"
echo "Usage: listfields.sh -file outputfilename"
echo ""

java -cp $INDEXHOME/CrawlerIndex-0.1-jar-with-dependencies.jar  org.irisa.genoue
st.seqcrawler.index.utils.Utils -listfields $@

echo "Over"