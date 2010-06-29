use 5.008008;
use ExtUtils::MakeMaker;
# See lib/ExtUtils/MakeMaker.pm for details of how to influence
# the contents of the Makefile that is written.
WriteMakefile(
    NAME              => 'Bio::DB::GFF::Adaptor::solr',
    VERSION      => '0,01', # finds $VERSION
    PREREQ_PM         => { 'JSON' => 2,21,
    						'LWP::Simple' => 1.41,
    						'URI::Escape' => 3.28,
    						'Bio::DB::GFF' => 1.006001
    					 }, # e.g., Module::Name => 1.1
    ($] >= 5.005 ?     ## Add these new keywords supported since 5.005
      (ABSTRACT_FROM  => 'lib/Bio/DB/GFF/Adaptor/solr.pm', # retrieve abstract from module
       AUTHOR         => 'olivier sallou <osallou@irisa.fr>') : ()),
);
