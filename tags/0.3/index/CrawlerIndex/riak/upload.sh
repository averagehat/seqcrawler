#!/bin/bash

for file in $@
 do
echo "Uploading $file..."
  case $file in
    *.html )
      content_type="Content-Type: text/html"
      ;;
    *.js )
      content_type="Content-Type: text/javascript"
      ;;
    *.css )
      content_type="Content-Type: text/css"
      ;;
    *.png )
      content_type="Content-Type: image/png"
      ;;
  esac

curl --silent -X PUT -H "${content_type}" --data-binary @$file http://seqcrawler.genouest.org:8098/riak/web/$file;

done
