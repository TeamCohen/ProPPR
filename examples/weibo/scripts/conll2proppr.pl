#!/usr/bin/perl
# This script is to generate the conll format from part-of-speech tagged text files.
# yww@cs.cmu.edu
# William Wang

$tpath = $ARGV[0];  # text
$dpath = $ARGV[1];  # dict

$ostem = $tpath;
$ostem =~s/.txt//;

$odata = $ostem . ".data";
$ofacts = $ostem . ".facts";
$odict = $ostem . ".dict";

open (T, $tpath) or die "cannot open the input file";
open (D, $dpath);

open (OD, ">$odata");
open (OF, ">$ofacts");
open (OT, ">$odict");

@lines= <T>;
@dlines = <D>;
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
        $arg2index = @tokens[8] - 1;
        $root = "s" . $sentid . "t" . 0 . "root";

        print OD "edge($arg1,V2)";
        if($arg2index == -1) {print OD "\t+edge($arg1,$root)";}

        $pos = lc(@tokens[4]);
        if($pos eq "vv") 
        {
           print OF "adjacent\t$arg1\t$root\n";
           print OF "adjacent\t$root\t$arg1\n";
           print OF "hasword\t$root\troot\n";
           print OF "haswordinverse\troot\t$root\n";
           print OF "haspos\t$root\tru\n";
           print OF "hasposinverse\tru\t$root\n";
        }

        print OF "hasword\t$arg1\tword$windex\n";
        print OF "haswordinverse\tword$windex\t$arg1\n";
        print OF "haspos\t$arg1\t$pos\n";
        print OF "hasposinverse\t$pos\t$arg1\n";

        for($j=0;$j<@sent;$j++)
        {
           if($i==$j) {next;}

           @parts = split(/\s+/,@sent[$j]);
           $word = @parts[1];
           $pindex = 0;

           if (exists $dictionary{$word}) 
           {
              $pindex = $dictionary{$word};
           }
           else 
           {
              $count++;
              $dictionary{$word} = $count;
              $pindex = $count;
           }           
           $arg2 = "s" . $sentid . "t" . @parts[0] . "word". $pindex;
           if($j==$i-1 || $j==$i+1) {print OF "adjacent\t$arg1\t$arg2\n";}
           if($j==$i-2 || $j==$i+2) {print OF "skipone\t$arg1\t$arg2\n";}
           if($j==$i-3 || $j==$i+3) {print OF "skiptwo\t$arg1\t$arg2\n";}
           if($j==$i-4 || $j==$i+4) {print OF "skipthree\t$arg1\t$arg2\n";}
           if($j<$i-4 || $j>$i+4) {print OF "farfrom\t$arg1\t$arg2\n";}

           if($j==$arg2index) {print OD "\t+edge($arg1,$arg2)";}
           #elsif($j!=$arg2index && $j<$arg2index+3 && $j>$arg2index-3)
           else
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

close T;