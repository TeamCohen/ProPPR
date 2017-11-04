@labels = ();
while ($lab = <>) {
    chop($lab);
    push(@labels,$lab);
}
@labels = sort(@labels);
foreach $i (0..$#labels-1) {
    foreach $j ($i+1..$#labels) {
	print join("\t",'mutex',$labels[$i],$labels[$j]),"\n";
    }
}
