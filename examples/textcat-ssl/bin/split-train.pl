$labeledFile = shift;          
$unlabeledFile = shift;          
$NUM_SEEDS = shift || 20;
if (!$unlabeledFile) {
    die "usage: perl split-train.pl labeled-filename unlabeled-filename [num-seeds] < alltrain.txt";
}

%docLabel = ();
%docKey = ();
while (<>) {
    chop;
    $line = $_;
    my($docid,$lab) = split(/\t/);
    $docLabel{$docid} = $lab;
    $docKey{$docId} = rand();
}
%numPerClass = ();
open(G,">$labeledFile") || die;
open(H,">$unlabeledFile") || die;
foreach $docId (sort {$docKey{$b}<=>$docKey{$a}} (keys(%docLabel))) {
    if (++$numPerClass{$docLabel{$docId}} <= $NUM_SEEDS) {
	print G $docId,"\t",$docLabel{$docId},"\n";
    } else {
	print H "$docId\n";
    }
}
