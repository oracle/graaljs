/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
@SuppressWarnings("hiding")
public record JSParserOptions(boolean strict,
                boolean scripting,
                boolean shebang,
                int ecmaScriptVersion,
                boolean syntaxExtensions,
                boolean constAsVar,
                boolean functionStatementError,
                boolean dumpOnError,
                boolean emptyStatements,
                boolean annexB,
                boolean allowBigInt,
                boolean classFields,
                boolean importAssertions,
                boolean privateFieldsIn,
                boolean topLevelAwait,
                boolean v8Intrinsics) {

    public static JSParserOptions fromOptions(JSContextOptions contextOpts) {
        int ecmaScriptVersion = contextOpts.getEcmaScriptVersion();
        boolean strict = contextOpts.isStrict();
        boolean scripting = contextOpts.isScripting();
        boolean shebang = contextOpts.isShebang();
        boolean syntaxExtensions = contextOpts.isSyntaxExtensions();
        boolean constAsVar = contextOpts.isConstAsVar();
        boolean functionStatementError = contextOpts.isFunctionStatementError();
        boolean dumpOnError = false;
        boolean emptyStatements = false;
        boolean annexB = contextOpts.isAnnexB();
        boolean allowBigInt = contextOpts.isBigInt();
        boolean classFields = contextOpts.isClassFields();
        boolean importAssertions = contextOpts.isImportAssertions();
        boolean privateFieldsIn = contextOpts.isPrivateFieldsIn();
        boolean topLevelAwait = contextOpts.isTopLevelAwait();
        boolean v8Intrinsics = contextOpts.isV8Intrinsics();
        return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                        classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
    }

    public JSParserOptions putStrict(boolean strict) {
        if (strict != this.strict) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putScripting(boolean scripting) {
        if (scripting != this.scripting) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putShebang(boolean shebang) {
        if (shebang != this.shebang) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putEcmaScriptVersion(int ecmaScriptVersion) {
        if (ecmaScriptVersion != this.ecmaScriptVersion) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putSyntaxExtensions(boolean syntaxExtensions) {
        if (syntaxExtensions != this.syntaxExtensions) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putConstAsVar(boolean constAsVar) {
        if (constAsVar != this.constAsVar) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putFunctionStatementError(boolean functionStatementError) {
        if (functionStatementError != this.functionStatementError) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements,
                            annexB, allowBigInt, classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putAnnexB(boolean annexB) {
        if (annexB != this.annexB) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putAllowBigInt(boolean allowBigInt) {
        if (allowBigInt != this.allowBigInt) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putClassFields(boolean classFields) {
        if (classFields != this.classFields) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putImportAssertions(boolean importAssertions) {
        if (importAssertions != this.importAssertions) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putPrivateFieldsIn(boolean privateFieldsIn) {
        if (privateFieldsIn != this.privateFieldsIn) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putTopLevelAwait(boolean topLevelAwait) {
        if (topLevelAwait != this.topLevelAwait) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }

    public JSParserOptions putV8Intrinsics(boolean v8Intrinsics) {
        if (v8Intrinsics != this.v8Intrinsics) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions, privateFieldsIn, topLevelAwait, v8Intrinsics);
        }
        return this;
    }
}
