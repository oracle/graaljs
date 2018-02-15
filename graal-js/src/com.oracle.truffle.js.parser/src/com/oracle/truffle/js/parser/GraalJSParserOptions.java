/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser;

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
    private static final String SYNTAX_EXTENSIONS_HELP = "enable Nashorn syntax extensions";
    private static final OptionKey<Boolean> SYNTAX_EXTENSIONS = new OptionKey<>(true);

    public static final String SCRIPTING_NAME = JS_OPTION_PREFIX + "scripting";
    private static final String SCRIPTING_HELP = "enable scripting features (Nashorn compatibility option)";
    private static final OptionKey<Boolean> SCRIPTING = new OptionKey<>(false);

    public static final String SHEBANG_NAME = JS_OPTION_PREFIX + "shebang";
    private static final String SHEBANG_HELP = "support files starting with #!";
    private static final OptionKey<Boolean> SHEBANG = new OptionKey<>(true);

    public static final String STRICT_NAME = JS_OPTION_PREFIX + "strict";
    private static final String STRICT_HELP = "run in strict mode";
    private static final OptionKey<Boolean> STRICT = new OptionKey<>(false);

    public static final String CONST_AS_VAR_NAME = JS_OPTION_PREFIX + "const-as-var";
    private static final String CONST_AS_VAR_HELP = "parse const declarations as a var";
    private static final OptionKey<Boolean> CONST_AS_VAR = new OptionKey<>(false);

    public static final String FUNCTION_STATEMENT_ERROR_NAME = JS_OPTION_PREFIX + "function-statement-error";
    private static final String FUNCTION_STATEMENT_ERROR_HELP = "Treat hoistable function statements in blocks as an error (in ES5 mode)";
    private static final OptionKey<Boolean> FUNCTION_STATEMENT_ERROR = new OptionKey<>(false);

    private final boolean strict;
    // Note: disregarding the value of the scripting option, scripting mode might be forced by
    // GraalJSParserHelper when the file begins with a '#' char
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
        options.add(OptionDescriptor.newBuilder(SYNTAX_EXTENSIONS, SYNTAX_EXTENSIONS_NAME).category(OptionCategory.USER).help(SYNTAX_EXTENSIONS_HELP).build());
        options.add(OptionDescriptor.newBuilder(SCRIPTING, SCRIPTING_NAME).category(OptionCategory.USER).help(SCRIPTING_HELP).build());
        options.add(OptionDescriptor.newBuilder(STRICT, STRICT_NAME).category(OptionCategory.USER).help(STRICT_HELP).build());
        options.add(OptionDescriptor.newBuilder(SHEBANG, SHEBANG_NAME).category(OptionCategory.USER).help(SHEBANG_HELP).build());
        options.add(OptionDescriptor.newBuilder(CONST_AS_VAR, CONST_AS_VAR_NAME).category(OptionCategory.USER).help(CONST_AS_VAR_HELP).build());
        options.add(OptionDescriptor.newBuilder(FUNCTION_STATEMENT_ERROR, FUNCTION_STATEMENT_ERROR_NAME).category(OptionCategory.USER).help(FUNCTION_STATEMENT_ERROR_HELP).build());
    }

    @Override
    public GraalJSParserOptions putOptions(OptionValues optionValues) {
        GraalJSParserOptions opts = this;
        opts = opts.putEcmaScriptVersion(JSContextOptions.ECMASCRIPT_VERSION.getValue(optionValues));
        opts = opts.putSyntaxExtensions(SYNTAX_EXTENSIONS.getValue(optionValues));
        opts = opts.putScripting(SCRIPTING.getValue(optionValues));
        opts = opts.putShebang(SHEBANG.getValue(optionValues));
        opts = opts.putStrict(STRICT.getValue(optionValues));
        opts = opts.putConstAsVar(CONST_AS_VAR.getValue(optionValues));
        opts = opts.putFunctionStatementError(FUNCTION_STATEMENT_ERROR.getValue(optionValues));
        opts = opts.putAnnexB(JSContextOptions.ANNEX_B.getValue(optionValues));
        return opts;
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
}
