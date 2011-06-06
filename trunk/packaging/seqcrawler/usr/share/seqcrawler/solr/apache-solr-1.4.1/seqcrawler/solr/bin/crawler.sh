#!/bin/bash

BASEDIR=$(dirname $0)
. $BASEDIR/env.sh

echo "Calling CrawlerIndex"

# Can replace classpath with jar with dependencies
#java -classpath $INDEXHOME/lib/commons-cli-1.2.jar:$INDEXHOME/lib/commons-codec-1.3.jar:$INDEXHOME/lib/commons-fileupload-1.2.1.jar:$INDEXHOME/lib/commons-httpclient-3.1.jar:$INDEXHOME/lib/commons-io-1.4.jar:$INDEXHOME/lib/commons-logging-1.1.1.jar:$INDEXHOME/lib/geronimo-stax-api_1.0_spec-1.0.1.jar:$INDEXHOME/lib/junit-3.8.1.jar:$INDEXHOME/lib/lucene-analyzers-2.9.1.jar:$INDEXHOME/lib/lucene-core-2.9.1.jar:$INDEXHOME/lib/lucene-highlighter-2.9.1.jar:$INDEXHOME/lib/lucene-memory-2.9.1.jar:$INDEXHOME/lib/lucene-misc-2.9.1.jar:$INDEXHOME/lib/lucene-queries-2.9.1.jar:$INDEXHOME/lib/lucene-snowball-2.9.1.jar:$INDEXHOME/lib/lucene-spellchecker-2.9.1.jar:$INDEXHOME/lib/servlet-api-2.5.jar:$INDEXHOME/lib/slf4j-api-1.5.5.jar:$INDEXHOME/lib/slf4j-nop-1.5.5.jar:$INDEXHOME/lib/solr-commons-csv-1.4.0.jar:$INDEXHOME/lib/solr-core-1.4.0.jar:$INDEXHOME/lib/solr-solrj-1.4.0.jar:$INDEXHOME/lib/stax-api-1.0.1.jar:$INDEXHOME/lib/wstx-asl-3.2.7.jar -jar $INDEXHOME/CrawlerIndex-0.1.jar $@
java -jar $INDEXHOME/CrawlerIndex-0.1-jar-with-dependencies.jar $@

echo "Done"