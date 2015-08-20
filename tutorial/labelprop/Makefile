tiny:
	proppr compile multirankwalk.ppr
	proppr set --programFiles multirankwalk.wam:tinyseeds.graph:tinyedges.graph
	proppr answer tinytest.examples
	proppr eval tinytest.examples tinytest.solutions.txt

toy:
	proppr compile multirankwalk.ppr
	proppr set --programFiles multirankwalk.wam:toyseeds.graph:toyedges.graph
	proppr answer toytest.examples
	proppr eval toytest.examples toytest.solutions.txtw

r8:
	proppr compile multirankwalk.ppr
	proppr set --programFiles multirankwalk.wam:r8seeds.graph:r8edges.graph
	proppr answer r8test.examples --apr eps=5e-7 --threads 10 --unnormalized
	proppr eval r8test.examples r8test.solutions.txt --metric recall --metric acc1 --metric map

20ng:
	proppr compile multirankwalk.ppr
	proppr set --programFiles multirankwalk.wam:20ngseeds.graph:20ngedges.graph
	proppr answer 20ngtest.examples --apr eps=1e-6 --threads 22
	proppr eval 20ngtest.examples 20ngtest.solutions.txt

clean:
	rm -f proppr.settings *.wam *.solutions.txt tmp.txt


