/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.external.suite;

import java.util.EnumMap;

public final class TestFileUtil {

    private TestFileUtil() {
        assert false;
    }

    /**
     * Merges properties of the {@code first} and the {@code second} test files.
     * <p>
     * In other words, creates new test file with copied properties of the {@code first} test file
     * overwritten by non-{@code null} properties of the {@code second} one.
     * 
     * @param first test file
     * @param second test file
     * @return new test file with merged properties
     */
    public static TestFile merge(TestFile first, TestFile second) {
        assert first != null;
        assert second != null;
        assert first.getFilePath().equals(second.getFilePath()) : first.getFilePath() + " != " + second.getFilePath();
        // create new
        TestFile mergedTestFile = new TestFile(first.getFilePath());
        // merge
        TestFile.EcmaVersion ecmaVersion = second.getEcmaVersion();
        mergedTestFile.setEcmaVersion(ecmaVersion != null ? ecmaVersion : first.getEcmaVersion());
        TestFile.Status status = second.getStatus();
        mergedTestFile.setStatus(status != null ? status : first.getStatus());
        EnumMap<TestFile.Endianness, TestFile.Status> statusOverrides = second.getStatusOverrides();
        mergedTestFile.setStatusOverrides(statusOverrides != null ? statusOverrides : first.getStatusOverrides());
        Boolean runInIsolation = second.getRunInIsolation();
        mergedTestFile.setRunInIsolation(runInIsolation != null ? runInIsolation : first.getRunInIsolation());
        String blockedBy = second.getBlockedBy();
        mergedTestFile.setBlockedBy(blockedBy != null ? blockedBy : first.getBlockedBy());
        String comment = second.getComment();
        mergedTestFile.setComment(comment != null ? comment : first.getComment());
        return mergedTestFile;
    }

}
