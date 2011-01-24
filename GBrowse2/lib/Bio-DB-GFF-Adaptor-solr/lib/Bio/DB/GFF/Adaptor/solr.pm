package Bio::DB::GFF::Adaptor::solr;

=head1 NAME

Bio::DB::GFF::Adaptor::solr - Database adaptor for a 
  solr indexed genome feature table set


=head1 SYNOPSIS

  use Bio::DB::GFF;
  my $db = Bio::DB::GFF->new(-adaptor=> 'solr',
                             -BANK     GenBank
                            );
                            
  For Gbrowse.conf/genbank.conf, add these:
     db_adaptor    = Bio::DB::GFF
     db_args       = 
        -adaptor solr
        # optional, define bank for search
        -BANK     GenBank
        # optional, solr index server to query, default to locahost
        -server_host localhost
        # optional, solr index server port to query, default to 8080
        -server_port 8080


See L<Bio::DB::GFF> for other methods.

  
=head1 DESCRIPTION

This adaptor implements a solr indexed version of Bio::DB::GFF.  
It inherits from Bio::DB::GFF. Code has been mainly copied and adapted. As such , some functions
may not work "alone", and usage is only guaranted within a GBrowse2 usage for the moment
Lots of code cleaning still need to be done

=head1 METHODS

See L<Bio::DB::GFF> for inherited methods.

=head1 BUGS

#24/01/11 O.SALLOU #3164609

=head1 SEE ALSO

L<http://lucene.apache.org/>, L<http://www.genouest.org/>,
L<Bio::DB::GFF>, L<bioperl>

=head1 AUTHOR

Olivier Sallou<lt>olivier.sallou@irisa.fr<gt>.
as modified from Bio::DB::GFF::Adaptor::lucegene.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.

=cut

use strict;


use Bio::DB::GFF;
use Bio::DB::GFF::Util::Rearrange; # for rearrange()
use File::Basename 'dirname';

use Bio::DB::GFF::Adaptor::SolrDBI;    #  and GFF independent 

use Bio::DB::GFF::Adaptor::dbi::iterator; #  works ok

use constant DEBUG  => 0;
use constant NOATTR => 0;
  
use LWP::Simple;
use JSON;

  
use constant MAX_SEGMENT => 100_000_000;  # the largest a segment can get
  
use vars qw(@ISA $debug);
@ISA =  qw(Bio::DB::GFF);


sub patch_GFF_Feature {
  local $^W = 0;
    eval <<'END';
use Bio::DB::GFF::Feature;
sub Bio::DB::GFF::Feature::display_name {
  my $self = shift;
  my $d    = $self->{display_name};
  $self->{display_name} = shift if @_;
  $d;
}
*info         = \&display_name;

# replace repeated, slow calls to do_attributes()
sub Bio::DB::GFF::Feature::attributes {
  my $self = shift;
  my ($tag)= @_;
  my $at = $self->{attributes}; # array ref
  unless( defined $at ) {
    defined(my $id = $self->id) or return;
    my $factory = $self->factory;
    my @atary= $factory->attributes($id);
    $self->{attributes} = $at = \@atary;  
    }
    
  my @result= @$at;
  if ($tag) {
    my @values=();
    while (my($key,$value) = splice(@result,0,2)) {
      push @values, $value if ($key eq $tag); 
      }
    return (wantarray) ? @values : $values[0];
  } elsif (wantarray) {
    return @result;
  } else {
    my %result=();
    while (my($key,$value) = splice(@result,0,2)) {
      push @{$result{$key}},$value;  
      }
    return \%result;
  }
}

sub Bio::DB::GFF::Feature::set_attributes {
  my $self = shift;
  ## convert from array of [key,val] to tuple array of (key,val)
  my $atary = shift;
  return unless ref($atary);
  my @result=();
  foreach my $attr (@$atary) { push @result, @$attr;  }
  $self->{attributes} = \@result;  # array ref to (key,val) list now;
}

END
  warn $@ if $@;
}

BEGIN {
  patch_GFF_Feature();
}



sub _lusearch { return shift->{handler}->_lusearch(@_); }
sub _luqueryType { return shift->{handler}->_luqueryType(@_); }
sub _luqueryId { return shift->{handler}->_luqueryId(@_); }
sub _luqueryName { return shift->{handler}->_luqueryName(@_); }
sub _luqueryLocation { return shift->{handler}->_luqueryLocation(@_); }
sub _luqueryNotes { return shift->{handler}->_luqueryNotes(@_); }
sub _lutermlist { return shift->{handler}->_lutermlist(@_); }
sub _luabscoords { return shift->{handler}->_luabscoords(@_); }
sub _featarray2hash { return shift->{handler}->_featarray2hash(@_); }
sub dbh {  return shift->{handler}; }

  
  
sub new {
  my $class = shift ;
  
  ## dang rearrange makes all args UPPERCASE !
  my ($file,$fasta,$db,$dbdir,$indexformat,$preferred_groups,$args) = rearrange([
							  [qw(GFF FILE)],
							  'FASTA',
							  [qw(DSN DB)],
						    [qw(DIR DIRECTORY)],
						    [qw(FORMAT INDEXFORMAT)],
							  'PREFERRED_GROUPS',
						    #? replace fasta or not with raw chr dna files# 
						    #[qw(DNA_LIB DNA)],
							 ],@_);

	

	my $self= $args; # is it hash ref ??
	$self->{data}=[];
	bless $self, $class;



  $self->preferred_groups($preferred_groups) if defined $preferred_groups;

  $self->dna_lib() if(defined $self->{DNA_LIB});
  # also need -DNAGLOB == *.raw option 

  $debug = 0;
  $debug= $self->{DEBUG} if(defined $self->{DEBUG});
  
  $self->{handler}= Bio::DB::GFF::Adaptor::SolrDBI->new( 
      FORMAT => $indexformat, adaptor => $self);
  $self->{needgroupsort}= $self->{handler}->{needgroupsort} || 0;


  if ($debug) {
  warn("GFF::Adaptor::solr  db=$db\n");
  foreach my $k (sort keys %$self) {
    print STDERR " $k => ",$self->{$k},"\n";
    }
  }
  return $self;
}



## for chromosome.raw seq files; require chrom.raw files named with obvious $id
## or use Bio::DB::Fasta (doesn't do gzipped; need chrom-id > filename mapping)
sub dna_lib {
  my $self  = shift;
  my $dnadir = shift || $self->{DNA_LIB};


  $self->{dna}= '';

  my $fglob= $self->{DNAGLOB} || '*/*/*.raw';
  #TODO  FIX WHEN RAW DATA IS AVAILABLE AS INDEXED DATA
  if (-d $dnadir) {
    my @files = glob("$dnadir/$fglob");

    opendir(D,$dnadir);
    while(my $d= readdir(D)) {
     next unless($d =~ m/^\w/); # not ..
     my $dd="$dnadir/$d";
     if ( -d $dd) {
       my @dfiles= glob("$dd/$fglob");
       push(@files, @dfiles) if @dfiles;
       }
     }
    close(D);

    #warn "dnafiles: ",@files," \n" if $debug;
    warn "no  dna files matching $fglob in $dnadir" unless @files;
    $self->{dnafiles}= \@files; # append dnadir ?

  }
}


# Loads DNA from backend storage, concatenate shards if any
sub get_dna_from_raw {
  my $self = shift;
  my ($id,$start,$stop,$class) = @_;
  # start = 1-based; 0-based for seek
  return unless(defined $stop);
  $start = 1 if !defined $start;
  my $reversed= 0;
  if ($start>$stop) {
    $reversed=1;
    ($start,$stop)= ($stop,$start);
    }

  my $url = $self->{STORAGEURL}.$id;
  my $content = get $url;
  warn "Couldn't get $url" unless defined $content;
  return unless defined $content;

  my $jsonDna = decode_json $content;
  my $alldna = $jsonDna->{content};

  my $shardsref = $jsonDna->{shards};
  
  my @shards;
  my $shardSize=0;

  if($shardsref) {
  @shards = @$shardsref;
  $shardSize = @shards;
  }
  
  
  if(@shards && $shardSize>0) {
    if(DEBUG) {
       warn "Loading shards for raw dna, nb shards = ".$shardSize;
    }
      for my $shard (@shards) {
          $url = $self->{STORAGEURL}.$shard;
          $content = get $url;
          warn "Couldn't get $url" unless defined $content;
          $jsonDna = decode_json $content;
          #Add shards content
          $alldna = $alldna.$jsonDna->{content};
      }
  }
  my $size= ($stop-$start+1);
  my $dna = substr $alldna, $start-1, $size;

    if ($reversed) {
      $dna =~ tr/gatcGATC/ctagCTAG/;
      $dna = reverse $dna;
      }
  return $dna;
}


#FIXME
sub load_or_store_fasta {
  my $self  = shift;
  my $fasta = shift;
  if ((-f $fasta && -w dirname($fasta))
      or
      (-d $fasta && -w $fasta)) {
    require Bio::DB::Fasta;
    my $dna_db = eval {Bio::DB::Fasta->new($fasta)} 
      or warn "No sequence available. Use -gff instead of -dir if you wish to load features without sequence.\n";
    $dna_db && $self->dna_db($dna_db);
  } else {
    $self->load_fasta($fasta);
  }
}

#FIXME
sub dna_db {
  my $self = shift;
  my $d    = $self->{dna_db};
  $self->{dna_db} = shift if @_;
  $d;
}

#FIXME
sub insert_sequence {
  my $self = shift;
  my($id,$offset,$seq) = @_;
  $self->{dna}{$id} .= $seq;
}

#FIXME
# low-level fetch of a DNA substring given its
# name, class and the desired range.
sub get_dna {
  my $self = shift;
  my ($id,$start,$stop,$class) = @_;
  if (my $dna_db = $self->dna_db) {
    return $dna_db->seq($id,$start=>$stop);
  }

  if (my $seq = $self->get_dna_from_raw($id,$start,$stop)) {
    return $seq;
    }
    
  return $self->{dna}{$id} if !defined $start || !defined $stop;
  $start = 1 if !defined $start;

  my $reversed = 0;
  if ($start > $stop) {
    $reversed++;
    ($start,$stop) = ($stop,$start);
  }
  my $dna = '';
  #FIX if dna not available , set it to empty string
  if($self->{dna}) { substr($self->{dna}{$id},$start-1,$stop-$start+1); }
  if ($reversed) {
    $dna =~ tr/gatcGATC/ctagCTAG/;
    $dna = reverse $dna;
  }

  $dna;
}

#FIXME
# this method loads the feature as a hash into memory -
# keeps an array of features-hashes as an in-memory db
sub load_gff_line {
  my $self = shift;
  my $feature_hash  = shift;
  $feature_hash->{strand} = '' if $feature_hash->{strand} && $feature_hash->{strand} eq '.';
  $feature_hash->{phase} = ''  if $feature_hash->{phase}  && $feature_hash->{phase} eq '.';
  push @{$self->{data}},$feature_hash;
}

# given sequence name, return (reference,start,stop,strand)
sub get_abscoords {
  my $self = shift;
  my ($name,$class,$refseq) = @_;

  ## why is this called 3 times w/ same params ???
  # warn "get_abscoords2: feature=$class; name=$name;\n" if $debug;  

  ## dang; GFF.pm is using default Sequence class when we dont use it;
  ## it is ignoring real refclass (3rd call here is for Sequence); 
  ## use cache? THIS WORKS.
  if ($class eq "Sequence" && defined $self->{lastseg} 
    && $self->{lastseg}->[0] eq $name ) 
    { $class= $self->{lastseg}->[1];  }
    
  # $outfields= "chr feature start stop strand name";
  # return [$ref,$class,$start,$stop,$strand]

  if (defined $self->{lastseg} 
    && $self->{lastseg}->[0] eq $name
    && $self->{lastseg}->[1] eq $class) {
    my @segcols= @{$self->{lastseg}}; # clone data ?
    warn "get_abscoords2 cached: ".join(",", @segcols)."\n" if $debug; # refseq=$refseq
    return [\@segcols] if @segcols; #[$self->{lastseg}];
    }
  my $segfeat= $self->_luabscoords($name,$class,$refseq);
  if (@$segfeat > 0) {
    my @segcols= @{$segfeat->[0]}; # clone data ?
    $self->{lastseg}= \@segcols if @segcols; #$segfeat->[0] ;
    warn "get_abscoords2 found: ".join(",", @{$segfeat->[0]})."\n" if $debug; # refseq=$refseq
  } else {
    warn "get_abscoords2 missed: feature=$class; name=$name;\n" if $debug;
    if($class=='Sequence') {
    	 $segfeat= $self->_luabscoords($name,'chromosome',$refseq);
         if (@$segfeat > 0) {
               my @segcols= @{$segfeat->[0]}; # clone data ?
               $self->{lastseg}= \@segcols if @segcols; #$segfeat->[0] ;
               warn "get_abscoords2 found: ".join(",", @{$segfeat->[0]})."\n" if $debug; # refseq=$refseq
  		 }	
    	 else {
    	 warn "get_abscoords2 missed: feature=chromosome; name=$name;\n" if $debug;
         $segfeat= $self->_luabscoords($name,'Region',$refseq);
         if (@$segfeat > 0) {
               my @segcols= @{$segfeat->[0]}; # clone data ?
               $self->{lastseg}= \@segcols if @segcols; #$segfeat->[0] ;
               warn "get_abscoords2 found: ".join(",", @{$segfeat->[0]})."\n" if $debug; # refseq=$refseq
  		 }
  		 else {
  		 	warn "get_abscoords2 missed: feature=Region; name=$name;\n" if $debug;
  		 }
  		 }
    }
  }
  return $segfeat; 
}



sub search_notes {
  my $self = shift;
  my ($search_string,$limit) = @_;
  my @results;
  my @words = map {quotemeta($_)} $search_string =~ /(\w+)/g;

  warn "search_notes: $search_string; \n" if $debug;
  my $features= $self->_lusearch( $self->_luqueryNotes($search_string));
  $self->{data}= $features;
  
  for my $feature (@{$self->{data}}) {
    next unless defined $feature->{gclass} && defined $feature->{gname}; # ignore NULL objects
    next unless $feature->{attributes};
    my @attributes = @{$feature->{attributes}};
    my @values     = map {$_->[1]} @attributes;
    my $value      = "@values";
    my $matches    = 0;
    my $note;
    for my $w (@words) {
      my @hits = $value =~ /($w)/g;
      $note ||= $value if @hits;
      $matches += @hits;
    }
    next unless $matches;

    my $relevance = 10 * $matches;
    my $featname = Bio::DB::GFF::Featname->new($feature->{gclass}=>$feature->{gname});
    push @results,[$featname,$note,$relevance];
    last if @results >= $limit;
  }
  @results;
}


sub _delete_features {
  my $self        = shift;
  my @feature_ids = sort {$b<=>$a} @_;
  my $removed = 0;
#   foreach (@feature_ids) {
#     next unless $_ >= 0 && $_ < @{$self->{data}};
#     $removed += defined splice(@{$self->{data}},$_,1);
#   }
  $removed;
}

sub _delete {
  my $self = shift;
    my $delete_spec = shift;
  my $ranges      = $delete_spec->{segments} || [];
  my $types       = $delete_spec->{types}    || [];
  my $force       = $delete_spec->{force};
  my $range_type  = $delete_spec->{range_type};

  my $deleted = 0;
  if (@$ranges) {
    my @args = @$types ? (-type=>$types) : ();
    push @args,(-range_type => $range_type);
    my %ids_to_remove = map {$_->id => 1} map {$_->features(@args)} @$ranges;
    $deleted = $self->delete_features(keys %ids_to_remove);
  } elsif (@$types) {
    my %ids_to_remove = map {$_->id => 1} $self->features(-type=>$types);
    $deleted = $self->delete_features(keys %ids_to_remove);
  } else {
    $self->throw("This operation would delete all feature data and -force not specified")
      unless $force;
    $deleted = @{$self->{data}};
    @{$self->{data}} = ();
  }
  $deleted;
}

# attributes -

# Some GFF version 2 files use the groups column to store a series of
# attribute/value pairs.  In this interpretation of GFF, the first such
# pair is treated as the primary group for the feature; subsequent pairs
# are treated as attributes.  Two attributes have special meaning:
# "Note" is for backward compatibility and is used for unstructured text
# remarks.  "Alias" is considered as a synonym for the feature name.
# If no name is provided, then attributes() returns a flattened hash, of
# attribute=>value pairs.

sub _onefeat_attributes {
  my $self = shift;
  my ($feature, $feature_id, $tag) = @_;
  my $attr ;
  my @result;
  for my $attr (@{$feature->{attributes}}) {
    my ($attr_name,$attr_value) = @$attr ;
    if ( $tag  && $attr_name eq $tag){ push @result,$attr_value; }
    elsif (! $tag ) { push @result,($attr_name,$attr_value); }
    }
  return @result; # only 1 feat
}


sub do_attributes {
  my $self = shift;
  my ($feature_id,$tag) = @_;
  my @result;

  #?? is this time sink? yes, about 1/3 of time for 1MB range; or more:
  ## simple map  goes from 3sec to 8sec; dang;
  ## GFF::Feature really should store its attributes, not call here each time it wants them
  
  return () if NOATTR; # patched GFF::Features ; shouldnt need to call here
  
  $tag='' unless(defined($tag));
  my $lastattr= $self->{lastattr};
  if ($lastattr) {
    my($lid,$ltag,$lresult)= @$lastattr;
    if (($feature_id eq $lid) && ($tag eq $ltag)) {
      return @$lresult;
      }
    }
  warn "do_attributes for id=$feature_id,tag=$tag\n" if $debug;
  
  ## dang; want to cache features here so dont' do dblookup for attributes of 
  ## each feature.  Is this only method using cached {data} ?
  
  use constant FID     => 12; # risky ? see LucegeneDBI
  use constant FATTR   => 15;

  my $data= $self->{data}; # NOATTR in iterator; dont want time cost if not calling here
  unless(@{$data}) {
    warn "do_attributes requery for data\n" if $debug;
    my $luq = $self->_luqueryId( $feature_id);
    $data= $self->_lusearch( $luq);
    }
    
  local $^W = 0; # ignore $isarray mistakes ?
  for my $feature (@{$data}) {
    my $isarray= (ref($feature) =~ /ARRAY/);  
    my $fid= ($isarray) ? $feature->[FID] : $feature->{feature_id};  
    # warn "at fid=$fid\n" if $debug;
    next unless($fid eq $feature_id);
    
    # $feature= $self->dbh->_featarray2hash($feature) if ($isarray);
    my $attref= ($isarray) ? $feature->[FATTR] : $feature->{attributes};
    # warn "attr.$feature_id = ",$attref,"\n" if $debug;
    next unless($attref);
    
    ## return $self->_onefeat_attributes( $feature, $feature_id, $tag);
    for my $attr (@$attref) {
      my ($attr_name,$attr_value) = @$attr ;
      if ($tag && $attr_name eq $tag){ push @result,$attr_value; }
      elsif (!$tag) { push @result,($attr_name,$attr_value); }
      }
    # warn "attr.$feature_id = ",join(',',@result),"\n" if $debug;
    $self->{lastattr}= [$feature_id,$tag,\@result]; # cache for repeat calls
    return @result;  
    }
  
}


#sub get_feature_by_attribute 
sub _feature_by_attribute{
  my $self = shift;
  my ($attributes,$callback) = @_;
  $callback || $self->throw('must provide a callback argument');
  my $count = 0;
  my $feature_id = -1;
  my $feature_group_id = undef;

  # keys %$attributes -- key/val hash list
  my @vals = values(%$attributes);
  my $search_string= join(" ",@vals);
  warn "_feature_by_attribute: $search_string; \n" if $debug;
  
  my $features= $self->_lusearch( "all:($search_string)");
  $self->{data}= $features;
  
  for my $feature (@{$self->{data}}) {

    # $feature_id++;
    $feature_id= $feature->{feature_id};
    
    for my $attr (@{$feature->{attributes}}) {
      my ($attr_name,$attr_value) = @$attr ;
      #there could be more than one set of attributes......
      foreach (keys %$attributes) {
	if ($_ eq $attr_name && $attributes->{$_} eq $attr_value){
           $callback->($feature->{ref},
	        $feature->{start},
	        $feature->{stop},
	        $feature->{source},
	        $feature->{method},
	        $feature->{score},
	        $feature->{strand},
	        $feature->{phase},
	        $feature->{gclass},
	        $feature->{gname},
		$feature->{tstart},
		$feature->{tstop},
	        $feature_id,
		$feature_group_id);
	   $count++;
        }
      }
    }
  }

}



# This is the low-level method that is called to retrieve GFF lines from
# the database.  It is responsible for retrieving features that satisfy
# range and feature type criteria, and passing the GFF fields to a
# callback subroutine.


sub get_features{
  my $self = shift;
  my ($search,$options,$callback) = @_;
  
  my $sth = $self->range_query(@{$search}{qw(rangetype
					     refseq
					     refclass
					     start
					     stop
					     types)},
			       @{$options}{qw(
					      sparse
					      sort_by_group
					      ATTRIBUTES
					      BINSIZE)}) or return;
					      
  warn "get_features1: n=",$sth->rows()," \n" if $debug;
  $self->{data}=  $sth->fetchall_arrayref(); # preserve for do_attributes call

  my $count = 0;
  while (my @row = $sth->fetchrow_array) {
    $callback->(@row);
    $count++;
  }
  $sth->finish;

  return $count;
}


=head2 patched GFF::make_feature 

dgg: handle real display_name
this is general callback for all the feature fetchers


=cut

sub make_feature {
  my $self = shift;
  my ($parent,$group_hash,          # these arguments provided by generic mechanisms
      $srcseq,                      # the rest is provided by adaptor
      $start,$stop,
      $source,$method,
      $score,$strand,$phase,
      $group_class,$group_name,
      $tstart,$tstop,
      $db_id,$group_id, # orig stops here ...
      $feattype, $attributes, # dgg; dont care?
      $display_name, # here is one we need; extra data
      ) = @_;

  return unless $srcseq;            # return undef if called with no arguments.  This behavior is used for
                                    # on-the-fly aggregation.

  $display_name= $group_name unless($display_name);
  
  my $group;  # undefined
  if (defined $group_class && defined $group_name) {
    $tstart ||= '';
    $tstop  ||= '';
    if ($group_hash) {
      $group = $group_hash->{$group_class,$group_name,$tstart,$tstop}
	||= $self->make_object($group_class,$group_name,$tstart,$tstop);
    } else {
      $group = $self->make_object($group_class,$group_name,$tstart,$tstop);
    }
  }

  my $ft;
  if (ref $parent) { # note that the src sequence is ignored
    $ft= Bio::DB::GFF::Feature->new_from_parent($parent,$start,$stop,
						  $method,$source,
						  $score,$strand,$phase,
						  $group,$db_id,$group_id,
						  $tstart,$tstop);
    ## $display_name= $parent->display_name() if($method =~ /exon|UTR/); # no, this is chromosom
  } else {
    $ft= Bio::DB::GFF::Feature->new($self,$srcseq,
				      $start,$stop,
				      $method,$source,
				      $score,$strand,$phase,
				      $group,$db_id,$group_id,
				      $tstart,$tstop);
  }
  $ft->display_name($display_name); ## dgg patch
  $ft->set_attributes($attributes); ## dgg patch
  return $ft;
}



# Low level implementation of fetching a named feature.
# GFF annotations are named using the group class and name fields.
# May return zero, one, or several Bio::DB::GFF::Feature objects.

=head2 _feature_by_name

 Title   : _feature_by_name
 Usage   : $db->get_features_by_name($name,$class,$callback)
 Function: get a list of features by name and class
 Returns : count of number of features retrieved
 Args    : name of feature, class of feature, and a callback
 Status  : protected

This method is used internally.  The callback arguments are those used
by make_feature().

=cut

sub _feature_by_name {
  my $self = shift;
  my ($class,$name,$location,$callback) = @_;
  $callback || $self->throw('must provide a callback argument');
  my $count = 0;
  my $id    = -1;
  my $regexp;

  warn "_feature_by_name: $class:$name \n" if $debug;
  
  my $luq="";
  $luq .= $self->_luqueryName( $name);
  $luq .= $self->_luqueryType( $class);
  if ($location) {
    $luq .= $self->_luqueryLocation($location->[0],$location->[2],$location->[2],'overlaps');
    }
    
  my $features= $self->_lusearch( $luq);
  $self->{data}= $features;

  for my $feature (@{$self->{data}}) {
    $id= $feature->{feature_id};

    $count++;
    $callback->(@{$feature}{qw(
			       ref
			       start
			       stop
			       source
			       method
			       score
			       strand
			       phase
			       gclass
			       gname
			       tstart
			       tstop
			      )},$id,0
	       );
  }
  return $count;
}

# Low level implementation of fetching a feature by it's id. 
# The id of the feature as implemented in the in-memory db, is the location of the 
# feature in the features hash array.
sub _feature_by_id {
  my $self = shift;
  my ($ids,$type,$callback) = @_;

  warn "_feature_by_id: @$ids \n" if $debug;
  my $luq="";
  $luq .= $self->_luqueryId( $ids);
  my $features= $self->_lusearch( $luq);
  $self->{data}= $features;

  my $feature_group_id = undef;

  my $count = 0;
  if ($type eq 'feature'){
    $callback || $self->throw('must provide a callback argument');
    # for my $feature_id (@$ids){
    for my $feature (@{$self->{data}}) {
       # my $feature = ${$self->{data}}[$feature_id];  ## this is bad; featid not num index
       
       $callback->($feature->{ref},
	        $feature->{start},
	        $feature->{stop},
	        $feature->{source},
	        $feature->{method},
	        $feature->{score},
	        $feature->{strand},
	        $feature->{phase},
	        $feature->{gclass},
	        $feature->{gname},
		$feature->{tstart},
		$feature->{tstop},
	        $feature->{feature_id}, #$feature_id,
		$feature_group_id);
	   $count++;			
    
    }
  }
}


# This method is similar to get_features(), except that it returns an
# iterator across the query.  
# See Bio::DB::GFF::Adaptor::memory_iterator.

# from dbi: - this works, faster than above (lower perl-object overhead)
sub get_features_iterator {
  my $self = shift;
  my ($search,$options,$callback) = @_;
  $callback || $self->throw('must provide a callback argument');
  
  my $sth = $self->range_query(@{$search}{qw(rangetype
					     refseq
					     refclass
					     start
					     stop
					     types)},
			       @{$options}{qw(
					      sparse
					      sort_by_group
					      ATTRIBUTES
					      BINSIZE)}) or return;
					      
  warn "get_features_iterator1: n=",$sth->rows()," \n" if $debug;
  $self->{data}= $sth->fetchall_arrayref(); # preserve for do_attributes call
  
  if (1 || $sth->can('next_feature')) {  
    $sth->callback($callback); # works to replace dbi::interator with LucegeneIterator
    return $sth;
    }
    
  return Bio::DB::GFF::Adaptor::dbi::iterator->new($sth,$callback); # works
}


sub range_query {
  my $self = shift;
  my($rangetype,$refseq,$class,$start,$stop,$types,
     $sparse,$order_by_group,$attributes,$bin) = @_;

  # my $dbh = $self->features_db;
  # NOTE: straight_join is necessary in some database to force the right index to be used.
#   my %a             = (refseq=>$refseq,class=>$class,start=>$start,stop=>$stop,types=>$types,
#                        attributes=>$attributes,bin_width=>$bin);
#   my $straight      = $self->do_straight_join(\%a) ? 'straight_join' : '';
#   my $select        = $self->make_features_select_part(\%a);
#   my $from          = $self->make_features_from_part($sparse,\%a);
#   my $join          = $self->make_features_join_part(\%a);
#   my ($where,@args) = $self->make_features_by_range_where_part($rangetype,\%a);
#   my ($group_by,@more_args) = $self->make_features_group_by_part(\%a);
#   my $order_by      = $self->make_features_order_by_part(\%a) if $order_by_group;
# 
#   my $query         = "SELECT $straight $select FROM $from WHERE $join";
#   $query           .= " AND $where" if $where;
#   if ($group_by) {
#     $query           .= " GROUP BY $group_by";
#     push @args,@more_args;
#   }
#   $query           .= " ORDER BY $order_by" if $order_by;

  my $query="";
  my @args= (); #? support this?
  $query .= $self->_luqueryLocation($refseq,$start,$stop,$rangetype);
  
  $query .= $self->_luqueryType($types) if (defined $types);
  
  if (DEBUG && defined $attributes) { warn "ATTRIBUTES=$attributes\n" ; } #when is this used?
  
  push(@args, 'orderby=gname') if $order_by_group; #??
  warn "#DEBUG CALL DO QUERY WITH QUERY= ".$query if(DEBUG); 
  my $sth = $self->dbh->do_query($query,@args);

  return $sth;
}


# This method is responsible for fetching the list of feature type names.
# The query may be limited to a particular range, in
# which case the range is indicated by a landmark sequence name and
# class and its subrange, if any.  These arguments may be undef if it is
# desired to retrieve all feature types.

# If the count flag is false, the method returns a simple list of
# Bio::DB::GFF::Typename objects.  If $count is true, the method returns
# a list of $name=>$count pairs, where $count indicates the number of
# times this feature occurs in the range.

sub get_types {
  my $self = shift;
  my ($srcseq,$class,$start,$stop,$want_count,$typelist) = @_;
  my(%result,%obj);

  if (defined $srcseq or defined $start or defined $stop) {

      my $sth = $self->range_query(
          'overlaps', $srcseq, $class, $start, $stop, $typelist,
          # rangetype, refseq, refclass, start, stop, types
          undef, 0, undef, undef);
          # sparse, sort_by_group, ATTRIBUTES, BINSIZE

      while (my @row = $sth->fetchrow_array) {

        my($fref,$fstart,$fstop,$fsource,$fmethod,$fscore,
          $fphase,$gclass,$gname,$tstart,$tstop,$fid,$gid,
          $fclass,$fattr)= @row;
     
#        ## shouldnt need these tests; query restricted above
#         if (defined $srcseq){
#           next unless $fref eq $srcseq ;
#         }
#         if (defined $class){ 
#           next unless $fclass eq $class ;
#         }
#          # the requested range should OVERLAP the retrieved features
#         if (defined $start or defined $stop) {
#           $start = 1           unless defined $start;
#           $stop  = MAX_SEGMENT unless defined $stop;
#           next unless $fstop >= $start && $fstart <= $stop;
#         }
#         if (defined $typelist && @$typelist){
#           next unless _matching_typelist($fmethod,$fsource,$typelist);
#         }
    
        my $type = Bio::DB::GFF::Typename->new($fmethod,$fsource);
        $result{$type}++;
        $obj{$type} = $type;
      }
      $sth->finish;

    }
    
  else {
    $typelist="*" unless(defined $typelist);
    my $luq= $self->_luqueryType($typelist);
    # $luq =~ s/^\+//;
    # my ($fld,$val)= split(/:/,$luq); # now in _lutermlist
    my $termlist= $self->_lutermlist($luq);  # feature term dictionary w/ counts
    # now returns [ [term1,count], [term2,count], ... ]
    
    foreach my $tm (@$termlist) {
      # my ($cnt,$feat)= split(/\s+/,$tm,2);
      # $feat =~ s/^$fld://;
      my ($term,$count)= @$tm;
      my ($method,$source)= split(/:/,$term,2);
      my $type = Bio::DB::GFF::Typename->new($method,$source);
      $result{$type}= $count;
      $obj{$type} = $type;
      }
    }
    
  return $want_count ? %result : values %obj;
 
}






sub do_initialize { 1; }
sub setup_load { }
sub finish_load { 1; }
sub get_feature_by_group_id{ 1; }

1;

__END__


