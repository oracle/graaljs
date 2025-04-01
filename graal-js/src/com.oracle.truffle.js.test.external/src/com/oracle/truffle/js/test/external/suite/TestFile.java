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

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oracle.truffle.js.runtime.JSConfig;

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

    public static final Comparator<TestFile> COMPARATOR = new Comparator<>() {
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
    private JavaVersion javaVersion;
    private Status status;
    private EnumMap<StatusOverrideCondition, Status> statusOverrides;
    private Boolean runInIsolation;
    private String blockedBy;
    private String comment;
    @JsonIgnore private volatile Result result;

    @JsonCreator
    public TestFile(@JsonProperty("filePath") String filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
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

    public JavaVersion getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(JavaVersion javaVersion) {
        this.javaVersion = javaVersion;
    }

    /**
     * Method {@link #getRealStatus(SuiteConfig)} should be preferred.
     */
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public EnumMap<StatusOverrideCondition, Status> getStatusOverrides() {
        return statusOverrides;
    }

    public void setStatusOverrides(EnumMap<StatusOverrideCondition, Status> statusOverrides) {
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
     * Returns the first matching entry of {@code overrideStatus}, if any, otherwise returns
     * {@link #getStatus()}.
     *
     * @return real status
     */
    @JsonIgnore
    public Status getRealStatus(SuiteConfig config) {
        if (javaVersion != null && !javaVersion.isSupported(Runtime.version())) {
            return Status.SKIP;
        }
        if (statusOverrides != null) {
            for (Map.Entry<StatusOverrideCondition, Status> entry : statusOverrides.entrySet()) {
                if (entry.getKey().getCondition().test(config)) {
                    return entry.getValue();
                }
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
                        ", javaVersion=" + javaVersion +
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
        public static final int MAX_VERSION = JSConfig.StagingECMAScriptVersion;

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

        public EcmaVersion filterByMinVersion(int minimalVersion) {
            int count = 0;
            for (int version : allVersions) {
                if (minimalVersion <= version) {
                    count++;
                }
            }
            if (count == allVersions.length) {
                return this;
            } else {
                int[] filtered = new int[count];
                int idx = 0;
                for (int version : allVersions) {
                    if (minimalVersion <= version) {
                        filtered[idx++] = version;
                    }
                }
                return EcmaVersion.forVersions(filtered);
            }
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
     * Represents a range of supported Java versions.
     */
    public record JavaVersion(
                    Integer minVersion,
                    Integer maxVersion) {

        public boolean isSupported(Runtime.Version version) {
            int feature = version.feature();
            return (minVersion == null || (minVersion <= feature)) && (maxVersion == null || (feature <= maxVersion));
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
     * Represents a condition for overriding {@link TestFile#getStatus()}.
     */
    public enum StatusOverrideCondition {

        BIG_ENDIAN(cfg -> ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder())),
        LITTLE_ENDIAN(cfg -> ByteOrder.LITTLE_ENDIAN.equals(ByteOrder.nativeOrder())),
        SVM(cfg -> cfg.isExtLauncher()),
        COMPILE_IMMEDIATELY(cfg -> cfg.isCompile()),
        INSTRUMENT(cfg -> cfg.isInstrument()),
        POLYGLOT(cfg -> cfg.isPolyglot()),
        AMD64(cfg -> System.getProperty("os.arch").equals("amd64") || System.getProperty("os.arch").equals("x86_64")),
        AARCH64(cfg -> System.getProperty("os.arch").equals("aarch64")),
        WINDOWS(cfg -> System.getProperty("os.name").startsWith("Windows")),
        MACOS(cfg -> System.getProperty("os.name").startsWith("Mac")),
        STAGING(cfg -> cfg.getMinESVersion() == JSConfig.StagingECMAScriptVersion),
        LAZY_TRANSLATION(cfg -> "true".equals(System.getProperty("polyglot.js.lazy-translation"))),
        NO_IC(cfg -> "0".equals(System.getProperty("polyglot.js.function-cache-limit"))),
        SHARED_ENGINE(cfg -> cfg.isShareEngine());

        private final Predicate<SuiteConfig> condition;

        StatusOverrideCondition(Predicate<SuiteConfig> condition) {
            this.condition = condition;
        }

        public Predicate<SuiteConfig> getCondition() {
            return condition;
        }
    }

    /**
     * Represents result of the test run.
     * <p>
     * This class is thread-safe.
     */
    public static final class Result {

        public static final Result PASSED = new Result(null, false);
        public static final Result IGNORED = new Result(null, false);

        private final String details;
        private final boolean timeout;

        private Result(String details, boolean timeout) {
            this.details = details;
            this.timeout = timeout;
        }

        public boolean isFailure() {
            return details != null || timeout;
        }

        public String getDetails() {
            return details;
        }

        public boolean isTimeout() {
            return timeout;
        }

        @Override
        public String toString() {
            return "Result{" +
                            "details='" + details + '\'' +
                            ", timeout=" + timeout +
                            '}';
        }

        // ~ Factories

        public static Result failed(Throwable throwable) {
            assert throwable != null;
            return failed(detailsFromThrowable(throwable));
        }

        public static Result failed(String details) {
            assert details != null;
            return new Result(details, false);
        }

        public static Result timeout(Throwable throwable) {
            assert throwable != null;
            return timeout(detailsFromThrowable(throwable));
        }

        public static Result timeout(String details) {
            assert details != null;
            return new Result(details, true);
        }

        private static String detailsFromThrowable(Throwable throwable) {
            String details = throwable.getClass().getName();
            String message = throwable.getMessage();
            if (message != null) {
                details += ": " + message;
            }
            return details;
        }
    }
}
