/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.regex.tregex.util.DebugUtil;

public final class RegexSource {

    private final String pattern;
    private final RegexFlags flags;
    private final RegexOptions options;
    private boolean hashComputed = false;
    private int cachedHash;

    public RegexSource(String pattern, RegexFlags flags, RegexOptions options) {
        this.pattern = pattern;
        this.flags = flags;
        this.options = options;
    }

    public String getPattern() {
        return pattern;
    }

    public RegexFlags getFlags() {
        return flags;
    }

    public RegexOptions getOptions() {
        return options;
    }

    @Override
    public int hashCode() {
        if (!hashComputed) {
            final int prime = 31;
            cachedHash = 1;
            cachedHash = prime * cachedHash + pattern.hashCode();
            cachedHash = prime * cachedHash + flags.hashCode();
            cachedHash = prime * cachedHash + options.hashCode();
            hashComputed = true;
        }
        return cachedHash;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof RegexSource &&
                        pattern.equals(((RegexSource) obj).pattern) &&
                        flags.equals(((RegexSource) obj).flags) &&
                        options.equals(((RegexSource) obj).options);
    }

    @Override
    public String toString() {
        return "/" + pattern + "/" + flags;
    }

    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("RegexSource",
                        new DebugUtil.Value("pattern", pattern),
                        new DebugUtil.Value("flags", flags),
                        new DebugUtil.Value("options", options));
    }
}
