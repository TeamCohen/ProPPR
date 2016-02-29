#!/usr/bin/perl

my $groundedFile=shift;
defined($groundedFile) or die "Usage:\n\ttrainOrTest.examples.grounded > trainOrTest.examples.ff\n";

open(my $cF,"<$groundedFile") or die "Couldn't open file $groundedFile for reading:\n$!\n";
open(my $fF,"<${groundedFile}.features") or die "Couldn't open file ${groundedFile}.features for reading:\n$!\n";
my @features=(0);
while(<$fF>) {
	chomp;
	push @features,$_;
}
my %ff;
my $ne=0;
while(<$cF>) {
    chomp;
    my ($fooq,$fooqid,$foopid,$foonid,$foosn,$foose,$foosld,@edges) = split("\t");
    #my @features = split(":",$featurestr);
    #foreach my $f (@features) { $ff{$f}++; }

    foreach my $e (@edges) {
	my @foo = split(":",$e);
	foreach my $f ( split(",", $foo[1]) ) {
	    $ff{$features[$f]}++;
	}
	$ne++;
    }
    
}

foreach my $f (keys %ff) {
    my $portion = $ff{$f}/$ne;
    print "$portion $ff{$f} $f\n";
}
