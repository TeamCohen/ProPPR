#!/usr/bin/perl
# This script is to compute the UAS from answers and data.
# yww@cs.cmu.edu
# William Wang

$tpath = $ARGV[0];  # answers

open (T, $tpath) or die "cannot open the answer file";

@ans = <T>;
$alen = @ans;

$max = 0;
$sent = "";

for($i=0;$i<@ans;$i++)
{
    $line=@ans[$i];
    $line=~s/\n//;
    if($line=~/^\#/) 
    {
       @tokens=split(/\s+/,$line);
       $q = @tokens[3];              
       $q =~s/edge\(//;
       $q =~s/,.*//;   
       $sent=$q;
	$sent=~s/t.*//;    
    }
    
    if(($i<$alen-2) && (@ans[$i+1]=~/^1/) && (@ans[$i+1]!~/root/))
    {
       $pred = @ans[$i+1];
       $pred =~s/\n//;
       @parts = split(/\s+/,$pred);
       $p = @parts[2];
       $p=~s/.*\[//;
       $p=~s/\]//;
       $all++;
       if($keys{$q} eq $p) {$correct++;}
    }
    
}

$acc = $correct/$all;

print "UAS: $acc\n";


