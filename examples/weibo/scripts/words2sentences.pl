#!/usr/bin/perl
# This script is to convert column words to sentences.
# yww@cs.cmu.edu
# William Wang

$tpath = $ARGV[0];  # words

open (T, $tpath) or die "cannot open the input file";

@lines= <T>;

foreach $line (@lines)
{
   $line =~s/\n//;
   if($line!~/^ *$/)
   {
      print "$line ";
   }
   else
   {
      print "。\n";
   }
}
close T;