PROPPR=/home/krivard/git/rinkitink/ProPPR
CP:=.:${PROPPR}/bin:${PROPPR}/conf/:${PROPPR}/lib/*
SCRIPTS:=/home/krivard/ProPPR-examples/scripts

### JAVA SETTINGS
JOPTS=-Xmx6G

### PROPPR PARAMETERS
PROVER=dpr:1e-5
THREADS=1

### DATASETS
FAST:=textcattoy top-1000-near-google webkb
SLOW:=cora weibo
ALL:=$(FAST) $(SLOW)


### TARGETS

default: $(addsuffix .results,$(FAST))

fast.clean: $(addsuffix .clean,$(FAST))

clean: $(addsuffix .clean,$(ALL))

%.results:
	cd $*; CP=${CP} $(MAKE) $(MFLAGS)

%.clean:
	cd $*; CP=${CP} $(MAKE) $(MFLAGS) clean