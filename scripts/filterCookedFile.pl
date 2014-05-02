#!/usr/bin/perl

my $cookedFile=shift;
my $thresh=shift;
my $newFile=shift;

defined($newFile) or die "Usage:\n\texamples.cooked frequencyThreshold examples.thresh.cooked\n";

# go through $cookedFile once to get all the feature frequencies
open(my $cf,"<$cookedFile") or die "Couldn't open $cookedFile:\n$!\n";
print "Analyzing $cookedFile...\n";
my %allFeatures;
my $nl=0;
while(<$cf>) {
  $nl++;
  my ($x, $x, $x, $x, $x, $x, $featureIndex, @edges) = split "\t";
  my @featureNames = split(":",$featureIndex);
  foreach my $edge (@edges) {
    my ($tmp,$flist) = split(":",$edge);
    foreach my $f (split(",",$flist)) {
      $allFeatures{$featureNames[$f]}++;
    }
  }
  print "Analyzed $nl lines...\n" if ($nl % 100 == 0);
}
close($cf);

my $eliminated = 0; my $total=0;
foreach my $key (keys %allFeatures) {
  $total++;
  $eliminated++ if ($allFeatures{$key}<$thresh);
}
print "\n";
print "Finished analyzing ($nl lines): Thresholding will eliminate $eliminated of $total features.\n";

#foreach my $key (keys %allFeatures) {
#  print "$allFeatures{$key}\t$key\n";
#}
#print "\n";


# go through $cookedFile a second time to write output
my $i_edgesize = 5;
open(my $cf,"<$cookedFile") or die "Couldn't open $cookedFile:\n$!\n";
open(my $nf,">$newFile") or die "Couldn't open $newFile:\n$!\n";
print "\nRewriting $newFile...\n";
print "(Really just copying)\n\n" if ($eliminated==0);
$nl=0;
while(<$cf>) {
  chomp;
  $nl++;

  if($eliminated==0) {
    print $nf "$_\n";
    next;
  }

  my ($queryT, $queryI, $posI, $negI, $v, $e, $featureIndex, @edges) = split "\t";
  my @line = ($queryT, $queryI, $posI, $negI, $v, $e);

  # create the new feature index for this example
  
  my @oldFeatureNames = split(":",$featureIndex);
  my @newFeatureNames;
  my %oldToNew;
  my $i=-1; my $j=-1;
  foreach my $f (@oldFeatureNames) {
    $i++;
    if ($allFeatures{$f}>$thresh) {
      $j++;
      push (@newFeatureNames,$f);
      $oldToNew{$i}=$j;
    } else {
      $oldToNew{$i}=-1;
    }
    #print "$i -> $oldToNew{$i} $f\n";
  }
  
  # insert new index into line
  
  push(@line,join(":",@newFeatureNames));
  #print "Old features: $featureIndex\n\n";
  #print "New features: ";
  #print join(":",@newFeatureNames);
  #print "\n\n";

  # convert edges and add to line
  
  my $nedges = 0;
  my $nedges_orig = 0;
  foreach my $edge (@edges) {
    #print "\nProcessing edge $edge\n";
    my ($tmp,$ftext) = split(":",$edge);
    next if ($ftext eq "");
    #print "\tOld features: $ftext\n";
    my @oldflist = split(",",$ftext);
    my @newflist;
    foreach my $f (@oldflist) {
      #print "\t$f: $oldToNew{$f}\n";
      push(@newflist,$oldToNew{$f}) unless ($oldToNew{$f} < 0);
    }
    $nedges_orig++;
    next if ($#newflist<0);
    
    my $newftext=join(",",@newflist);
    push(@line,"$tmp:$newftext");
    $nedges++;
    #print "New features: $newftext\n";
  }
  
  if ($line[$i_edgesize] >= $nedges) {
  	$line[$i_edgesize] = $nedges;
  } else {
  	print "Bad #edges calculation. Orig line header: $line[0] $line[1] $line[2] $line[3] $line[4] $line[5]\n";
  	print "Edge list size:$#edges\nOld #edges: $nedges_orig\nNew #edges: $nedges\n";
  }
  
  print $nf join("\t",@line);
  print $nf "\n";

  print "Rewrote $nl lines...\n" if ($nl % 100 == 0);
}
close $cf;
close $nf;
print "\nFinished rewriting ($nl lines).\n";
