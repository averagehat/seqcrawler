#!/usr/bin/perl

=head1 NAME

mongo - CGI script to query the MongoDB database based on an input id


=head1 SYNOPSIS

  Call cgi script with parameter id: http://myserver/cgi-bin/mongo/mongo.pl?id=BX123
  Warning, id must be URL encoded

  
=head1 DESCRIPTION

This cgi scripts provide a query access point to get the sequence data in JSON format.
JSON parameters are _id , shards, content, metadata.
metadata is an object with start and stop parameters

=head1 METHODS

=head1 BUGS


=head1 SEE ALSO


=head1 AUTHOR

Olivier Sallou<lt>olivier.sallou@irisa.fr<gt>.

This script is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.

=cut

use MongoDB;
use JSON;
use CGI qw/:standard/;

$query = new CGI;

print header(-type=>'application/json');

if(param()) {

my $connection = MongoDB::Connection->new;
my $database   = $connection->seqcrawler;
my $collection = $database->bank;
my $id = param('id');
my $data       = $collection->find_one({ _id => $id });
if($data) {
    my $myjson = encode_json $data;
    print $myjson;
}
else {
    print "{ \"error\" : \"No match found\"}";
}
}
