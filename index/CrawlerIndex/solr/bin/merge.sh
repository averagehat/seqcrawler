#!/bin/bash

BASEDIR=$(dirname $0)
. $BASEDIR/env.sh

echo "Merging operation"

#java -Xmx3000M -cp $INDEXHOME/lib/lucene-core-2.9.1.jar:$INDEXHOME/lib/lucene-misc-2.9.1.jar org/apache/lucene/misc/IndexMergeTool $@
java -Xmx3000M -cp $INDEXHOME/CrawlerIndex-$INDEXVERSION-jar-with-dependencies.jar org/irisa/genouest/seqcrawler/index/Merge $@



echo "Done"