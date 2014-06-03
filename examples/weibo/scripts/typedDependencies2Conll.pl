#!/usr/bin/perl
# This script is to convert column words to sentences.
# yww@cs.cmu.edu
# William Wang

$tpath = $ARGV[0];  # words

open (T, $tpath) or die "cannot open the input file";

@lines= <T>;
$prev_index = 0;
$current = 1;

foreach $line (@lines)
{
   $line =~s/\n//;
   if($line!~/^ *$/)
   {
      @tokens = split(/,/,$line);
            $cur = @tokens[1];
            $cur =~s/\)//g;
            @parts = split(/\-/,$cur);
            $index = @parts[1];
            $word = @parts[0];
            $parent = @tokens[0];
            @items = split(/\(/,$parent);
            $name = @items[0];
            $rawp = @items[1];
            @stuff = split(/\-/,$rawp);

            if($current == 1 && $index <= $prev_index) 
            {
               next;
            }
           else{
               print "$index\t$word\t_\t_\t_\t_\t @stuff[1]\n";
           }

            if($index==1) {$current=1;}
            $prev_index = $index;
   }
   else
   {
       $current = 0;
      print "\n";
   }
}
close T;