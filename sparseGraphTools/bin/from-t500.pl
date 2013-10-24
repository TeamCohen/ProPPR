$MINFREQ = 20;

print STDERR "contexts....\n";
open(F,"/usr2/bigdata/rtw/t500-matrix/contexts.txt");
$k = 0;
while ($line = <F>) {
    chop($line);
    $k++;
    $line =~ s/_/ARG/;
    $line =~ s/[\'\- \.]/_/g;
    print "cName","\t","c$k","\t","c$line","\n";
}
close(F);

print STDERR "nps....\n";
open(F,"/usr2/bigdata/rtw/t500-matrix/nps.txt");
$k = 0;
while ($line = <F>) {
    chop($line);
    $k++;
    $line =~ s/[\'\- \.]/_/g;
    print "nName","\t","n$k","\t","n$line","\n";
}
close(F);

print STDERR "matrix 1....\n";
open(F,"/usr2/bigdata/rtw/t500-matrix/matrix.txt");
while ($line = <F>) {
    chop($line);
    my($np,$context,$freq) = split(/\t/,$line);
    print "n2f\t","n$np","\t","c$context","\n" if $freq>=$MINFREQ; 
}
close(F);

print STDERR "matrix 2....\n";
open(F,"/usr2/bigdata/rtw/t500-matrix/matrix.txt");
while ($line = <F>) {
    chop($line);
    my($np,$context,$freq) = split(/\t/,$line);
    print "f2n\t","c$context","\t","n$np","\n" if $freq>=$MINFREQ; 
}
close(F);
