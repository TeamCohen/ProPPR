#!/usr/bin/perl

my $cookedFile=shift;
defined($cookedFile) or die "Usage:\n\ttrainOrTest.examples.cooked > trainOrTest.examples.ff\n";

open(my $cF,"<$cookedFile") or die "Couldn't open file $cookedFile for reading:\n$!\n";
my %ff;
my $ne=0;
while(<$cF>) {
    chomp;
    my ($foo1,$foo2,$foo3,$foo4,$foo5,$foo6,$featurestr,@edges) = split("\t");
    my @features = split(":",$featurestr);
    
    foreach my $f (@features) { $ff{$f}++; }

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
    print "$portion $f\n";
}
