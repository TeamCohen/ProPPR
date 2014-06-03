#!/usr/bin/perl




my %count_of;
while (my $line = <>) { #read from file or STDIN
	$line =~s/\n//g;
	$line =~s/[\t\s][\t\s]*/ /g;
  foreach my $word (split /[\s\t]+/, $line) {
     $count_of{$word}++;
  }
}

#print "All words and their counts: \n";

foreach my $key (sort {$count_of{$b} <=> $count_of{$a}} keys %count_of){
   print "$count_of{$key}\t$key\n";
}


#for my $word (sort keys %count_of) {
#  print "$count_of{$word}\t$word\n";
#}