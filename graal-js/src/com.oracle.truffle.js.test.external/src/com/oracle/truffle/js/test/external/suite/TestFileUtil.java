/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
        EnumMap<TestFile.StatusOverrideCondition, TestFile.Status> statusOverrides = second.getStatusOverrides();
        mergedTestFile.setStatusOverrides(statusOverrides != null ? statusOverrides : first.getStatusOverrides());
        Boolean runInIsolation = second.getRunInIsolation();
        mergedTestFile.setRunInIsolation(runInIsolation != null ? runInIsolation : first.getRunInIsolation());
        String blockedBy = second.getBlockedBy();
        mergedTestFile.setBlockedBy(blockedBy != null ? blockedBy : first.getBlockedBy());
        String comment = second.getComment();
        mergedTestFile.setComment(comment != null ? comment : first.getComment());
        TestFile.JavaVersion javaVersion = second.getJavaVersion();
        mergedTestFile.setJavaVersion(javaVersion != null ? javaVersion : first.getJavaVersion());
        return mergedTestFile;
    }

    private static int getJavaSpecificationVersion() {
        String value = System.getProperty("java.specification.version");
        if (value.startsWith("1.")) {
            value = value.substring(2);
        }
        return Integer.parseInt(value);
    }

    /**
     * The integer value corresponding to the value of the {@code java.specification.version} system
     * property after any leading {@code "1."} has been stripped.
     */
    public static final int JAVA_SPEC = getJavaSpecificationVersion();

}
