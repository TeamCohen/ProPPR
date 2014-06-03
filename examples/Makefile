PROPPR=/home/krivard/git/rinkitink/ProPPR
CP:=.:${PROPPR}/bin:${PROPPR}/conf/:${PROPPR}/lib/*
SCRIPTS:=/home/krivard/ProPPR-examples/scripts

### JAVA SETTINGS
JOPTS=-Xmx6G

### PROPPR PARAMETERS
PROVER=dpr:1e-5
THREADS=1


### TARGETS

default: textcattoy.results top-1000-near-google.results cora.results

clean: textcattoy.clean top-1000-near-google.clean cora.clean

%.results:
	cd $*; CP=${CP} $(MAKE) $(MFLAGS)

%.clean:
	cd $*; CP=${CP} $(MAKE) $(MFLAGS) clean