#!/usr/bin/perl

my $querfile=shift;
my $solfile=shift;
my $labeledfile=shift;
defined($labeledfile) or die "Usage:\n\tqueryfile solutionsfile labeledfile\n\tqueryfile: input\n\tsolutionsfile: input\n\tlabeledfile: output\n";

open(my $qf,"<$querfile") or die "Couldn't read queryfile $querfile:\n$!\n";
open(my $sf,"<$solfile") or die "Couldn't read solutionsfile $solfile:\n$!\n";
open(my $tf,">$labeledfile") or die "Couldn't open labeledfile $labeledfile for writing:\n$!\n";

my $queryLine,$query,$ex,$pos,$reached=0,$L=0,$k=1,$Q=0,$F=0;
my $neg;
while(<$sf>) {
    chomp;
    $L++;
    print $tf "$_";
    if (/^#/) {
	if ($queryLine ne "") {
	    $Q++;
	    $k=1;
	}

	$queryLine = <$qf>; 
	last unless $queryLine;
	chomp($queryLine);
	
	($query,$pos) = split("\t",$queryLine);
	$ex = $pos;
	my $ci = rindex($pos,",")+1;
	$pos = substr($pos,$ci,length($pos)-1-$ci);
	$neg = substr($query,0,rindex($query,",")+1);

	my ($junk,$provedq,$junk) = split "\t";
	if ( substr($query,0,rindex($query,"E")) ne substr($provedq,0,rindex($provedq,"-"))) {
	    print "No match at line $L:\n$query\n$provedq\n";
	    last;
	}
	print $tf "\n";
	next;
    }
    my ($rank,$score,$sol) = split "\t";
    $sol = substr($sol,5,length($sol)-6);
    if ($pos eq $sol) {
	print $tf "\t+\n";
    } else {
	print $tf "\t-\n";
    }
    print "processed $L lines\n" unless ($L % 10000);
}
if ($queryLine) {
    $Q++;
}

print "processed $L lines of $solfile\n";
print "$Q queries\n";
#print "$F unreachable solutions\n";

close($qf);
close($sf);
close($tf);
