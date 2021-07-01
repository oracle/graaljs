/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.JSContextOptions.ANNEX_B;
import static com.oracle.truffle.js.runtime.JSContextOptions.BIGINT;
import static com.oracle.truffle.js.runtime.JSContextOptions.CLASS_FIELDS;
import static com.oracle.truffle.js.runtime.JSContextOptions.CONST_AS_VAR;
import static com.oracle.truffle.js.runtime.JSContextOptions.ECMASCRIPT_VERSION;
import static com.oracle.truffle.js.runtime.JSContextOptions.FUNCTION_STATEMENT_ERROR;
import static com.oracle.truffle.js.runtime.JSContextOptions.NASHORN_COMPATIBILITY_MODE;
import static com.oracle.truffle.js.runtime.JSContextOptions.SCRIPTING;
import static com.oracle.truffle.js.runtime.JSContextOptions.SHEBANG;
import static com.oracle.truffle.js.runtime.JSContextOptions.STRICT;
import static com.oracle.truffle.js.runtime.JSContextOptions.SYNTAX_EXTENSIONS;
import static com.oracle.truffle.js.runtime.JSContextOptions.IMPORT_ASSERTIONS;

import org.graalvm.options.OptionValues;

@SuppressWarnings("hiding")
public final class JSParserOptions {

    private final boolean strict;
    private final boolean scripting;
    private final boolean shebang;
    private final int ecmaScriptVersion;
    private final boolean syntaxExtensions;
    private final boolean constAsVar;
    private final boolean functionStatementError;
    private final boolean dumpOnError;
    private final boolean emptyStatements;
    private final boolean annexB;
    private final boolean allowBigInt;
    private final boolean classFields;
    private final boolean importAssertions;

    public JSParserOptions() {
        this.strict = false;
        this.scripting = false;
        this.shebang = false;
        this.ecmaScriptVersion = JSConfig.MaxECMAScriptVersion;
        this.syntaxExtensions = false;
        this.constAsVar = false;
        this.functionStatementError = false;
        this.dumpOnError = false;
        this.emptyStatements = false;
        this.annexB = JSConfig.AnnexB;
        this.allowBigInt = true;
        this.classFields = true;
        this.importAssertions = false;
    }

    private JSParserOptions(boolean strict, boolean scripting, boolean shebang, int ecmaScriptVersion, boolean syntaxExtensions, boolean constAsVar, boolean functionStatementError,
                    boolean dumpOnError, boolean emptyStatements, boolean annexB, boolean allowBigInt, boolean classFields, boolean importAssertions) {
        this.strict = strict;
        this.scripting = scripting;
        this.shebang = shebang;
        this.ecmaScriptVersion = ecmaScriptVersion;
        this.syntaxExtensions = syntaxExtensions;
        this.constAsVar = constAsVar;
        this.functionStatementError = functionStatementError;
        this.dumpOnError = dumpOnError;
        this.emptyStatements = emptyStatements;
        this.annexB = annexB;
        this.allowBigInt = allowBigInt;
        this.classFields = classFields;
        this.importAssertions = importAssertions;
    }

    public boolean isStrict() {
        return strict;
    }

    public boolean isScripting() {
        return scripting;
    }

    public boolean isShebang() {
        return shebang;
    }

    public boolean isSyntaxExtensions() {
        return syntaxExtensions;
    }

    public boolean isConstAsVar() {
        return constAsVar;
    }

    public int getEcmaScriptVersion() {
        return ecmaScriptVersion;
    }

    public boolean isES6() {
        return ecmaScriptVersion >= 6;
    }

    public boolean isES8() {
        return ecmaScriptVersion >= 8;
    }

    public boolean isFunctionStatementError() {
        return functionStatementError;
    }

    public boolean isDumpOnError() {
        return dumpOnError;
    }

    public boolean isEmptyStatements() {
        return emptyStatements;
    }

    public boolean isAnnexB() {
        return annexB;
    }

    public boolean isAllowBigInt() {
        return allowBigInt;
    }

    public boolean isClassFields() {
        return classFields;
    }

    public boolean isImportAssertions() {
        return importAssertions;
    }

    public JSParserOptions putOptions(OptionValues optionValues) {
        JSParserOptions opts = this;
        int ecmaScriptVersion = ECMASCRIPT_VERSION.getValue(optionValues);
        opts = opts.putEcmaScriptVersion(ecmaScriptVersion);
        opts = opts.putSyntaxExtensions(SYNTAX_EXTENSIONS.hasBeenSet(optionValues) ? SYNTAX_EXTENSIONS.getValue(optionValues) : NASHORN_COMPATIBILITY_MODE.getValue(optionValues));
        opts = opts.putScripting(SCRIPTING.getValue(optionValues));
        opts = opts.putShebang(SHEBANG.hasBeenSet(optionValues) ? SHEBANG.getValue(optionValues) : ecmaScriptVersion >= JSConfig.ECMAScript2020);
        opts = opts.putStrict(STRICT.getValue(optionValues));
        opts = opts.putConstAsVar(CONST_AS_VAR.getValue(optionValues));
        opts = opts.putFunctionStatementError(FUNCTION_STATEMENT_ERROR.getValue(optionValues));
        opts = opts.putAnnexB(ANNEX_B.getValue(optionValues));
        opts = opts.putAllowBigInt(BIGINT.getValue(optionValues));
        opts = opts.putClassFields(CLASS_FIELDS.hasBeenSet(optionValues) ? CLASS_FIELDS.getValue(optionValues) : ecmaScriptVersion >= JSContextOptions.CLASS_FIELDS_ES_VERSION);
        opts = opts.putImportAssertions(IMPORT_ASSERTIONS.getValue(optionValues));
        return opts;
    }

    public static JSParserOptions fromOptions(OptionValues optionValues) {
        return new JSParserOptions().putOptions(optionValues);
    }

    public JSParserOptions putStrict(boolean strict) {
        if (strict != this.strict) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions);
        }
        return this;
    }

    public JSParserOptions putScripting(boolean scripting) {
        if (scripting != this.scripting) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions);
        }
        return this;
    }

    public JSParserOptions putShebang(boolean shebang) {
        if (shebang != this.shebang) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions);
        }
        return this;
    }

    public JSParserOptions putEcmaScriptVersion(int ecmaScriptVersion) {
        if (ecmaScriptVersion != this.ecmaScriptVersion) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions);
        }
        return this;
    }

    public JSParserOptions putSyntaxExtensions(boolean syntaxExtensions) {
        if (syntaxExtensions != this.syntaxExtensions) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions);
        }
        return this;
    }

    public JSParserOptions putConstAsVar(boolean constAsVar) {
        if (constAsVar != this.constAsVar) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions);
        }
        return this;
    }

    public JSParserOptions putFunctionStatementError(boolean functionStatementError) {
        if (functionStatementError != this.functionStatementError) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements,
                            annexB, allowBigInt, classFields, importAssertions);
        }
        return this;
    }

    public JSParserOptions putAnnexB(boolean annexB) {
        if (annexB != this.annexB) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions);
        }
        return this;
    }

    public JSParserOptions putAllowBigInt(boolean allowBigInt) {
        if (allowBigInt != this.allowBigInt) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions);
        }
        return this;
    }

    public JSParserOptions putClassFields(boolean classFields) {
        if (classFields != this.classFields) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions);
        }
        return this;
    }

    public JSParserOptions putImportAssertions(boolean importAssertions) {
        if (importAssertions != this.importAssertions) {
            return new JSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB, allowBigInt,
                            classFields, importAssertions);
        }
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (annexB ? 1231 : 1237);
        result = prime * result + (constAsVar ? 1231 : 1237);
        result = prime * result + (dumpOnError ? 1231 : 1237);
        result = prime * result + ecmaScriptVersion;
        result = prime * result + (emptyStatements ? 1231 : 1237);
        result = prime * result + (functionStatementError ? 1231 : 1237);
        result = prime * result + (scripting ? 1231 : 1237);
        result = prime * result + (shebang ? 1231 : 1237);
        result = prime * result + (strict ? 1231 : 1237);
        result = prime * result + (syntaxExtensions ? 1231 : 1237);
        result = prime * result + (allowBigInt ? 1231 : 1237);
        result = prime * result + (classFields ? 1231 : 1237);
        result = prime * result + (importAssertions ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof JSParserOptions)) {
            return false;
        }
        JSParserOptions other = (JSParserOptions) obj;
        if (annexB != other.annexB) {
            return false;
        } else if (constAsVar != other.constAsVar) {
            return false;
        } else if (dumpOnError != other.dumpOnError) {
            return false;
        } else if (ecmaScriptVersion != other.ecmaScriptVersion) {
            return false;
        } else if (emptyStatements != other.emptyStatements) {
            return false;
        } else if (functionStatementError != other.functionStatementError) {
            return false;
        } else if (scripting != other.scripting) {
            return false;
        } else if (shebang != other.shebang) {
            return false;
        } else if (strict != other.strict) {
            return false;
        } else if (syntaxExtensions != other.syntaxExtensions) {
            return false;
        } else if (allowBigInt != other.allowBigInt) {
            return false;
        } else if (classFields != other.classFields) {
            return false;
        } else if (importAssertions != other.importAssertions) {
            return false;
        }
        return true;
    }
}
