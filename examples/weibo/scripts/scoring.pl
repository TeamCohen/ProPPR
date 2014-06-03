#!/usr/bin/perl
# This script is to compute the UAS from answers and data.
# yww@cs.cmu.edu
# William Wang

$tpath = $ARGV[0];  # answers
$dpath = $ARGV[1];  # data

open (T, $tpath) or die "cannot open the answer file";
open (D, $dpath) or die "cannot open the data file";

@ans = <T>;
@data = <D>;

$alen = @ans;

%keys = ();
$correct = 0;
$all = 0;

foreach $line (@data)
{
   $line=~s/\n//;
   @tokens = split(/\t/,$line);
   foreach $token (@tokens)
   {
      if($token!~/^+/) {next;}
      else
      {
        $target = $token;
        $target =~ s/\+edge\(//g;
        $target =~ s/\)$//g;
        @parts = split(/,/,$target);
        $keys{@parts[0]} = @parts[1];
      }
   }
}

$max = 0;
$prev_sent = "";
$sent = "";

%roots = ();
$lastpun = 0;

for($i=0;$i<@ans;$i++)
{
       if($line=~/^\#/) 
    {
	$lastpun = $i;
    }
}

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

       if(($i==$lastpun) || ($sent ne $prev_sent))
       {
         foreach my $key (keys %roots) 
         { 
            $all++;
            @tokens = split(/,/,$key);
            $rt = $prev_sent."t0root";
            #print "$key true:$keys{@tokens[0]} top2:@tokens[1]\n";

            if(($max==$roots{$key}) && ($keys{@tokens[0]} eq $rt))
            {
              $correct++;
            }
            elsif(($max!=$roots{$key}) && ($keys{@tokens[0]} eq @tokens[1]))
            {
              $correct++;
            }
         }
         %roots = ();
         $max = 0;
       }
      
    }
    
    if(($i<$alen-2) && (@ans[$i+1]=~/^1/))
    {
       $pred = @ans[$i+1];
       $pred =~s/\n//;
       @parts = split(/\s+/,$pred);
       $p = @parts[2];

       if(@ans[$i+2]=~/^2/)
       {
         $top2 = @ans[$i+2];
         $top2 =~s/\n//;
         @items = split(/\s+/,$top2);
         $n = @items[2];
         $n=~s/.*\[//;
         $n=~s/\]//;
       }

       $s = @parts[1];
       
       $p=~s/.*\[//;
       $p=~s/\]//;
       if($p!~/root/)
       {
          $all++;
          if($keys{$q} eq $p) {$correct++;}
       }
       else
       {
          if($s > $max) {$max=$s;}
          $k = $q . "," . $n;
	   $roots{$k} = $s;
       }
       $prev_sent = $sent;
    }
    
}

$acc = $correct/$all;

print "UAS: $acc\n";


