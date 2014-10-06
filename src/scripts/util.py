import sys

# miscellaneous utilities

def linesOf(fileName, interval=5000, msg="Read %d lines",chatty=True):
    """Enumerate lines in a file, while printing period status
    updates, and skipping blank lines and lines with a leading
    hashmark.  An interval=0 means not to output status updates."""
    if chatty: sys.stderr.write("Loading from file '%s'...\n" % fileName)
    f = open(fileName,'r')
    for k,line in enumerate(f):
        if chatty and interval and k and (k % interval)==0: sys.stderr.write((msg % k)+"\n")
        line = line.rstrip()
        if line and not line.startswith('#'):
            yield line
    f.close()

def encode(u):
    """Encode u as a string w/o tabs or newlines"""
    return repr(u)

def decode(u):
    """Invert the encode operation"""
    return eval(u)

def argmin(d):
    """Return the key of a dict with smallest value"""
    return min(swapItems(d))[1]

def argmax(d):
    """Return the key of dict with largest value."""
    return max(swapItems(d))[1]

def swapItems(d):
    """Enumerate pairs (val,key) where d[key]==val."""
    for (key,val) in d.items():
        yield (val,key)

def normalizeVector(vec,desiredSum=1.0):
    """Given a dictionary that represents a sparse numeric vector,
    rescale the values to sum to some desired amount."""
    z = sum(vec.values()) / desiredSum
    for u in vec: vec[u] /= z

def rescaleVector(vec,targetMin=0,targetMax=1.0):
    """Given a dictionary that represents a sparse numeric vector,
    rescale the values to fall in a range targetMin...targetMax"""
    lo = min(vec.values())
    hi = max(vec.values())
    scale = (targetMax - targetMin)/(hi - lo)
    for u in vec: vec[u] = (vec[u]-lo)*scale + targetMin

def loadVector(vecFile,interval=100000):
    vec = {}
    for line in linesOf(vecFile,interval=interval):
        (x,val) = line.split("\t")
        vec[x] = float(val)
    return vec

def saveVector(vec,vecFile):
    f = open(vecFile,'w')
    for x,val in vec.items():
        f.write('%s\t%f\n' % (x,val))
    f.close()

