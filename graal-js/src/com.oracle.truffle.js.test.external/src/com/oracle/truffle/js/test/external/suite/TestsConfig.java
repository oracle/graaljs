/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.external.suite;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class represents configuration of individual tests.
 */
public final class TestsConfig {

    private Set<TestFile> testFiles = new TreeSet<>(TestFile.COMPARATOR);

    public Collection<TestFile> getTestFiles() {
        return Collections.unmodifiableCollection(testFiles);
    }

    public void setTestFiles(Collection<TestFile> testFiles) {
        this.testFiles.clear();
        this.testFiles.addAll(testFiles);
    }

    public void addTests(Collection<TestFile> tests) {
        // remove "old" tests
        testFiles.removeAll(tests);
        // add new ones
        testFiles.addAll(tests);
    }

    public void removeTests(Collection<TestFile> tests) {
        testFiles.removeAll(tests);
    }

}
