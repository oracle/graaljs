/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.js.parser;

/**
 * Parser environment consists of command line options, and output and error writers, etc.
 */
public final class ScriptEnvironment {

    public static final int ES_2015 = 6;
    public static final int ES_2016 = 7;
    public static final int ES_2017 = 8;
    public static final int ES_2018 = 9;
    public static final int ES_2019 = 10;
    public static final int ES_2020 = 11;
    public static final int ES_2021 = 12;
    public static final int ES_2022 = 13;
    public static final int ES_2023 = 14;
    public static final int ES_2024 = 15;
    public static final int ES_STAGING = Integer.MAX_VALUE;

    /** Accept "const" keyword and treat it as variable. Interim feature */
    final boolean constAsVar;

    /** Empty statements should be preserved in the AST */
    final boolean emptyStatements;

    /** ECMAScript language version. */
    final int ecmaScriptVersion;

    /**
     * Behavior when encountering a function declaration in a lexical context where only statements
     * are acceptable (function declarations are source elements, but not statements).
     */
    public enum FunctionStatementBehavior {
        /**
         * Accept the function declaration silently and treat it as if it were a function expression
         * assigned to a local variable.
         */
        ACCEPT,
        /**
         * Log a parser warning, but accept the function declaration and treat it as if it were a
         * function expression assigned to a local variable.
         */
        WARNING,
        /**
         * Raise a {@code SyntaxError}.
         */
        ERROR
    }

    /**
     * Behavior when encountering a function declaration in a lexical context where only statements
     * are acceptable (function declarations are source elements, but not statements).
     */
    final FunctionStatementBehavior functionStatement;

    /** Do not support non-standard syntax extensions. */
    final boolean syntaxExtensions;

    /** is this environment in scripting mode? */
    final boolean scripting;

    /** does the environment support shebang? */
    final boolean shebang;

    /** is this environment in strict mode? */
    final boolean strict;

    /** is BigInt supported? */
    final boolean allowBigInt;

    /** Are Annex B Web Compatibility extensions enabled? */
    final boolean annexB;

    /** Is class field support enabled. */
    final boolean classFields;

    /** Are import attributes enabled. */
    final boolean importAttributes;

    /** Are legacy import assertions enabled. */
    final boolean importAssertions;

    /** Is private field in enabled */
    final boolean privateFieldsIn;

    /** Is top-level await enabled? */
    final boolean topLevelAwait;

    /** Are V8 intrinsics supported? */
    final boolean v8Intrinsics;

    /** Are source phase imports enabled. */
    final boolean sourcePhaseImports;

    public ScriptEnvironment(boolean strict,
                    int ecmaScriptVersion,
                    boolean emptyStatements,
                    boolean syntaxExtensions,
                    boolean scripting,
                    boolean shebang,
                    boolean constAsVar,
                    boolean allowBigInt,
                    boolean annexB,
                    boolean classFields,
                    boolean importAttributes,
                    boolean importAssertions,
                    boolean sourcePhaseImports,
                    boolean privateFieldsIn,
                    boolean topLevelAwait,
                    boolean v8Intrinsics,
                    FunctionStatementBehavior functionStatementBehavior) {
        this.constAsVar = constAsVar;
        this.emptyStatements = emptyStatements;
        this.functionStatement = functionStatementBehavior;
        this.syntaxExtensions = syntaxExtensions;
        this.strict = strict;
        this.scripting = scripting;
        this.shebang = shebang;
        this.ecmaScriptVersion = ecmaScriptVersion;
        this.allowBigInt = allowBigInt;
        this.annexB = annexB;
        this.classFields = classFields;
        this.importAttributes = importAttributes;
        this.importAssertions = importAssertions;
        this.sourcePhaseImports = sourcePhaseImports;
        this.privateFieldsIn = privateFieldsIn;
        this.topLevelAwait = topLevelAwait;
        this.v8Intrinsics = v8Intrinsics;
    }

    public boolean isStrict() {
        return strict;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("hiding")
    public static final class Builder {
        private int ecmaScriptVersion = 6;
        private boolean constAsVar;
        private boolean emptyStatements;
        private boolean syntaxExtensions = true;
        private boolean scripting;
        private boolean shebang;
        private boolean strict;
        private boolean allowBigInt;
        private boolean annexB = true;
        private boolean classFields = true;
        private boolean importAttributes = false;
        private boolean importAssertions = false;
        private boolean sourcePhaseImports = false;
        private boolean privateFieldsIn = false;
        private boolean topLevelAwait = false;
        private boolean v8Intrinsics = false;
        private FunctionStatementBehavior functionStatementBehavior = FunctionStatementBehavior.ERROR;

        private Builder() {
        }

        public Builder ecmaScriptVersion(int ecmaScriptVersion) {
            this.ecmaScriptVersion = ecmaScriptVersion;
            return this;
        }

        public Builder constAsVar(boolean constAsVar) {
            this.constAsVar = constAsVar;
            return this;
        }

        public Builder emptyStatements(boolean emptyStatements) {
            this.emptyStatements = emptyStatements;
            return this;
        }

        public Builder syntaxExtensions(boolean syntaxExtensions) {
            this.syntaxExtensions = syntaxExtensions;
            return this;
        }

        public Builder scripting(boolean scripting) {
            this.scripting = scripting;
            return this;
        }

        public Builder shebang(boolean shebang) {
            this.shebang = shebang;
            return this;
        }

        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        public Builder allowBigInt(boolean allowBigInt) {
            this.allowBigInt = allowBigInt;
            return this;
        }

        public Builder annexB(boolean annexB) {
            this.annexB = annexB;
            return this;
        }

        public Builder classFields(boolean classFields) {
            this.classFields = classFields;
            return this;
        }

        public Builder importAttributes(boolean importAttributes) {
            this.importAttributes = importAttributes;
            return this;
        }

        public Builder importAssertions(boolean importAssertions) {
            this.importAssertions = importAssertions;
            return this;
        }

        public void sourcePhaseImports(boolean sourcePhaseImports) {
            this.sourcePhaseImports = sourcePhaseImports;
        }

        public Builder privateFieldsIn(boolean privateFieldsIn) {
            this.privateFieldsIn = privateFieldsIn;
            return this;
        }

        public Builder topLevelAwait(boolean topLevelAwait) {
            this.topLevelAwait = topLevelAwait;
            return this;
        }

        public Builder v8Intrinsics(boolean v8Intrinsics) {
            this.v8Intrinsics = v8Intrinsics;
            return this;
        }

        public Builder functionStatementBehavior(FunctionStatementBehavior functionStatementBehavior) {
            this.functionStatementBehavior = functionStatementBehavior;
            return this;
        }

        public ScriptEnvironment build() {
            return new ScriptEnvironment(strict, ecmaScriptVersion, emptyStatements, syntaxExtensions, scripting, shebang, constAsVar, allowBigInt, annexB,
                            classFields, importAttributes, importAssertions, sourcePhaseImports, privateFieldsIn, topLevelAwait, v8Intrinsics, functionStatementBehavior);
        }
    }
}
