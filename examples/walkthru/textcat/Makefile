tiny:
	proppr compile textcat.ppr
	proppr set --programFiles textcat.wam:tinylabels.cfacts:tinycorpus.graph
	proppr ground tinytrain
	proppr train tinytrain
	sort -k2 -gr tinytrain.params
	proppr answer tinytrain --params tinytrain.params 
	proppr eval tinytrain --metric acc2

toy:
	proppr compile textcat.ppr
	proppr set --programFiles textcat.wam:toylabels.cfacts:toycorpus.graph
	proppr ground toytrain
	proppr train toytrain --epochs 4 --srw l2plocal:eta=10
	proppr answer toytest --params toytrain.params 
	proppr eval toytest --metric acc2
	proppr answer toytrain --params toytrain.params 
	proppr eval toytrain --metric acc2

r8:
	proppr compile textcat.ppr
	proppr set --programFiles textcat.wam:r8labels.cfacts:r8corpus.graph
	proppr ground r8train --threads 10 --duplicateCheck -1
	proppr train r8train --threads 10 --srw l2plocal
	proppr answer r8test --params r8train.params --duplicateCheck -1
	proppr eval r8test --metric acc2

clean:
	rm -f proppr.settings *.wam *.solutions.txt *.examples.grounded *.params

