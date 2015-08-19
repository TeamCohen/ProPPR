### DATASET
BASEDON=../webkb
SRW=
include ${BASEDON}/Makefile
JOPTS:=$(JOPTS) -Dconfig.properties=adagrad.properties
empty :=
space := $(empty) $(empty)
PROGRAM:=$(subst ${space},:,$(addprefix ${BASEDON}/,$(subst :, ,${PROGRAM})))
vpath %.data ${BASEDON}
