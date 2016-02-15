#!/usr/bin/perl

my $groundedFile=shift;
my $keyFile=shift;
defined($keyFile) or die "Usage:\n\ttrainOrTest.examples.grounded trainOrTest.examples.graphKey > query_feature_src_dst.txt\n";

open(my $cF,"<$groundedFile") or die "Couldn't open file $groundedFile for reading:\n$!\n";
open(my $fF,"<${groundedFile}.features") or die "Couldn't open file ${groundedFile}.features for reading:\n$!\n";
open(my $kF,"<$keyFile") or die "Couldn't open key file $keyFile for reading:\n$!\n";

my @features=(0);
while(<$fF>) {
	chomp;
	push @features,$_;
}

my $c=0;
my $k=0;
my $kcache=<$kF> or die "End of key file reached before I could even get started\n";
$kcache =~ s/-[0-9][0-9]*/_/;
# for each line of the grounded file, gather all the edges by feature
while(<$cF>) {
    chomp;
    $c++;
    my %edgedata; # feature -> ( [src,dst] )
    my $ne=0;
    $_=~s/-[0-9][0-9]*/_/;
    my ($query,$foo2,$foo3,$foo4,$foo5,$foo6,$foo7,@edges) = split("\t");
    
    my $searchi = -1;
    foreach my $feature (@features) {
	$edgedata{$feature} = ();
    }
    foreach my $e (@edges) {
	my @foo = split(":",$e);
	my @bar = split("->",$foo[0]);
	foreach my $f ( split(",", $foo[1]) ) {
	    push(@{$edgedata{$features[$f]}}, \@bar);
	}
    }
    
    # then gather the node labels for this query
    $ne=0;
    my @nodeNames;
    while(1) {
	my $key = $kcache;
	$k++;
	chomp($key);
	my ($kquery,$nodeid,$name) = split("\t",$key);
	($kquery eq $query) or die "Mismatch between line $c of grounded file and line $k of key file:\n$query\n$kquery\n";
	#print "$k: $nodeid -> $name\n";
	$nodeNames[$nodeid] = $name;
	$ne++;
	#print "$k: $kcache";
	
	# preload next line:
	last unless ($kcache = <$kF>);
	$kcache =~ s/-[0-9][0-9]*/_/;
	#print "\t$k+1: $kcache";
	chomp($kcache);
	($kquery,$nodeid,$name) = split("\t",$kcache);
	#my $yes="keep going";
	#$yes="stop" if ($kquery ne $query);
	#print "\t$k+1: $yes\n";
	last if ($kquery ne $query);
    } 
    #print "$ne node names retrieved at line $k of key file\n";

    # then print the edge data
    $ne=0;
    foreach my $feature (keys %edgedata) {
	foreach my $e_ref ( @{$edgedata{$feature}} ) {
	    #print "$e_ref\n";
	    my $src = $nodeNames[$e_ref->[0]];
	    my $dst = $nodeNames[$e_ref->[1]];
	    print "$query\t$feature\t$e_ref->[0]\t$e_ref->[1]\t$src\t$dst\n";
	    $ne++;
	}
    }
}

close($cF);
close($kF);
