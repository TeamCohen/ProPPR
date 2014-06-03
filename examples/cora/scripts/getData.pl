#!/usr/bin/perl
# This script is to convert Cora MLN format the SLD format, and generate train/test data.
# input is *.raw, output is sld format data

$fpath = $ARGV[0];  # original coral raw file
open (F, $fpath);

@lines = <F>;
%samebib = ();
%sameauthor = ();
%sametitle = ();
%samevenue = ();

foreach $line (@lines)
{
  $line=~s/\n//;
  $neg = 0;
  if($line=~/^!/) { $neg = 1;}
  if($line=~/SameBib/)
  {
     $line=~s/.*\(//;
     $line=~s/\)//;
     @tuple=split(/,/,$line);
     @tuple[0] = lc(@tuple[0]);
     @tuple[1] = lc(@tuple[1]);
     if($neg == 0) {push(@{ $samebib{@tuple[0]} }, @tuple[1]);}
     else {push(@{ $samebib{@tuple[0]} }, "-" . @tuple[1]);}
  }    
  if($line=~/SameAuthor/)
  {
     $line=~s/.*\(//;
     $line=~s/\)//;
     @tuple=split(/,/,$line);
     @tuple[0] = lc(@tuple[0]);
     @tuple[1] = lc(@tuple[1]);
     if($neg == 0) {push(@{ $sameauthor{@tuple[0]} }, @tuple[1]);}
     else {push(@{ $sameauthor{@tuple[0]} }, "-" . @tuple[1]);}
  }     
  if($line=~/SameTitle/)
  {
     $line=~s/.*\(//;
     $line=~s/\)//;
     @tuple=split(/,/,$line);
     @tuple[0] = lc(@tuple[0]);
     @tuple[1] = lc(@tuple[1]);
     if($neg == 0) {push(@{ $sametitle{@tuple[0]} }, @tuple[1]);}
     else {push(@{ $sametitle{@tuple[0]} }, "-" . @tuple[1]);}
  }     
  if($line=~/SameVenue/)
  {
     $line=~s/.*\(//;
     $line=~s/\)//;
     @tuple=split(/,/,$line);
     @tuple[0] = lc(@tuple[0]);
     @tuple[1] = lc(@tuple[1]);
     if($neg == 0) {push(@{ $samevenue{@tuple[0]} }, @tuple[1]);}
     else {push(@{ $samevenue{@tuple[0]} }, "-" . @tuple[1]);}
  }      
}

foreach $string (keys %samebib) {
    print "samebib($string,BC2)\t"; 
    foreach $element (@{$samebib{$string}})
    { 
       if($element=~/^\-/)
       { 
          $element=~s/^\-//g;
          print "-samebib($string,$element)\t";
       }
       else {print "+samebib($string,$element)\t";}
    }
    print "\n";
} 

foreach $string (keys %sameauthor) {
    print "sameauthor($string,A2)\t"; 
    foreach $element (@{$sameauthor{$string}})
    { 
       if($element=~/^\-/)
       { 
          $element=~s/^\-//g;
          print "-sameauthor($string,$element)\t";
       }
       else {print "+sameauthor($string,$element)\t";}
    }
    print "\n";
} 

foreach $string (keys %sametitle) {
    print "sametitle($string,T2)\t"; 
    foreach $element (@{$sametitle{$string}})
    { 
       if($element=~/^\-/)
       { 
          $element=~s/^\-//g;
          print "-sametitle($string,$element)\t";
       }
       else {print "+sametitle($string,$element)\t";}
    }
    print "\n";
} 

foreach $string (keys %samevenue) {
    print "samevenue($string,V2)\t"; 
    foreach $element (@{$samevenue{$string}})
    { 
       if($element=~/^\-/)
       { 
          $element=~s/^\-//g;
          print "-samevenue($string,$element)\t";
       }
       else {print "+samevenue($string,$element)\t";}
    }
    print "\n";
} 

close F;

 
