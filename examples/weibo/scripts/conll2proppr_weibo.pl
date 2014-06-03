#!/usr/bin/perl
# This script is to generate the conll format from part-of-speech tagged text files.
# yww@cs.cmu.edu
# William Wang

$tpath = $ARGV[0];  # text
$dpath = $ARGV[1];  # dict
$apath = $ARGV[2];  # allowed words (threshold cutoff)

$ostem = $tpath;
$ostem =~s/.txt//;

$odata = $ostem . ".data";
$ofacts = $ostem . ".facts";
$odict = $ostem . ".dict";

open (T, $tpath) or die "cannot open the input file";
open (A, $apath);
open (D, $dpath);

open (OD, ">$odata");
open (OF, ">$ofacts");
open (OT, ">$odict");

@lines= <T>;
@dlines = <D>;
@alines = <A>;
@sent = ();

%dictionary = ();
if($dpath ne "")
{
   foreach $dline (@dlines)
   {
      $dline=~s/\n//;
      @tokens = split(/\t/,$dline);
      $dictionary{@tokens[1]} = @tokens[0];
   }
   $count = keys( %dictionary );
}
else
{
  $count = 0;
}

%allow = ();

foreach $aline (@alines)
{
   $aline=~s/\n//;
   @tokens = split(/\t/,$aline);
   $allow{@tokens[0]} = 1;
}


$sentid = 1;
foreach $line (@lines)
{
   $line =~s/\n//;
   if($line!~/^ *$/)
   {
      push(@sent,$line);
   }
   else
   {
     for($i=0;$i<@sent;$i++)
     {
        @tokens = split(/\s+/,@sent[$i]);
        $word = @tokens[1];
        $windex = 0;

        if (exists $dictionary{$word}) 
        {
          $windex = $dictionary{$word};
        }
        else 
        {
          $count++;
          $dictionary{$word} = $count;
          $windex = $count;
        }

        $arg1 = "s" . $sentid . "t" . @tokens[0] . "word" . $windex;
        $arg2index = @tokens[6] - 1;
        $root = "s" . $sentid . "t" . 0 . "root";

        print OD "edge($arg1,V2)";
        if($arg2index == -1) {print OD "\t+edge($arg1,$root)";}

        $pos = lc(@tokens[4]);

        if($pos =~/^v/) 
        {
           #print OF "adjacent\t$arg1\t$root\n";
           #print OF "adjacent\t$root\t$arg1\n";
           print OF "distrt\t$arg1\t$root\tzero\n";

           print OF "hasprop\t$root\t" . "word_root" . "\n";
	    print OF "hasprop\t$root\t" . "pos_ru" . "\n";

           #print OF "hasword\t$root\troot\n";
           #print OF "haswordinverse\troot\t$root\n";

           #print OF "haspos\t$root\tru\n";
           #print OF "hasposinverse\tru\t$root\n";
        }

        if (exists $allow{$word}) 
        {
           print OF "hasprop\t$arg1\t" . "word_word$windex" . "\n";
        }
        
        #print OF "hasword\t$arg1\tword$windex\n";
        #print OF "haswordinverse\tword$windex\t$arg1\n";

        print OF "hasprop\t$arg1\t" . "pos_$pos\n";

        #print OF "haspos\t$arg1\t$pos\n";
        #print OF "hasposinverse\t$pos\t$arg1\n";

        for($j=0;$j<@sent;$j++)
        {
           if($i==$j) {next;}

           @parts = split(/\s+/,@sent[$j]);
           $term = @parts[1];
           $ppos = lc(@parts[4]);

           $pindex = 0;

           if (exists $dictionary{$term}) 
           {
              $pindex = $dictionary{$term};
           }
           else 
           {
              $count++;
              $dictionary{$term} = $count;
              $pindex = $count;
           }           
           $arg2 = "s" . $sentid . "t" . @parts[0] . "word". $pindex;
           if($j==$i-1 || $j==$i+1) 
           {
              #print OF "adjacent\t$arg1\t$arg2\n";
              #$pbigram = "word". $pindex . "_" . "word" . $windex;
              #$pbipos = $pos . "_" . $ppos;
              #print OF "hasword\t$arg1\t$pbigram\n";
              #print OF "haswordinverse\t$pbigram\t$arg1\n";
              #print OF "haspos\t$arg1\t$pbipos\n";
              #print OF "hasposinverse\t$pbipos\t$arg1\n";
	       
              if($j==$i-1) {
                print OF "distl\t$arg1\t$arg2\tzero\n";
	         print OF "previous\t$arg1\t$arg2\n";
              }
              else {
                print OF "distr\t$arg1\t$arg2\tzero\n";
                print OF "next\t$arg1\t$arg2\n";
              }
	    }
           
           $fidx = 0;
           $sidx = 0;
           if($j==$i+3)
           {
              $fidx = $i+1;
              $sidx = $i+2;
           }
	    elsif($j==$i-3)
           {
              $fidx = $i-1;
              $sidx = $i-2;
           }  

           $cindex = 0;

           if($j==$i-2){
             $cindex = $i-1;
           }
           elsif($j==$i+2){
             $cindex = $i+1;
           }

           if($j==$i-2 || $j==$i+2) {
             #print OF "skipone\t$arg1\t$arg2\n";
	      @parts = split(/\s+/,@sent[$cindex]);
             $term = @parts[1];
             $id = &getIndex($term);
             $ppos = lc(@parts[4]);

              if($j==$i-2) {
                print OF "distl\t$arg1\t$arg2\tone\n";
              }
              else {
                print OF "distr\t$arg1\t$arg2\tone\n";
              }
             
             #print OF "dist\t$arg1\t$arg2\tone\n";
             #print OF "contextw\t$arg1\t$arg2\tword$id\n";
             #print OF "contextp\t$arg1\t$arg2\t$ppos\n";
           }
           if($j==$i-3 || $j==$i+3) {
             @parts = split(/\s+/,@sent[$fidx]);
             $term = @parts[1];
             $fid = &getIndex($term);
             $fpos = lc(@parts[4]); 
             @parts = split(/\s+/,@sent[$sidx]);
             $term = @parts[1];
             $sid = &getIndex($term);
             $spos = lc(@parts[4]); 

              if($j==$i-3) {
                print OF "distl\t$arg1\t$arg2\ttwo\n";
              }
              else {
                print OF "distr\t$arg1\t$arg2\ttwo\n";
              }

             #print OF "dist\t$arg1\t$arg2\ttwo\n";
             #print OF "contextwsk2\t$arg1\t$arg2\tword$fid\tword$sid\n";
             #print OF "contextpsk2\t$arg1\t$arg2\t$fpos\t$spos\n";   
             #print OF "skiptwo\t$arg1\t$arg2\n";
           }
           if($j==$i-4 || $j==$i+4) {
             #print OF "dist\t$arg1\t$arg2\tthree\n";
             #print OF "skipthree\t$arg1\t$arg2\n";

              if($j==$i-4) {
                print OF "distl\t$arg1\t$arg2\tthree\n";
              }
              else {
                print OF "distr\t$arg1\t$arg2\tthree\n";
              }

           }

           print OF "samesent\t$arg1\t$arg2\n";

           if($j<$i-5 || $j>$i+5) {
           print OF "farfrom\t$arg1\t$arg2\n";
           }

           if($j==$arg2index) {print OD "\t+edge($arg1,$arg2)";}
           elsif($j!=$arg2index && $j<$arg2index+5 && $j>$arg2index-5)
	    #elsif($j!=$arg2index && ($j>$arg2index+2 || $j<$arg2index-2))
           #else
           {
              print OD "\t-edge($arg1,$arg2)";
           }
	 }
        print OD "\n";
     }
     $sentid++;
     @sent = ();
   }

}

foreach my $word (sort {$dictionary{$a} <=> $dictionary{$b}} keys %dictionary) {
    print OT "$dictionary{$word}\t$word\n";
}

sub getIndex
{
    if (exists $dictionary{$_[0]}) 
    {
       $pindex = $dictionary{$_[0]};
    }
    else 
    {
       $count++;
       $dictionary{$_[0]} = $count;
       $pindex = $count;
    }    
  $pindex;
}

close T;