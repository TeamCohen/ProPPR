#!/usr/bin/perl
# This script is to compute the UAS from answers and data.
# yww@cs.cmu.edu
# William Wang

$tpath = $ARGV[0];  # answers
$dpath = $ARGV[1];  # reference

open (T, $tpath) or die "cannot open the answer file";
open (D, $dpath) or die "cannot open the data file";

@ans = <T>;
@data = <D>;

$correct = 0;
$all = 0;

for($i=0;$i<@ans;$i++)
{
 
    $line=@ans[$i];
    $line=~s/\n//;
    if($line!~/^[0-9]/) {next;}
    else
    {
      @tokens=split(/\s+/,$line);
      $a = @tokens[6];       
   
      $ref = @data[$i];
      @parts=split(/\s+/,$ref);
      $all++;
      if($a==@parts[6]) {$correct++;}
    }
}

$acc = $correct/$all;

print "UAS: $acc\n";


