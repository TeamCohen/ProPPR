#!/usr/bin/perl

my $file=shift;
my ($head,$tail) = split(/\./,$file);
my $rofile = "$head.rowOffset";
my $cifile = "$head.colIndex";

open(my $fi,"<$file") or die "Couldn't open $file for reading:\n$!\n";
open(my $ro,">$rofile") or die "Couldn't open $rofile for writing:\n$!\n";
open(my $ci,">$cifile") or die "Couldn't open $cifile for writing:\n$!\n";
my $n=0,$lastrow=-1;
while(<$fi>) {
    chomp;
    my ($src,$dst) = split;
    if ($src > $lastrow) {
	while($lastrow < $src) {
	    $lastrow++;
	    print $ro "$n\n";
	    #print "line=$n src=$src ro=$n \n"
	}
    }
    print $ci "$dst\n";
    $n++;
}
