/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

/**
 * All the information related to test file.
 * <p>
 * This class is <b>NOT</b> thread-safe. All the properties are assumed to be read-only from
 * multiple threads. Only one property can be safely manipulated from multiple threads -
 * {@link #getResult() test result}.
 *
 * @see TestFileUtil#merge(TestFile, TestFile)
 */
public final class TestFile {

    public static final Comparator<TestFile> COMPARATOR = new Comparator<TestFile>() {
        @Override
        public int compare(TestFile o1, TestFile o2) {
            int result = o1.getFilePath().compareToIgnoreCase(o2.getFilePath());
            if (result == 0) {
                result = o1.getFilePath().compareTo(o2.getFilePath());
            }
            return result;
        }
    };

    private final String filePath;
    private EcmaVersion ecmaVersion;
    private Status status;
    private EnumMap<Endianness, Status> statusOverrides;
    private Boolean runInIsolation;
    private String blockedBy;
    private String comment;
    @JsonIgnore private volatile Result result;

    @JsonCreator
    public TestFile(@JsonProperty("filePath") String filePath) {
        assert filePath != null;
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public EcmaVersion getEcmaVersion() {
        return ecmaVersion;
    }

    public void setEcmaVersion(EcmaVersion ecmaVersion) {
        this.ecmaVersion = ecmaVersion;
    }

    /**
     * Method {@link #getRealStatus()} should be preferred.
     */
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public EnumMap<Endianness, Status> getStatusOverrides() {
        return statusOverrides;
    }

    public void setStatusOverrides(EnumMap<Endianness, Status> statusOverrides) {
        this.statusOverrides = statusOverrides;
    }

    public Boolean getRunInIsolation() {
        return runInIsolation;
    }

    public void setRunInIsolation(Boolean runInIsolation) {
        this.runInIsolation = runInIsolation;
    }

    public String getBlockedBy() {
        return blockedBy;
    }

    public void setBlockedBy(String blockedBy) {
        this.blockedBy = blockedBy;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    @JsonIgnore
    public boolean hasRun() {
        return result != null;
    }

    @JsonIgnore
    public boolean hasPassed() {
        assert hasRun() : "Use hasRun() first";
        return result == Result.PASSED;
    }

    /**
     * Returns {@code true} if the test is to be run but was ignored from whatever reason (e.g. not
     * a real test file).
     *
     * @return {@code true} if the test is to be run but was ignored from whatever reason (e.g. not
     *         a real test file)
     */
    @JsonIgnore
    public boolean isIgnored() {
        assert hasRun() : "Use hasRun() first";
        return result == Result.IGNORED;
    }

    /**
     * Returns real status.
     * <p>
     * It means {@link #getStatusOverrides() overriden status} for the {@link Endianness#current()
     * current endianness} if defined, {@link #getStatus() status} otherwise.
     *
     * @return real status
     */
    @JsonIgnore
    public Status getRealStatus() {
        if (statusOverrides != null) {
            Status overridenStatus = statusOverrides.get(Endianness.current());
            if (overridenStatus != null) {
                return overridenStatus;
            }
        }
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestFile testFile = (TestFile) o;
        return filePath.equals(testFile.filePath);
    }

    @Override
    public int hashCode() {
        return filePath.hashCode();
    }

    @Override
    public String toString() {
        return "TestFile{" +
                        "filePath=" + filePath +
                        ", ecmaVersion=" + ecmaVersion +
                        ", status=" + status +
                        ", statusOverrides=" + statusOverrides +
                        ", runInIsolation=" + runInIsolation +
                        ", blockedBy='" + blockedBy + '\'' +
                        ", comment='" + comment + '\'' +
                        ", result=" + result +
                        '}';
    }

    // ~ Inner classes

    /**
     * Represents specific ECMA version(s).
     * <p>
     * Either as a list of specific versions or a numeric range.
     * <p>
     * This class is thread-safe.
     */
    public static final class EcmaVersion {

        public static final int MIN_VERSION = 5;
        public static final int MAX_VERSION = JSTruffleOptions.MaxECMAScriptVersion;

        private final int[] allVersions;
        private final int[] versions;
        private final Integer minVersion;
        private final Integer maxVersion;

        @JsonCreator
        public EcmaVersion(@JsonProperty("versions") int[] versions, @JsonProperty("minVersion") Integer minVersion, @JsonProperty("maxVersion") Integer maxVersion) {
            if (versions != null) {
                Arrays.sort(versions);
            }
            this.versions = versions;
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
            allVersions = createAllVersions(versions, minVersion, maxVersion);
        }

        @SuppressWarnings("all")
        private static int[] createAllVersions(int[] versions, Integer minVersion, Integer maxVersion) {
            if (versions != null) {
                assert versions.length > 0 : "'versions' cannot be empty";
                assert minVersion == null : "'versions' set, 'minVersion' should be null";
                assert maxVersion == null : "'versions' set, 'maxVersion' should be null";
                return versions;
            }
            if (minVersion == null) {
                minVersion = MIN_VERSION;
            }
            if (maxVersion == null) {
                maxVersion = MAX_VERSION;
            }
            assert minVersion <= maxVersion : "'minVersion' (" + minVersion + ") must be less or equal 'maxVersion' (" + maxVersion + ")";
            int[] result = new int[maxVersion - minVersion + 1];
            for (int i = minVersion; i <= maxVersion; i++) {
                result[i - minVersion] = i;
            }
            return result;
        }

        @JsonIgnore
        public int[] getAllVersions() {
            return allVersions;
        }

        public int[] getVersions() {
            return versions;
        }

        public Integer getMinVersion() {
            return minVersion;
        }

        public Integer getMaxVersion() {
            return maxVersion;
        }

        @Override
        public String toString() {
            return "EcmaVersion{" +
                            "allVersions=" + Arrays.toString(allVersions) +
                            ", versions=" + Arrays.toString(versions) +
                            ", minVersion=" + minVersion +
                            ", maxVersion=" + maxVersion +
                            '}';
        }

        // ~ Factories

        public static EcmaVersion forVersions(int... versions) {
            return new EcmaVersion(versions, null, null);
        }

        public static EcmaVersion forRange(int minVersion, int maxVersion) {
            return new EcmaVersion(null, minVersion, maxVersion);
        }

        public static EcmaVersion forMinVersion(int minVersion) {
            return new EcmaVersion(null, minVersion, null);
        }

        public static EcmaVersion forMaxVersion(int maxVersion) {
            return new EcmaVersion(null, null, maxVersion);
        }

    }

    /**
     * Represents test status.
     */
    public enum Status {
        /**
         * Test is correct and passing on Graal.js.
         */
        PASS,
        /**
         * Skipped test (e.g. internal test of the 3rd-party JS engine, test violating ES etc.).
         */
        SKIP,
        /**
         * Test is correct but (only currently?) failing on Graal.js.
         */
        FAIL;
    }

    /**
     * Represents endianness.
     */
    public enum Endianness {
        BIG_ENDIAN,
        LITTLE_ENDIAN;

        private static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();

        public static Endianness current() {
            if (NATIVE_BYTE_ORDER.equals(ByteOrder.BIG_ENDIAN)) {
                return BIG_ENDIAN;
            }
            assert NATIVE_BYTE_ORDER.equals(ByteOrder.LITTLE_ENDIAN) : NATIVE_BYTE_ORDER;
            return LITTLE_ENDIAN;
        }

    }

    /**
     * Represents result of the test run.
     * <p>
     * This class is thread-safe.
     */
    public static final class Result {

        public static final Result PASSED = new Result(null, null, false);
        public static final Result IGNORED = new Result(null, null, false);

        private final String details;
        private final Throwable throwable;
        private final boolean timeout;

        private Result(String details, Throwable throwable, boolean timeout) {
            this.details = details;
            this.throwable = throwable;
            this.timeout = timeout;
        }

        public boolean isFailure() {
            return details != null || throwable != null || timeout;
        }

        public String getDetails() {
            return details;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public boolean isTimeout() {
            return timeout;
        }

        @Override
        public String toString() {
            return "Result{" +
                            "details='" + details + '\'' +
                            ", throwable=" + throwable +
                            ", timeout=" + timeout +
                            '}';
        }

        // ~ Factories

        public static Result failed(String details, Throwable throwable) {
            assert details != null;
            assert throwable != null;
            return new Result(details, throwable, false);
        }

        public static Result failed(Throwable throwable) {
            assert throwable != null;
            return new Result(null, throwable, false);
        }

        public static Result failed(String details) {
            assert details != null;
            return new Result(details, null, false);
        }

        public static Result timeout(String details, Throwable throwable) {
            assert details != null;
            assert throwable != null;
            return new Result(details, throwable, true);
        }

        public static Result timeout(Throwable throwable) {
            assert throwable != null;
            return new Result(null, throwable, true);
        }

        public static Result timeout(String details) {
            assert details != null;
            return new Result(details, null, true);
        }

    }

}
