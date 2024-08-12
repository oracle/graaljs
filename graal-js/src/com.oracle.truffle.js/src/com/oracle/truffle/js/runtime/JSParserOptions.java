/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime;

/**
 * Option values that affect JS language semantics, i.e. how code is parsed and translated. Must be
 * a subset of {@link JSLanguageOptions} to ensure that code is only shared between contexts that
 * agree on all these options (but may differ in others that do not affect code and shared state).
 *
 * @see JSLanguageOptions
 */
public record JSParserOptions(boolean strict,
                boolean scripting,
                boolean shebang,
                int ecmaScriptVersion,
                boolean syntaxExtensions,
                boolean constAsVar,
                boolean functionStatementError,
                boolean emptyStatements,
                boolean annexB,
                boolean allowBigInt,
                boolean classFields,
                boolean importAttributes,
                boolean importAssertions,
                boolean sourcePhaseImports,
                boolean privateFieldsIn,
                boolean topLevelAwait,
                boolean v8Intrinsics) {

    public static JSParserOptions fromLanguageOptions(JSLanguageOptions options) {
        int ecmaScriptVersion = options.ecmaScriptVersion();
        boolean strict = options.strict();
        boolean scripting = options.scripting();
        boolean shebang = options.shebang();
        boolean syntaxExtensions = options.syntaxExtensions();
        boolean constAsVar = options.constAsVar();
        boolean functionStatementError = options.functionStatementError();
        boolean emptyStatements = false;
        boolean annexB = options.annexB();
        boolean allowBigInt = options.bigInt();
        boolean classFields = options.classFields();
        boolean importAttributes = options.importAttributes();
        boolean importAssertions = options.importAssertions();
        boolean sourcePhaseImports = options.sourcePhaseImports();
        boolean privateFieldsIn = options.privateFieldsIn();
        boolean topLevelAwait = options.topLevelAwait();
        boolean v8Intrinsics = options.v8Intrinsics();
        return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, emptyStatements, annexB, allowBigInt,
                        classFields, importAttributes, importAssertions, sourcePhaseImports, privateFieldsIn, topLevelAwait, v8Intrinsics);
    }

    public JSParserOptions withStrict(@SuppressWarnings("hiding") boolean strict) {
        if (strict != this.strict) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, emptyStatements, annexB, allowBigInt,
                            classFields, importAttributes, importAssertions, sourcePhaseImports, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }
}
