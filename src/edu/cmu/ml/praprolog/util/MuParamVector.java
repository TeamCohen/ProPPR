package edu.cmu.ml.praprolog.util;

import java.util.Map;

/**
 * A version of the parameter vector which also tracks the last update time of each key
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class MuParamVector extends ParamVector<TimestampedWeight> {

	@Override
	protected Map<String, TimestampedWeight> getBackingStore() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected TimestampedWeight newValue(Double value) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected Double getWeight(TimestampedWeight value) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
