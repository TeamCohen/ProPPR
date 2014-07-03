#!/usr/bin/perl

my $querfile=shift;
my $solfile=shift;
my $unreachablefile=shift;
my $trainfile=shift;
defined($trainfile) or die "Usage:\n\tqueryfile solutionsfile unreachablefile trainingfile\n\tqueryfile: input\n\tsolutionsfile: input\n\tunreachablefile: output\n\ttrainingfile: output\n";

open(my $qf,"<$querfile") or die "Couldn't read queryfile $querfile:\n$!\n";
open(my $sf,"<$solfile") or die "Couldn't read solutionsfile $solfile:\n$!\n";
open(my $uf,">$unreachablefile") or die "Couldn't open unreachablefile $unreachablefile for writing:\n$!\n";
open(my $tf,">$trainfile") or die "Couldn't open trainingfile $trainfile for writing:\n$!\n";

my $queryLine,$query,$ex,$pos,$reached=0,$L=0,$k=1,$Q=0,$F=0;
my $neg;
while(<$sf>) {
    chomp;
    $L++;
    if (/^#/) {
	if ($queryLine ne "") {
	    $Q++;
	    if ($reached == 0) { 
		print "Didn't reach positive example for $query: $pos\n"; $F++; 
		print $uf "$query\n";
		print $tf "#";
	    }
	    writeLabels($tf,$query,$ex);
	    $reached = 0;
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
	next;
    }
    my ($rank,$score,$sol) = split "\t";
    $sol = substr($sol,5,length($sol)-6);
    if ($pos eq $sol) {
	$reached=1;
	next;
    }
    my $k2 = $k*$k;
    if ($rank ge "$k2") { # greater than or equal to
	# add solutions of rank k^2 for k=1,2,3...
	$ex = "$ex\t-$neg$sol)";
	$k++;
    }
    print "processed $L lines\n" unless ($L % 10000);
}
if ($queryLine) {
    $Q++;
    if ($reached == 0) { print "Didn't reach positive example for $query: $pos\n"; $F++; }
    writeLabels($tf,$query,$ex);
}

print "processed $L lines of $solfile\n";
print "$Q queries\n";
print "$F unreachable solutions\n";

close($qf);
close($sf);
close($tf);

sub writeLabels {
    my $fp = shift;
    my $query = shift;
    my $ex = shift;
    print $fp "$query\t$ex\n";
}
    
