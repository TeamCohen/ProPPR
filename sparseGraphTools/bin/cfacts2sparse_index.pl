#!/usr/bin/perl
#
# Input: cfacts file named functor_arg1type_arg2type.cfacts, with lines like
# functor[TAB]arg1[TAB]arg2
# -or-
# functor[TAB]arg2[TAB]arg2[TAB]weight
# 
# Implicit input: index file named arg1type.i, with lines like
# aaaarg1
# bbbarg1
# cccarg1
# ...
# (so in lex order)
#
# Implicit input: index file named arg2type.i, same format
#
# Intermediate output: temp file with lines like
# arg1_id[TAB]arg2[TAB]weight
# 
# Output: functor_arg1type_arg2type.index, with lines like
# arg1_id[TAB]arg2_id[TAB]weight
#

my $facts = shift;
my ($head,$tail) = split(/\./,$facts);
my ($functor,$arg1type, $arg2type) = split(/_/,$head);
print "$facts: $functor, $arg1type, $arg2type\n";

open(my $fa1,"<$arg1type.i") or die "Couldn't open index file '$arg1type.i' for reading arg1 of $facts:\n$!\n";
open(my $fo,">tmp") or die "Couldn't open tmp file for writing:\n$!\n";
open(my $ff,"<$facts") or die "Couldn't open facts file '$facts' for reading:\n$!\n";
my %arg,$i=0;
print "Reading from $arg1type.i...\n";
while(<$fa1>) {
    chomp;
    $arg{$_}=$i;
    $i++;
}
close($fa1);

print "Reading from $facts; writing to tmp...\n";
while(<$ff>) {
    chomp;
    my ($functor, $src, $dst, $wt) = split;
    print $fo "$arg{$src}\t$dst";
    $wt and print $fo "\t$wt";
    print $fo "\n";
}
close($ff);
close($fo);

%arg=();
$i=0;
open(my $fa2,"<$arg2type.i") or die "Couldn't open index file '$arg2type.i' for reading arg2 of $facts:\n$!\n";
print "Reading from $arg2type.i...\n";
while(<$fa2>) {
    chomp;
    $arg{$_}=$i;
    $i++;
}
close($fa2);

open($ff,"<tmp") or die "Couldn't open tmp file for reading:\n$!\n";
open($fo,">$head.index") or die "Couldn't open $head.index for writing:\n$!\n";
print "Reading from tmp, writing to $head.index...\n";
while(<$ff>) {
    chomp;
    my ($src,$dst,$wt) = split;
    print $fo "$src\t$arg{$dst}";
    $wt and print $fo "\t$wt";
    print $fo "\n";
}
close($ff);
close($fo);
print "Done.\n";
