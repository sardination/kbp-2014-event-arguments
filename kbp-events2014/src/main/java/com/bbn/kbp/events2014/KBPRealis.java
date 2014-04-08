package com.bbn.kbp.events2014;

import com.google.common.base.Optional;

/**
 * Represents a valid Realis value for the KBP 2014 Event Argument Task.
 */
public enum KBPRealis {
	Actual, Generic, Other;

	public static final String NIL = "NIL";

    /**
     * Parses a string from {@code asString} as a realis. {@code NIL} becomes
     * {@code Optional.absent()}. Invalid values will raise an exception.
     * @param s
     * @return
     */
	public static Optional<KBPRealis> parseOptional(final String s) {
		if (s.equals(NIL)) {
			return Optional.absent();
		} else {
			return Optional.of(KBPRealis.valueOf(s));
		}
	}

    /**
     * Returns a string representation of a realis suitable for inclusion in
     * a KBP EA task output file. {@code Optional.absent()} becomes {@code NIL}.
     * @param realis
     * @return
     */
	public static String asString(final Optional<KBPRealis> realis) {
		if (realis.isPresent()) {
			return realis.get().toString();
		} else {
			return NIL;
		}
	}
}