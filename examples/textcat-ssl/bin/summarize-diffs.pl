# process the log file from an expt.bash run and output every
# improvement in accuracy.  if most of these diffs are positive then
# SSL is working

while (<>) {
    if (/CONDITION.*using (\d+).*label is (.)/) {
	$n = $1;
	$lab = $2;
    }
    if (/difference:python/) {
	chop($nextLine = <>);
	print "$n\t$lab\t$nextLine\n";
    }
}
