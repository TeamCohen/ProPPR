#!/usr/bin/perl

my $groundedFile = shift;

defined($groundedFile) or die "Usage:\n\texamples.grounded > output\n";

open(my $cf,"<$groundedFile") or die "Couldn't open grounded input file $groundedFile:\n$!\n";
open(my $fF,"<${groundedFile}.features") or die "Couldn't open file ${groundedFile}.features for reading:\n$!\n";

my @features=(0);
while(<$fF>) {
	chomp;
	push @features,$_;
}

my %all_labels = ();
my $line=0;
while(<$cf>) {
    chomp;
    $line++;
    my ($query,$qid,$posid,$negid,$nodesize,$edgesize,$labelDeps,@edges) = split("\t");
    my @pos = split(",",$posid);
    my @neg = split(",",$negid);

    # build graph
    my %graph;
    foreach my $e (@edges) {
	my @parts = split (":",$e);
	my @neighbors = split("->",$parts[0]);
	my @labels = split(",",$parts[1]);
	foreach $el (@labels) {
	    push ( @{$graph{$neighbors[0]}{$el}}, $neighbors[1] );
	}
    }

    my %is_pos=();
    @is_pos{@pos} = (1) x scalar @pos;
    my %is_neg=();
    @is_neg{@neg} = (1) x scalar @neg;
    my %label_data = ();

    # walk from query node
#    print "pos ids:\t$posid\n";
#    print "neg ids:\t$negid\n";
    my %seen = ();
    walk($qid,\%graph,\%seen,[],\%is_pos,\%is_neg,\%label_data);
#    print "label\t#pos\t#neg\n";
    foreach my $el (keys %label_data) { 
#	print "$el\t$label_data{$el}{pos}\t$label_data{$el}{neg}\n"; 
	$fn = $features[$el];
	$all_labels{$fn}{count}{pos} += $label_data{$el}{pos};
	$all_labels{$fn}{count}{neg} += $label_data{$el}{neg};
	if ($label_data{$el}{pos} > 0) {
	    push @{$all_labels{$fn}{queries}{pos}},$line;
	}
	if ($label_data{$el}{neg} > 0) {
	    push @{$all_labels{$fn}{queries}{neg}},$line;
	}
    }
    if ( ( $line % 50) == 0) {
	print STDERR "$line queries read...\r";
    }

#    last;
    # print graph
#    foreach my $src (keys %graph) {
#	foreach my $label (keys %{$graph{$src}}) {
#	    foreach my $dst (@{$graph{$src}{$label}}) {
#		print "$src\t$label\t$dst\n";
#	    }
#	}
#    }
#    last;
}
close($cf);

print "\n";
foreach my $el (keys %all_labels) {
    print "$el\t$all_labels{$el}{count}{pos}\t$all_labels{$el}{count}{neg}\n";
}


sub walk {
    my ($node, $graph, $seen, $path, $is_pos, $is_neg, $label_data) = @_;
#    print "$node:";
#    foreach my $el (@{$path}) {print "$el,";}
#    print "\n";
    $seen->{$node}=1;
    check($node,$is_pos,"pos",$path,$label_data);
    check($node,$is_neg,"neg",$path,$label_data);
    foreach my $label (keys %{$graph->{$node}}) {
	push @{$path},$label;
	foreach my $next (@{$graph->{$node}{$label}}) {
	    next if ($seen->{$next});
	    walk($next,$graph,$seen,$path,$is_pos,$is_neg,$label_data);
	}
	pop @{$path};
    }
}

sub check {
    my ($node, $is_x, $key, $path, $label_data) = @_;
    if ($is_x->{$node}) {
	foreach my $el (@{$path}) {
	    $label_data->{$el}{$key}++;
	}
    }
}
