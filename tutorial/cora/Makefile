cora: cora.wam cora.graph train.examples test.examples
	proppr compile cora.wam
	proppr set --programFiles cora.wam:cora.graph
	proppr set --epochs 30 --srw l2plocal:mu=0.002
	proppr ground train
	proppr train train
	proppr answer train pre.train.solutions.txt
	proppr answer train post.train.solutions.txt --params train.params
	proppr answer test pre.test.solutions.txt
	proppr answer test post.test.solutions.txt --params train.params
	sort -k 2rg train.params
	proppr eval train.examples pre.train.solutions.txt
	proppr eval train.examples post.train.solutions.txt
	proppr eval test.examples pre.test.solutions.txt
	proppr eval test.examples post.test.solutions.txt

clean:default.clean
	rm -f *.wam *.solutions.txt *.grounded* *.params *.examples *.graph proppr.settings

### DATASET BUILDING

cora.graph: train.raw test.raw
	scripts/getGraph.sh $^ | sort | uniq > $@

train.examples: train.raw
	scripts/getData.pl $^ > $@

test.examples: test.raw
	scripts/getData.pl $^ > $@