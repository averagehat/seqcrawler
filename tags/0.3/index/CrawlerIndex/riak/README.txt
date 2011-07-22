This solr directory is solr configuration to use for seqcrawler.

**********************************
Updates in configuration to be done:

upload.sh:
modify the hostname where to post the files (riak host)


************************************
Files installation

Upload all files in riak host with:

upload.sh filename


*** Update of IP / Ring

riak stop
update riak ip and innodb store in app.config and vm.config
riak-admin reip riak@old riak@new
rm /var/lib/riak/*
join to ring if required
riak start