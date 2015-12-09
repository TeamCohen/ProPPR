#!/usr/bin/python
from os import write
from os import path

VERSION_2WAM="2.0-wam"
VERSION_2CANON="2.0-canon"
VERSION_1="1.0"

def parseGraphKeyLine(line):
	"""
	returns (query, state_id, state_version, (state_metadata), isQuery, isCompleted)
	"""
	line = line.replace("\n","")
	(query,sid,state) = line.split("\t")
	if state.startswith("state"): # 2.0 notation
		state = state.replace("state<","")
		state = state.replace(">","")
		(heap,reg,calls,rest) = state.split("] ")
		calls = calls.replace("c[","").replace("sf:","\\lsf:")
		return (query, sid, VERSION_2WAM, (heap,reg,rest,calls), sid==1, "*" in rest)
	elif state.startswith("canonState"): # 2.0+ notation
		state = state.replace("canonState<","")
		(canon,rest) = state.split(">")
		return (query, sid, VERSION_2CANON, (canon), sid==1, "*" in rest)
	elif state.startswith("lpState"): # 1.0 notation
		state = state.replace("lpState: ","")
		state = state.replace("c[","")
		state = state.replace("v[-","X")
		state = state.replace("]","")
		(head,tail) = state.split(" ... ")
		tail = tail.replace(" ","\\l")
		return (query, sid, VERSION_1, (head,tail), sid==1, tail is "")
	else:
		assert False, "Didn't recognize key file syntax :(\n"

def fetchMasterFeatures(groundFile):
	masterFeatures=[""]
	masterFeaturesFile = "%s.features"%groundFile
	if path.isfile(masterFeaturesFile):
		with open(masterFeaturesFile,'r') as featureFile:
			for f in featureFile:
				masterFeatures.append(f.strip())
	return masterFeatures
