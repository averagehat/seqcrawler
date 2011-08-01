// Configuration for seqcrawler

   $pageCountResults = 20;
   $solrUrl = "http://seqcrawler.genouest.org/solr/select?rows="+$pageCountResults+"&qt=shard&wt=json&";
   $solrFacet ="facet=on&facet.field=bank&";
   // RIAK: $storageurl = "http://seqcrawler.genouest.org/riak/web";
   // MONGO:
   $storageurl = "http://seqcrawler.genouest.org/mongo";
   $gburl = "http://seqcrawler.genouest.org";
   $gbbank = "genouest";
   
   
   // Rewrite fields
   $links = [];
   $links['test.id'] = '<a href="test.html?id=#VAR#>#VAR#</a>';
   $links['all.test'] = "Sample test for #VAR#";
