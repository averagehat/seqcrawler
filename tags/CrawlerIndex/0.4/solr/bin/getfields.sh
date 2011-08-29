#!/bin/bash

BASEDIR=$(dirname $0)
. $BASEDIR/env.sh

echo "List fields operation"
echo "Usage: listfields.sh -file outputfilename"
echo ""

java -cp $INDEXHOME/CrawlerIndex-$INDEXVERSION-jar-with-dependencies.jar  org.irisa.genouest.seqcrawler.index.utils.Utils -listfields $@

echo "Over"