package Bio::DB::GFF::Adaptor::SolrDBI;


use strict;
use LWP::Simple;
use URI::Escape;
use JSON;

use constant DEBUG => 0;


use constant ITERATOR  => 'Bio::DB::GFF::Adaptor::LucegeneIterator';

sub _iterator { return ITERATOR }

our  ($LUBIN, $LUBINDEBUG, 
      $qsep, $localID, $debug, $rootpath);

BEGIN{


$LUBINDEBUG = 0;  #was 1; now this mostly does lc(term), also pads loc numbers; drop!?
$LUBIN= undef;


$qsep = ' '; 

$rootpath= undef; 
$debug= 0;
$localID= 1;
};


sub new {
  my $class = shift ;
	my %fields = @_;  #?? is this ok or use rearrange() ?? 
	my $self = \%fields;  ## fields need FORMAT, adaptor params
	bless $self, $class;
  $self->solrInit();

  if (DEBUG) {
  local $^W = 0; # ignore $tohash mistakes ? 
  foreach my $k (sort keys %$self) {
    print STDERR " $k => ",$self->{$k},"\n";
    }
  print STDERR "\n";
  }
  return $self;
}

sub solrConfig {
  my $self= shift;
  my ($keys,$default)= @_;
  my ($val,$key)= (undef,undef);
  while ($key = shift(@$keys)) {
    my $isuc= ($key =~ /^A-Z/);
    my $uckey= uc($key); #  caller may have uc'ed all keys (bioperl std)
    $val= $self->{$key} || $self->{$uckey};
    return $val if(defined $val);
    $val= $self->{adaptor}->{$key} || $self->{adaptor}->{$uckey};
    return $val if(defined $val);
    $val= $self->{adaptor}->{'-'.$key} || $self->{adaptor}->{'-'.$uckey};
    return $val if(defined $val);
    $val= $ENV{$key} if($isuc); # is this ok for all keys?
    return $val if(defined $val);
    }
  return $default;
}


sub solrInit {
  my $self= shift;
  
  $self->{SOLR_INIT}= 0;
  $self->{as_array}= 0; # feature sruct

  $self->{use_server}   = $self->solrConfig( [qw(use_server)], 0); #? should be 0 default
  $self->{server_port}  = $self->solrConfig( [qw(dbport port)], 8080);
  $self->{server_host}  = $self->solrConfig( [qw(host)], "localhost");
  $self->{maxresult}    = $self->solrConfig( [qw(maxresult)], 99999999);
  $self->{FORMAT}       = "gff";
  $self->{BANK}         = $self->solrConfig( [qw(BANK bank)], "all") unless($self->{BANK});
  $self->{STORAGEURL}   = $self->solrConfig( [qw(STORAGEURL storageurl)], "undefined") unless($self->{STORAGEURL});


  if ($self->{FORMAT} eq 'gff') {
    $self->{is_gff}= 1; 
    $self->{needgroupsort}= 1; 
    $self->{outfields}="chr source feature start stop score strand phase attributes";
    $self->{segfields}="chr feature start stop strand";
    $self->{outfields2feature}= \&_json2feature;
    
  } 
  
  my $DNA_LIB = $self->{DNA_LIB} = $self->solrConfig( [qw(LUCEGENE_DNA_LIB DNA_LIB dna)],  
      "/tmp");

  $self->{LOGFILE} = $self->solrConfig( [qw(LOGFILE)]);  

  $self->{CLIENTIPS} = $self->solrConfig( [qw(CLIENTIPS)]);
  $self->{ONLYLOCAL} = $self->solrConfig( [qw(ONLYLOCAL)], "true");

  local $^W = 0; # ignore $tohash mistakes ?

  $self->{SOLR_INIT}= 1;
}


sub solrSearch {
  my $self= shift;

  my ($myroot,$command,$convertor)= @_;
  my @result=(); 
  # SEQCRAWLER
  # Expecting something like +chr:AY403533 +feature:gene +(start:[900 TO 1000] end:[900 TO 1000])
  # my $query = "q=chr:AY403533 AND feature:sequence&rows=10";
  $self->{query} .= " +bank:".$self->{BANK} if($self->{BANK} ne "all");
  my $over=0;
  my $nbresults=0;
  my $step=0;
  while($over!=1) {
  #Limit each query to 200 results then loop if required
  my $query = "qt=shard&q="._luqueryescape($self->{query})."&rows=200&wt=json&fl=*%2Cscore&start=".$step;
  if(DEBUG) { print STDERR "#QUERY,bank $self->{BANK}, querying with: $query"; }
  my $url = 'http://'.$self->{server_host}.':'.$self->{server_port}.'/solr/select/?'.$query;

  my $content = get $url;
  die "Couldn't get $url" unless defined $content;
  if(DEBUG) { print STDERR "#server answer is: ".$content };
  my $queryResults = decode_json $content;
  
  if($queryResults->{response}->{numFound}) {
      if(DEBUG) { print STDERR "####### JSON DECODE FOUND : ".$queryResults->{response}->{numFound} };
      $nbresults+=$queryResults->{response}->{numFound};
  }
  else {
      if(DEBUG) { print STDERR "####### No match found" };
  }
  my $jsondoc=0;
  for($jsondoc=0;$jsondoc<$queryResults->{response}->{numFound};$jsondoc++) { 
  my @res= $convertor->($self,$queryResults->{response}->{docs}[$jsondoc]);
  push(@result,@res) if @res > 0;
  }
  # Stop if max reached or all matches loaded
  if($nbresults>$self->{maxresult} || $nbresults>=$queryResults->{response}->{numFound})  { $over=1 };
  $step+=200;
  }

  return \@result;
}


#-------------------------------------

=item  query subroutines for solr gff

=cut

# array indices for @feature_row instead of %feature_hash
use constant FREF    => 0;
use constant FSTART  => 1;
use constant FSTOP   => 2;
use constant FSOURCE => 3;
use constant FMETHOD => 4;
use constant FSCORE  => 5;
use constant FSTRAND => 6;
use constant FPHASE  => 7;
use constant GCLASS  => 8;
use constant GNAME   => 9;
use constant TSTART  => 10;
use constant TSTOP   => 11;
use constant FID     => 12;
use constant GID     => 13;
use constant FCLASS  => 14;
use constant FATTR   => 15;
use constant FNAME   => 16;

#? need _feathash2array  ??

sub _featarray2hash {
  my $self= shift;
  my $ftr = shift;
  return $ftr if ( ref( $ftr ) =~ /HASH/);
  # return {} unless ( ref( $ftr ) =~ /ARRAY/);
  my $fth= {
    'ref'  => $ftr->[FREF], ## == chr ?
    class  => $ftr->[FCLASS], # what of source:method ????
    source => $ftr->[FSOURCE],
    method => $ftr->[FMETHOD],
    feature_id => $ftr->[FID], # not in orig. hash list ??
    start  => $ftr->[FSTART],
    stop   => $ftr->[FSTOP],
    score  => $ftr->[FSCORE],
    strand => $ftr->[FSTRAND],
    phase  => $ftr->[FPHASE ],
    gclass => $ftr->[GCLASS],
    gname  => $ftr->[GNAME],
    tstart => $ftr->[TSTART],
    tstop  => $ftr->[TSTOP],
    group_id => $ftr->[GID],
    attributes  => $ftr->[FATTR], 
    name => $ftr->[FNAME], # dgg added
    };
  return $fth;
}



# all fields [ Dbxref, EOR, ID, Name, Parent, attributes, chr, count,
# cyto_range, docclass, docid, feature, field, gbunit, lastModified,
# modified, phase, score, source, species, start, stop, strand, synonym,
# synonym_2nd, uid, url]
# return fields [ chr, source, feature, start, stop, score, strand, phase, attributes]

sub _json2feature {
  my $self= shift;
  my( $jsondoc)= @_;
  my ( $groupid,$tstart,$tstop)= (0,0,0);
  my ( $name, $id, $parent) = ('','','');
  my $chr = $jsondoc->{chr};
  my $source = $jsondoc->{bank};
  my $feattype = $jsondoc->{feature}; 
  my $start = $jsondoc->{start}[0];
  my $stop = $jsondoc->{end}[0];
  my $score = $jsondoc->{score};
  my $strand = $jsondoc->{strand};
  my $attributes = $jsondoc->{attributes};
  my $phase=0;
  return () unless(defined $feattype
    && $chr =~ /^\w/ && $start =~ /^\d+$/ && $feattype =~ /^\w/);


  $source= '' if ($source eq '.'); #??
  # my $method= $feattype;
  $score= 0 if ($score eq '.');
  $strand = ($strand =~ m/[-]/) ? '-' : '+'; 

  my @parents=();
  my $target= '';

  my @attributes;
  foreach my $group (split(/;/,$attributes)) {
    my ($tag,$value) = split /=/,$group;
    next unless($tag && $value);
    $tag             = unescape($tag); 
    my @values       = map {unescape($_)} split /,/,$value;
    if ($tag =~ /^synonym$/i) { $tag = 'Alias'; }
    push @attributes,[$tag=>$_] foreach @values;

    if ($tag =~ /^ID$/i) { $id= $values[0]; }
    elsif ($tag =~ /^Parent$/i) { @parents= @values; } 
    elsif ($tag =~ /^Name$/i) { $name= $values[0]; }
    elsif ($tag =~ /^Target$/i) {
       ($target,$tstart,$tstop)= split(/[\+\s]/, $values[0]);
       } # format:Target=name+tstar+tstop
    elsif (!$name && $tag =~ /^Alias$/i) { $name= $values[0]; }
    }
   
  #Remove to keep storage web interface address agnostic of the system  
  #push @attributes,['raw data' => '<a href="'.$self->{URL}.'/web/dataquery.html?id='.$id.'&source='.$chr.'&start='.$start.'&stop='.$stop.'"> See raw sequence and transcript</a>'] if($self->{STORAGEURL} ne "undefined");;
    

  my $gclass= $feattype;  ## what is gclass - group class - need parent class if parent !
  if (@parents) {
    if ($feattype =~ /exon|intron|UTR/) {  $gclass= 'mRNA'; } ## hack !
    elsif ($feattype =~ /match_part/) { $gclass= 'match'; } ## hack !
    elsif ($feattype =~ /mRNA/) { @parents=(); } ##  THAT fixed dang aggregator.display_name
    ## GFF doesn't handle 3+level parents
    }
  unless(@parents) {
    my $par= $target || $id || $name;
    @parents=($par);
    }

  $id= 'NULL_' . $localID++ unless($id);

  my @ftlist=();
  foreach my $gname (@parents) {
  my $ft;
if ($self->{as_array}) {
    my @ft=(); $ft= \@ft;
    $ft[FREF]    = $chr;
    $ft[FSTART]  = $start;
    $ft[FSTOP]   = $stop;
    $ft[FSOURCE] = $source;
    $ft[FMETHOD] = $feattype; # $method;
    $ft[FSCORE]  = $score;
    $ft[FSTRAND] = $strand;
    $ft[FPHASE ] = $phase;
    $ft[GCLASS]  = $gclass;
    $ft[GNAME]   = $gname;
    $ft[TSTART]  = $tstart;
    $ft[TSTOP]   = $tstop;
    $ft[FID]     = $id;
    $ft[GID]     = $groupid;
    $ft[FCLASS]  = $feattype;
    $ft[FATTR]   = \@attributes; # hash ref ??
    $ft[FNAME]   = $name; ## dgg
} else {
  $ft= {
    'ref'  => $chr, ## == chr ?
    class  => $feattype, # what of source:method ????
    source => $source,
    method => $feattype, #$method,
    feature_id => $id, # not in orig. hash list ??
    start  => $start,
    stop   => $stop,
    score  => $score,
    strand => $strand,
    phase  => $phase,
    gclass => $gclass,
    gname  => $gname,
    tstart => $tstart,
    tstop  => $tstop,
    group_id => $groupid,
    attributes  => \@attributes,
    name => $name,
    };
}
  push(@ftlist, $ft);
  }

  return @ftlist;

}


sub _luqueryescape { 
  my $v = shift;
  return uri_escape($v);
}


## gff: fieldnames = chr source feature start stop score strand phase attributes

sub _luqueryName {
  my $self= shift;
  my( $val)= @_;
  my $luq="";
  if ($val) {
    if (ref($val) =~ /ARRAY/) { $val= join(" ",@$val); };
    #$val = _luqueryescape($val);
    #$val = "(".$val.")" if ($val =~ /\s/);
    $val = "\"".$val."\"" if ($val =~ /\s/);
    
    $val = lc($val) if ($LUBINDEBUG);
    if ($self->{is_gff}) {
    $luq .= "+(name:$val alias:$val name:$val synonym:$val id:$val) $qsep";
    } else {
    $luq .= "+(gene:$val id:$val) $qsep"; #  db_xref:$val search all? or only gene==name ?
    }
    }
  return $luq;
}

sub _luqueryId {
  my $self= shift;
  my( $val)= @_;
  my $luq="";
  if ($val) {
    if (ref($val) =~ /ARRAY/) { $val= join(" ",@$val); };
    #$val = _luqueryescape($val);
    #$val = "(".$val.")" if ($val =~ /\s/);
    $val = "\"".$val."\"" if ($val =~ /\s/);
    $val = lc($val) if ($LUBINDEBUG);
    if ($self->{is_gff}) {
    $luq .= "+(id:$val) $qsep";
    } else {
    $luq .= "+(id:$val) $qsep"; # search all? or only id==ids ?
    }
    }
  return $luq;
}

sub _luqueryNotes {
  my $self= shift;
  my( $val)= @_;
  my $luq="";
  if ($val) {
    if (ref($val) =~ /ARRAY/) { $val= join(" ",@$val); };
    #$val = _luqueryescape($val);
    #$val = "(".$val.")" if ($val =~ /\s/);
    $val = "\"".$val."\"" if ($val =~ /\s/);
    
    $val = lc($val) if ($LUBINDEBUG);
    if ($self->{is_gff}) {
    $luq .= "+(text:$val) $qsep";

    } else {
    $luq .= "+(notes:$val) $qsep";  
    }
    }
  return $luq;
}

sub _luqueryType {
  my $self= shift;
  return $self->_luqueryType_gff(@_) if ($self->{is_gff});
  
  my( $typelist)= @_;
  my $luq="";
  if ($typelist) {
    if (ref($typelist) =~ /ARRAY/) {
    my @typelist=();
    foreach (@$typelist) {
      my ($method,$source);
      if (ref($_) =~ /ARRAY/) { ($method,$source) = @$_; }
      else { $method= $_; }
      my $type= defined($source) ? "$method:$source" : $method;
      push(@typelist, $type);
      }
    $typelist= join(" ",@typelist); 
    }
    
    $typelist = _luqueryescape($typelist);
    $typelist = "(".$typelist.")" if ($typelist =~ /\s/);
    
    $typelist= lc($typelist) if ($LUBINDEBUG);
    $luq .= "+feature:$typelist $qsep";
    }
    
  return $luq;
}

sub _luqueryType_gff {
  my $self= shift;
  my( $typelist)= @_;
  my $luq="";
  
  if ($typelist) {
    ## luq for gff source,method:
    ## +((+feature:xxx +source:yyy) OR (+feature:xx1 +source:yy1) OR (feature:xx2 source:yy2))
    
    if (ref($typelist) =~ /ARRAY/) {
      my @typelist=();
      foreach (@$typelist) {
        my ($method,$source);
        if (ref($_) =~ /ARRAY/) { ($method,$source) = @$_; }
        else { $method= $_; }
        # $method =~ s/:/?/g; #? need more luquery escapes?
        $method = _luqueryescape($method);
        if (defined $source && $source ne ".") {
          # $source =~ s/:/?/g; #? need more luquery escapes?
          $source = _luqueryescape($source);
          push(@typelist, "(feature:$method AND source:$source)");
          }
        else {
          push(@typelist, "feature:$method"); #? do we need source:. here
          }
        }
      $typelist= join(" ",@typelist); 
      }
    else {

      $typelist = _luqueryescape($typelist);
      $typelist = "feature:$typelist";
      }
      
    $typelist = "(".$typelist.")" if ($typelist =~ /\s/);
    $typelist= lc($typelist) if ($LUBINDEBUG);

    $luq .= "+$typelist $qsep" if $typelist;
    }
  return $luq;
}



sub _numpad {
# 00007807470; need 11? leading 000 for lucene w/o bio field handlers
  my $n= shift;
  return sprintf("%011d",$n);
}

sub _luqueryLocation {
  my $self= shift;
  my( $refseq, $start, $stop, $rangetype)= @_;

  my $luq="";
  $refseq= lc($refseq) if ($LUBINDEBUG);
  $luq .= "+chr:$refseq $qsep" if ($refseq);

  my $MINLOC = ($LUBINDEBUG) ? _numpad(0) : 0 ;
  my $MAXLOC = ($LUBINDEBUG) ? _numpad(999999999) : 999999999 ;
  ## this is overlaps range search
  if (defined($start) && defined($stop)) {
    $start= _numpad($start) if ($LUBINDEBUG);
    $stop = _numpad($stop) if ($LUBINDEBUG);

    if ($self->{is_gff}) {
    $luq .= "+(start:[$start TO $stop] stop:[$start TO $stop]) $qsep";
    # ^^gff-start+stop-runtime:        12.05 real         0.01 user         0.03 sys
    } else {
    #$luq .= "+range.start:[$MINLOC $stop] +range.stop:[$start $MAXLOC] $qsep";
    ## ^^fff-overlaps-runtime:   16.02 real         0.02 user         0.03 sys
    $luq .= "+range.start+range.stop:[$start $stop] $qsep"; 
    ## ^^fff-start+stop-runtime:    7.97 real         0.04 user         0.01 sys
    }
    
  } elsif (defined($start)) {
    if ($self->{is_gff}) {
    $luq .= "+start:[$start TO $MAXLOC] $qsep";
    } else {
    $luq .= "+range.start:[$start TO $MAXLOC] $qsep";
    }
  } elsif (defined($stop)) {
    if ($self->{is_gff}) {
    $luq .= "+stop:[$MINLOC TO $stop] $qsep";
    } else {
    $luq .= "+range.stop:[$MINLOC TO $stop] $qsep";
    }
  }
  if(DEBUG) { print STDERR "# lyqueryLocation #### ".$luq." ###" };
  return $luq;
}



sub _lusearch {
  my $self= shift;
  my( $query)= @_;
  
  my $luq="";
  if (ref($query) =~ /HASH/) {
    foreach my $fld (sort keys %$query) {
      my $val= $query->{$fld}; 
      $luq .= "+($fld:$val) " if ($val);
      }
  } elsif (ref($query) =~ /ARRAY/) {
    my $n= @$query;
    for( my $i=0; $i<$n; $i += 2) {   
      my $fld= $query->[$i]; 
      my $val= $query->[$i+1]; 
      $luq .= "+($fld:$val) " if ($val);
      }
  
  } else {
    $luq= $query; ## case for plain string
    }
    
  return [] unless($luq =~ /\w/);
 
  my $outfields= $self->{outfields}; #"chr start feature gene map range id db_xref notes";
  my $command="format table;fields $outfields;find $luq";
  $self->{resultcolumns} = $outfields;
  $self->{query} = $luq;
  
  my $featlist= $self->solrSearch($rootpath, $command,  $self->{outfields2feature});
  
  ## ^ needs option to read id/field-values for query from file/stdin
  return $featlist;
}


sub _segjson2array {
my $self= shift;
my($jsondoc)= @_;
my @cols;
#chr feature start stop strand
$cols[0] = $jsondoc->{chr};
$cols[1] = $jsondoc->{feature};
$cols[2] = $jsondoc->{start}[0];
$cols[3] = $jsondoc->{end}[0];
$cols[4] = ($jsondoc->{strand}=~/[-]/) ? '-' : '+'; 
return (\@cols);
}

sub _luabscoords {
  my $self= shift;
  my($name,$class,$refseq)= @_;
  
  my $luq="";
   # name is   name:position  , class is correct , must split name? to write with coor constraint
  $luq .= $self->_luqueryName($name);
  $luq .= $self->_luqueryType($class);
  return [] unless($luq =~ /\w/);

    
  my $outfields= $self->{segfields}; # chr feature start stop strand 
  my $command="format table;fields $outfields;find $luq";
  $self->{resultcolumns} = $outfields; # turn into [list..]?
  $self->{query} = $luq;

  my $seglist= $self->solrSearch($rootpath, $command, \&_segjson2array);
  return $seglist;
}

sub _termlist2array {
  my $self= shift;
  my($line)= @_;
  # block this end result: for total docs 295581
  return () if ($line =~ m/for total/);
  my ($count,$term)= split(/\s+/,$line, 2);
  $term =~ s/^\w+://; # there is leading fieldname: on terms; leave in or not?
  return ([$term,$count]);
}

sub _lutermlist {
  my $self= shift;
  my( $field, $term)= @_;
  return [] unless($field);
  $term="" unless (defined $term);
  $field =~ s/^[+-]*//; # in case this is from query +field:value
  $field =~ s/ $qsep//g;  # qpart separator for QFilter
  if ($field =~ /:/ && !$term) {
    ($field,$term)= split(/:/,$field, 2);
    }
  $term .= "*" unless($term =~ m/\*/); 
  $term= lc($term) if ($LUBINDEBUG);
  my $command="list terms $field:$term";
  my $termlist= $self->solrSearch($rootpath, $command, \&_termlist2array);
  
  return $termlist;
}

sub unescape {  # from GFF.pm
  my $v = shift;
  $v =~ tr/+/ /;
  $v =~ s/%([0-9a-fA-F]{2})/chr hex($1)/ge;
  return $v;
}


sub do_query {
  my $self= shift;
  my( $query, @args)= @_;
  
  $self->{as_array}= 1;
  my $featlist = $self->_lusearch($query);
  $self->{as_array}= 0;
  
  if ( $self->{needgroupsort} && grep( /orderby=gname/, @args) ) {
    @{$featlist} = sort { 
         $a->[GNAME]  cmp  $b->[GNAME] 
      } @{$featlist};
    }
  
  my $iterator= $self->_iterator->new($self->{adaptor},$featlist,$self->{resultcolumns});
  return $iterator;
}

# supposed to quote args into query ?
sub dbi_quote {
  my $self = shift;
  my( $query, @args)= @_;
  # put quoted args into query at '?' locs ?
  
  return $query;
}

sub quote {
  my $self = shift;
  my( $query)= @_;
  return $query;
}

sub errstr {
  my $self = shift;
  return $self->{errstr} || '';
}

sub do {
  my $self = shift;
  my( $query, @args)= @_;
  # put quoted args into query at '?' locs ?
  
  return 0;
}

#-----------------



package Bio::DB::GFF::Adaptor::LucegeneIterator;
# sub _iterator

use strict;
use constant FACT         => 0;
use constant FEAT         => 1;
use constant COLNAMES     => 2;
use constant FEAT2        => 3;
#use constant FN           => 3;
#use constant FI           => 4;
use constant CALLBACK    => 4;
use constant CACHE       => 5;
use constant DEBUG => 0; 

use constant NOATTR => 1; 

sub new {
  my $class  = shift;
  my ($factory,$features,$colnames,$callback)= @_;
  if (NOATTR) {
  return bless [ $factory,$features,$colnames,[],$callback,[] ],$class;
  } else {
  my @feat2= @$features; # make copy for reuse; see below
  return bless [$factory,$features,$colnames,\@feat2,$callback,[] ],$class;
  }
#  my ($n,$i)= (scalar(@$features), 0);  # this slows down considerably !
#  return bless [$factory,$features,$colnames,$n,$i],$class;
}

sub callback { 
  my $self = shift;
  #my $old = $self->[CALLBACK];
  $self->[CALLBACK]= shift if @_;
  return $self->[CALLBACK]; # $old ?
}

## default gff columns for range_query:
##         my($fref,$fstart,$fstop,$fsource,$fmethod,$fscore,
##          $fphase,$gclass,$gname,$tstart,$tstop,$fid,$gid,
##          $fclass,$fattr)= @row;

sub fetchrow_array { # dbi::sth
  my $self = shift;
  return unless @{$self->[FEAT]};
  my $row = shift @{$self->[FEAT]}; # here is \@array of fields - right ones?
#   my $i= $self->[FI]++;
#   return if ($i >= $self->[FN]);
#   my $row = $self->[FEAT]->[$i]; #? dont shift, preserve array?  
  #warn "fi= ".join(",", @$row)."\n" if DEBUG;
  return @$row;
}

sub fetchrow_arrayref { 
  my $self = shift;
  return unless @{$self->[FEAT]};
  my $row = shift @{$self->[FEAT]};  
#   my $i= $self->[FI]++;
#   return if ($i >= $self->[FN]);
#   my $row = $self->[FEAT]->[$i]; #? dont shift, preserve array?  
  return $row;
}

# $hash_ref = $sth->fetchrow_hashref; use [COLNAMES] == string now; make [list]

sub fetchall_arrayref {
  my $self = shift;
  return $self->[FEAT2]; # but above SHIFTs this down as called ! invalid later
}

sub rows {  
  my $self = shift;
  return scalar(@{$self->[FEAT]});
#   return $self->[FN] - $self->[FI];
}


sub execute {
  my $self = shift;
  my @args = @_;
  # call back to _lusearch/do_query ?
}

sub finish {
  my $self = shift;
#  $self->[FEAT]= []; # undef ?
#  $self->[FN] = $self->[FI] = 0;
}

## add Bio::DB::GFF::Adaptor::dbi::iterator methods

sub next_feature {
  my $self = shift;
  return shift @{$self->[CACHE]} if @{$self->[CACHE]};
  my $sth = $self; #$self->[STH];
  my $callback = $self->[CALLBACK]; #? what if none?

  my $features;
  while (1) {
    if (my @row = $sth->fetchrow_array) {
      $features = $callback->(@row);
      last if $features;
    } else {
      $sth->finish;
      # undef $self->[STH];
      $features = $callback->();
      last;
    }
  }
  $self->[CACHE] = $features or return;
  shift @{$self->[CACHE]};
}

*next_seq = \&next_feature;



1;

=head1 DBI adaptor for Solr indexer.Sends queries to an index server where data is stored as GFF.


  # Search for: +feature:gene +chr:2l +(start:[00000000000 TO 00001000000] stop:[00000500000 TO 00099999999])
  # Match 56 of 1004183 documents ; 8939 ms search time
  
  Solr indexes
  gff: fieldnames = chr source feature start stop score strand attributes
  
  
  gene    feature chr     start   range.stop      url
  2L      chromosome_arm  2L      1       22217931        dmel-2L-r3.2.2.fff,507-581
  
  
=cut
