#!/usr/bin/perl
# This script is to generate the conll format from part-of-speech tagged text files.
# yww@cs.cmu.edu
# William Wang

$tpath = $ARGV[0];  # text

open (T, $tpath) or die "cannot open the input file";

@lines= <T>;

foreach $line (@lines)
{
   $index = 1;
   $line =~s/\n//;
   @tokens = split(/\s+/,$line);
   foreach $token (@tokens)
   {
	@parts = split(/\#/,$token);
	print "$index\t@parts[0]\t@parts[0]\t@parts[1]\t@parts[1]\t-\t-\t-\t-\t-\n";
	$index++;
   }
   print "\n";
}

close T;