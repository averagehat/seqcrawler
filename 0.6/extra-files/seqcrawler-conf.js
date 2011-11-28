   $pageCountResults = 20;
   $solrUrl = "http://localhost/solr/select?rows="+$pageCountResults+"&qt=shard&wt=json&";
   $solrFacet ="facet=on&facet.field=bank&";
   // MONGO:
   $storageurl = "http://localhost/mongo";
   $gburl = "http://localhost";
   $gbbank = "seqcrawler";

   // Set this variable to debug web interface. Query will send back dummy GFF data with no server query.
   $debugweb = 0;
   
   // Rewrite fields
   $links = [];