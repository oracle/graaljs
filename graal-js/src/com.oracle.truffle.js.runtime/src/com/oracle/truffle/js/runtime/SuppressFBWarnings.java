/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

/**
 * Used to suppress <a href="http://findbugs.sourceforge.net">FindBugs</a> warnings.
 */
public @interface SuppressFBWarnings {
    /**
     * The set of FindBugs
     * <a href="http://findbugs.sourceforge.net/bugDescriptions.html">warnings</a> that are to be
     * suppressed in annotated element. The value can be a bug category, kind or pattern.
     */
    String[] value();

    /**
     * Reason why the warning is suppressed.
     */
    String justification();
}
