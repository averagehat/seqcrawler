#!/usr/bin/perl

use HTTP::Request;
use LWP::UserAgent;
use JSON;
use MongoDB;

$solrTest = 0;
$mongoTest =0;
$gbrowseTest=0;

$request = HTTP::Request->new(GET => 'http://localhost:8080/solr/select?q=gene&wt=json');

$ua = LWP::UserAgent->new;
$response = $ua->request($request);

$solr = decode_json $response->content;

$numFound = $solr->{response}->{numFound};

if($numFound>0) {
  print "Index looks ok\n";
  $solrTest = 1;
}
else {
 print "No match in index, something went wrong!!\n";
}


my $connection = MongoDB::Connection->new;
my $database   = $connection->seqcrawler;
my $collection = $database->bank;
my $mongoCount = $collection->count();
if($mongoCount>1) {
  print "Mongo DB looks ok\n";
  $mongoTest = 1;
}
else {
 print "Mongo DB is empty, something went wrong!!\n";
}

#look for chrI    SGD     gene    335     649     .       +       .       ID=YAL069W;Name=YAL069W;Ontology_term=GO:0003674,GO:0005575,GO:0008150;Note=Dubious%20open%20reading%20frame%20unlikely%20to%20encode%20a%20protein%2C%20based%20on%20available%20experimental%20and%20comparative%20sequence%20data;dbxref=SGD:S000002143;orf_classification=Dubious
$request = HTTP::Request->new(GET => 'http://localhost/gb2/gbrowse_details/sample?ref=chrI;start=335;end=649;name=YAL069W;class=gene;feature_id=YAL069W;db_id=general');
$ua = LWP::UserAgent->new;
$response = $ua->request($request);

$gb2 = $response->content;

if($gb2=~/YAL069W/) {
  print "GBrowse2 looks fine\n";
  $gbrowseTest = 1;
}
else {
  print "GBrowse2 did not answered correctly\n";
  print "Could be an apache of gb2 config issue\n";
}


if($solrTest==1 && $mongoTest==1 && $gbrowseTest==1) {
  print "All tests passed, looks fine!\n";
}
else {
  print "Some tests failed: \n";
  if($solrTest!=1) { print "- Index test FAILED\n"; }
  if($mongoTest!=1) { print "- MongoDB storage test FAILED\n"; }
  if($gbrowseTest!=1) { print "- GBrowse2 test FAILED\n"; }
}

