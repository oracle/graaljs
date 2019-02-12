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
package com.oracle.truffle.js.parser;

import static com.oracle.truffle.js.runtime.JSContextOptions.newOptionDescriptor;
import static com.oracle.truffle.js.runtime.JSTruffleOptions.JS_OPTION_PREFIX;

import java.util.List;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.ParserOptions;

@SuppressWarnings("hiding")
public final class GraalJSParserOptions implements ParserOptions {
    public static final String SYNTAX_EXTENSIONS_NAME = JS_OPTION_PREFIX + "syntax-extensions";
    private static final OptionKey<Boolean> SYNTAX_EXTENSIONS = new OptionKey<>(false);
    private static final String SYNTAX_EXTENSIONS_HELP = "enable Nashorn syntax extensions";

    public static final String SCRIPTING_NAME = JS_OPTION_PREFIX + "scripting";
    private static final OptionKey<Boolean> SCRIPTING = new OptionKey<>(false);
    private static final String SCRIPTING_HELP = "enable scripting features (Nashorn compatibility option)";

    public static final String SHEBANG_NAME = JS_OPTION_PREFIX + "shebang";
    private static final OptionKey<Boolean> SHEBANG = new OptionKey<>(false);
    private static final String SHEBANG_HELP = "support files starting with #!";

    public static final String STRICT_NAME = JS_OPTION_PREFIX + "strict";
    private static final OptionKey<Boolean> STRICT = new OptionKey<>(false);
    private static final String STRICT_HELP = "run in strict mode";

    public static final String CONST_AS_VAR_NAME = JS_OPTION_PREFIX + "const-as-var";
    private static final OptionKey<Boolean> CONST_AS_VAR = new OptionKey<>(false);
    private static final String CONST_AS_VAR_HELP = "parse const declarations as a var";

    public static final String FUNCTION_STATEMENT_ERROR_NAME = JS_OPTION_PREFIX + "function-statement-error";
    private static final OptionKey<Boolean> FUNCTION_STATEMENT_ERROR = new OptionKey<>(false);
    private static final String FUNCTION_STATEMENT_ERROR_HELP = "Treat hoistable function statements in blocks as an error (in ES5 mode)";

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

    public GraalJSParserOptions() {
        this.strict = false;
        this.scripting = false;
        this.shebang = false;
        this.ecmaScriptVersion = JSTruffleOptions.MaxECMAScriptVersion;
        this.syntaxExtensions = false;
        this.constAsVar = false;
        this.functionStatementError = false;
        this.dumpOnError = false;
        this.emptyStatements = false;
        this.annexB = JSTruffleOptions.AnnexB;
    }

    private GraalJSParserOptions(boolean strict, boolean scripting, boolean shebang, int ecmaScriptVersion, boolean syntaxExtensions, boolean constAsVar, boolean functionStatementError,
                    boolean dumpOnError, boolean emptyStatements, boolean annexB) {
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

    @Override
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

    public static void describeOptions(List<OptionDescriptor> options) {
        options.add(newOptionDescriptor(SYNTAX_EXTENSIONS, SYNTAX_EXTENSIONS_NAME, OptionCategory.USER, SYNTAX_EXTENSIONS_HELP));
        options.add(newOptionDescriptor(SCRIPTING, SCRIPTING_NAME, OptionCategory.USER, SCRIPTING_HELP));
        options.add(newOptionDescriptor(STRICT, STRICT_NAME, OptionCategory.USER, STRICT_HELP));
        options.add(newOptionDescriptor(SHEBANG, SHEBANG_NAME, OptionCategory.USER, SHEBANG_HELP));
        options.add(newOptionDescriptor(CONST_AS_VAR, CONST_AS_VAR_NAME, OptionCategory.USER, CONST_AS_VAR_HELP));
        options.add(newOptionDescriptor(FUNCTION_STATEMENT_ERROR, FUNCTION_STATEMENT_ERROR_NAME, OptionCategory.USER, FUNCTION_STATEMENT_ERROR_HELP));
    }

    @Override
    public GraalJSParserOptions putOptions(OptionValues optionValues) {
        GraalJSParserOptions opts = this;
        opts = opts.putEcmaScriptVersion(JSContextOptions.ECMASCRIPT_VERSION.getValue(optionValues));
        opts = opts.putSyntaxExtensions(SYNTAX_EXTENSIONS.hasBeenSet(optionValues) ? SYNTAX_EXTENSIONS.getValue(optionValues) : JSContextOptions.NASHORN_COMPATIBILITY_MODE.getValue(optionValues));
        opts = opts.putScripting(SCRIPTING.getValue(optionValues));
        opts = opts.putShebang(SHEBANG.getValue(optionValues));
        opts = opts.putStrict(STRICT.getValue(optionValues));
        opts = opts.putConstAsVar(CONST_AS_VAR.getValue(optionValues));
        opts = opts.putFunctionStatementError(FUNCTION_STATEMENT_ERROR.getValue(optionValues));
        opts = opts.putAnnexB(JSContextOptions.ANNEX_B.getValue(optionValues));
        return opts;
    }

    public static GraalJSParserOptions fromOptions(OptionValues optionValues) {
        return new GraalJSParserOptions().putOptions(optionValues);
    }

    public GraalJSParserOptions putStrict(boolean strict) {
        if (strict != this.strict) {
            return new GraalJSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB);
        }
        return this;
    }

    public GraalJSParserOptions putScripting(boolean scripting) {
        if (scripting != this.scripting) {
            return new GraalJSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB);
        }
        return this;
    }

    public GraalJSParserOptions putShebang(boolean shebang) {
        if (shebang != this.shebang) {
            return new GraalJSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB);
        }
        return this;
    }

    public GraalJSParserOptions putEcmaScriptVersion(int ecmaScriptVersion) {
        if (ecmaScriptVersion != this.ecmaScriptVersion) {
            return new GraalJSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB);
        }
        return this;
    }

    public GraalJSParserOptions putSyntaxExtensions(boolean syntaxExtensions) {
        if (syntaxExtensions != this.syntaxExtensions) {
            return new GraalJSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB);
        }
        return this;
    }

    public GraalJSParserOptions putConstAsVar(boolean constAsVar) {
        if (constAsVar != this.constAsVar) {
            return new GraalJSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB);
        }
        return this;
    }

    public GraalJSParserOptions putFunctionStatementError(boolean functionStatementError) {
        if (functionStatementError != this.functionStatementError) {
            return new GraalJSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements,
                            annexB);
        }
        return this;
    }

    public GraalJSParserOptions putAnnexB(boolean annexB) {
        if (annexB != this.annexB) {
            return new GraalJSParserOptions(strict, scripting, shebang, ecmaScriptVersion, syntaxExtensions, constAsVar, functionStatementError, dumpOnError, emptyStatements, annexB);
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
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GraalJSParserOptions)) {
            return false;
        }
        GraalJSParserOptions other = (GraalJSParserOptions) obj;
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
        }
        return true;
    }
}
