// Configuration for seqcrawler

   $pageCountResults = 20;
   $solrUrl = "http://localhost/solr/select?rows="+$pageCountResults+"&qt=shard&wt=json&";
   $solrFacet ="facet=on&facet.field=bank&";
   // RIAK: $storageurl = "http://localhost/riak/web";
   // MONGO:
   $storageurl = "http://localhost/mongo";
   $gburl = "http://localhost";
   $gbbank = "seqcrawler";
   
   // Set this variable to debug web interface. Query will send back dummy GFF data with no server query.
   $debugweb = 0;
   
   // Rewrite fields
   $links = [];
   $links['test.id'] = '<a href="test.html?id=#VAR#>#VAR#</a>';
   $links['all.test'] = "Sample test for #VAR#";
