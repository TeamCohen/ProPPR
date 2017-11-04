import sys
import re

if __name__ == "__main__":
    for line in sys.stdin:
        if line.startswith("#"):
            pass
        else:
            parts = line.strip().split()
            weight = parts[1]
            if float(weight)>0:
                goal = parts[2]
                m = re.match('(\w+)\((\w+),(\w+)\)',goal)
                assert m,'bad goal '+goal
                print "\t".join([m.group(1),m.group(2),m.group(3),weight])

