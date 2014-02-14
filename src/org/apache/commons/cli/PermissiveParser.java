package org.apache.commons.cli;

import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

public class PermissiveParser extends BasicParser {
	private boolean ignoreUnrecognizedOption;
	Set<String> unrecognizedOptions;

    public PermissiveParser(final boolean ignoreUnrecognizedOption) {
        this.ignoreUnrecognizedOption = ignoreUnrecognizedOption;
        this.unrecognizedOptions = new TreeSet<String>();
    }

    @Override
    protected void processOption(final String arg, final ListIterator iter) throws ParseException {
        boolean hasOption = getOptions().hasOption(arg);
        if (hasOption || !ignoreUnrecognizedOption) {
            super.processOption(arg, iter);
        } else if (ignoreUnrecognizedOption) {
        	this.unrecognizedOptions.add(arg);
        }
    }
    
    public Set<String> getUnrecognizedOptions() { return this.unrecognizedOptions; }
    public boolean hasUnrecognizedOptions() { return !this.unrecognizedOptions.isEmpty(); }
}
