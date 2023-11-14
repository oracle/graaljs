/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;

public final class Strings {

    public static final TruffleString EMPTY_STRING = TruffleString.Encoding.UTF_16.getEmpty();
    public static final TruffleString LINE_SEPARATOR = constant("\n");
    public static final String LINE_SEPARATOR_JLS = toJavaString(LINE_SEPARATOR);

    /* Numeric */
    public static final TruffleString ZERO = constant("0");
    public static final TruffleString NEGATIVE_ZERO = constant("-0");
    public static final TruffleString INFINITY = constant("Infinity");
    public static final TruffleString NEGATIVE_INFINITY = constant("-Infinity");
    public static final TruffleString POSITIVE_INFINITY = constant("+Infinity");
    public static final TruffleString NAN = constant("NaN");
    public static final TruffleString CAPS_POSITIVE_INFINITY = constant("POSITIVE_INFINITY");
    public static final TruffleString CAPS_NEGATIVE_INFINITY = constant("NEGATIVE_INFINITY");
    public static final TruffleString CAPS_MAX_VALUE = constant("MAX_VALUE");
    public static final TruffleString CAPS_MIN_VALUE = constant("MIN_VALUE");
    public static final TruffleString CAPS_EPSILON = constant("EPSILON");
    public static final TruffleString CAPS_MAX_SAFE_INTEGER = constant("MAX_SAFE_INTEGER");
    public static final TruffleString CAPS_MIN_SAFE_INTEGER = constant("MIN_SAFE_INTEGER");

    /* Math */
    public static final TruffleString E = constant("E");
    public static final TruffleString PI = constant("PI");
    public static final TruffleString LN_10 = constant("LN10");
    public static final TruffleString LN_2 = constant("LN2");
    public static final TruffleString LOG_2_E = constant("LOG2E");
    public static final TruffleString LOG_10_E = constant("LOG10E");
    public static final TruffleString SQRT_1_2 = constant("SQRT1_2");
    public static final TruffleString SQRT_2 = constant("SQRT2");

    /* Special chars */
    public static final TruffleString ANGLE_BRACKET_OPEN = constant("<");
    public static final TruffleString ANGLE_BRACKET_OPEN_2 = constant("<<");
    public static final TruffleString ANGLE_BRACKET_CLOSE = constant(">");
    public static final TruffleString ANGLE_BRACKET_CLOSE_2 = constant(">>");
    public static final TruffleString ANGLE_BRACKET_CLOSE_3 = constant(">>>");
    public static final TruffleString ANGLE_BRACKET_OPEN_SLASH = constant("</");
    public static final TruffleString BACKSLASH = constant("\\");
    public static final TruffleString BIG_ARROW_SPACES = constant(" => ");
    public static final TruffleString BRACKET_OPEN = constant("[");
    public static final TruffleString BRACKET_OPEN_2 = constant("[[");
    public static final TruffleString BRACKET_CLOSE = constant("]");
    public static final TruffleString BRACKET_CLOSE_2_COLON = constant("]]: ");
    public static final TruffleString COMMA = constant(",");
    public static final TruffleString COMMA_SPC = constant(", ");
    public static final TruffleString COMMA_NEWLINE = constant(",\n");
    public static final TruffleString COLON = constant(":");
    public static final TruffleString COLON_SPACE = constant(": ");
    public static final TruffleString DASH = constant("-");
    public static final TruffleString DOUBLE_QUOTE = constant("\"");
    public static final TruffleString DOT = constant(".");
    public static final TruffleString DOT_SLASH = constant("./");
    public static final TruffleString DOT_DOT_SLASH = constant("../");
    public static final TruffleString DOT_DOT_DOT = constant("...");
    public static final TruffleString EMPTY_ARRAY = constant("[]");
    public static final TruffleString EMPTY_ARRAY_DOTS = constant("[...]");
    public static final TruffleString EMPTY_OBJECT = constant("{}");
    public static final TruffleString EMPTY_OBJECT_DOTS = constant("{...}");
    public static final TruffleString EQUALS_DOUBLE_QUOTE = constant("=\"");
    public static final TruffleString GET_DOLLAR_UNDERSCORE = constant("get $_");
    public static final TruffleString PAREN_OPEN = constant("(");
    public static final TruffleString PAREN_CLOSE = constant(")");
    public static final TruffleString UNDERSCORE = constant("_");
    public static final TruffleString UNDERSCORE_2 = constant("__");
    public static final TruffleString SLASH = constant("/");
    public static final TruffleString SPACE = constant(" ");
    public static final TruffleString SINGLE_QUOTE = constant("'");
    public static final TruffleString SYMBOL_PLUS = constant("+");
    public static final TruffleString SYMBOL_PLUS_PLUS = constant("++");
    public static final TruffleString SYMBOL_MINUS = constant("-");
    public static final TruffleString SYMBOL_MINUS_MINUS = constant("--");
    public static final TruffleString SYMBOL_AMPERSAND = constant("&");
    public static final TruffleString SYMBOL_PIPE = constant("|");
    public static final TruffleString SYMBOL_PERCENT = constant("%");
    public static final TruffleString SYMBOL_CARET = constant("^");
    public static final TruffleString SYMBOL_TILDE = constant("~");
    public static final TruffleString SYMBOL_EQUALS_EQUALS = constant("==");
    public static final TruffleString SYMBOL_STAR = constant("*");
    public static final TruffleString SYMBOL_STAR_STAR = constant("**");

    /* Regex */
    public static final TruffleString EMPTY_REGEX = constant("(?:)");
    public static final TruffleString INPUT = constant("input");
    public static final TruffleString MULTILINE = constant("multiline");
    public static final TruffleString LAST_MATCH = constant("lastMatch");
    public static final TruffleString LAST_PAREN = constant("lastParen");
    public static final TruffleString LEFT_CONTEXT = constant("leftContext");
    public static final TruffleString RIGHT_CONTEXT = constant("rightContext");
    public static final TruffleString $_ = constant("$_");
    public static final TruffleString $_AMPERSAND = constant("$&");
    public static final TruffleString $_PLUS = constant("$+");
    public static final TruffleString $_BACKTICK = constant("$`");
    public static final TruffleString $_SQUOT = constant("$'");
    public static final TruffleString $_1 = constant("$1");
    public static final TruffleString $_2 = constant("$2");
    public static final TruffleString $_3 = constant("$3");
    public static final TruffleString $_4 = constant("$4");
    public static final TruffleString $_5 = constant("$5");
    public static final TruffleString $_6 = constant("$6");
    public static final TruffleString $_7 = constant("$7");
    public static final TruffleString $_8 = constant("$8");
    public static final TruffleString $_9 = constant("$9");
    public static final TruffleString SET_INPUT = constant("setInput");

    public static final TruffleString BRACKET_SYMBOL_DOT = constant("[Symbol.");
    public static final TruffleString UND_DASH = constant("und-");
    public static final TruffleString DASH_PER_DASH = constant("-per-");
    public static final TruffleString X_DASH = constant("x-");
    public static final TruffleString DASH_X_DASH = constant("-x-");

    /* words */
    public static final TruffleString ACCESSOR = constant("accessor");
    public static final TruffleString ADD = constant("add");
    public static final TruffleString ALL = Strings.constant("all");
    public static final TruffleString ANY = Strings.constant("any");
    public static final TruffleString APPLY = Strings.constant("apply");
    public static final TruffleString ARGUMENTS = Strings.constant("arguments");
    public static final TruffleString BOUND = constant("bound");
    public static final TruffleString CALL = constant("call");
    public static final TruffleString CAUSE = constant("cause");
    public static final TruffleString COMPARE = constant("compare");
    public static final TruffleString CONSTRUCT = Strings.constant("construct");
    public static final TruffleString DEFAULT = constant("default");
    public static final TruffleString DEFAULT_VALUE = constant("defaultValue");
    public static final TruffleString DELETE = constant("delete");
    public static final TruffleString DONE = constant("done");
    public static final TruffleString EMPTY = constant("empty");
    public static final TruffleString EMPTY_X = constant("empty \u00d7 ");
    public static final TruffleString ENTRIES = constant("entries");
    public static final TruffleString UC_ERROR = constant("Error");
    public static final TruffleString FILE = constant("file");
    public static final TruffleString FLAGS = constant("flags");
    public static final TruffleString FORMAT = constant("format");
    public static final TruffleString FUNCTION = constant("function");
    public static final TruffleString GLOBAL = constant("global");
    public static final TruffleString HAS = constant("has");
    public static final TruffleString INSTANCE = constant("instance");
    public static final TruffleString JOIN = constant("join");
    public static final String JOIN_JLS = toJavaString(JOIN);
    public static final TruffleString JSON = constant("json");
    public static final TruffleString KEY = constant("key");
    public static final TruffleString KEYS = constant("keys");
    public static final TruffleString LENGTH = constant("length");
    public static final TruffleString LITERAL = constant("literal");
    public static final TruffleString LOWER = constant("lower");
    public static final TruffleString MESSAGE = constant("message");
    public static final TruffleString MODULE = constant("module");
    public static final TruffleString NAME = constant("name");
    public static final TruffleString NATIVE = constant("native");
    public static final TruffleString NEG = constant("neg");
    public static final TruffleString NEXT = constant("next");
    public static final TruffleString NOW = constant("now");
    public static final TruffleString NULL = constant("null");
    public static final TruffleString NULL_UNDEFINED = constant("null|undefined");
    public static final TruffleString UC_NUMBER = constant("Number");
    public static final TruffleString OBJECT = constant("object");
    public static final TruffleString PARSE = constant("parse");
    public static final TruffleString POS = constant("pos");
    public static final TruffleString PRIMITIVE_VALUE = constant("PrimitiveValue");
    public static final TruffleString RAW = constant("raw");
    public static final TruffleString RETURN = constant("return");
    public static final TruffleString SOURCE = constant("source");
    public static final TruffleString STRING = constant("string");
    public static final TruffleString UC_STRING = constant("String");
    public static final TruffleString SUPER = constant("super");
    public static final TruffleString SWITCH = constant("switch");
    public static final TruffleString SYMBOL = constant("symbol");
    public static final TruffleString UC_SYMBOL = constant("Symbol");
    public static final TruffleString THEN = Strings.constant("then");
    public static final TruffleString THIS = Strings.constant("this");
    public static final TruffleString THROW = Strings.constant("throw");
    public static final TruffleString UNDEFINED = constant("undefined");
    public static final TruffleString UNKNOWN = constant("unknown");
    public static final TruffleString UPPER = constant("upper");
    public static final TruffleString URL = constant("url");
    public static final TruffleString VALUE = constant("value");
    public static final String VALUE_JLS = toJavaString(VALUE);
    public static final TruffleString VALUES = constant("values");
    public static final TruffleString WITH = constant("with");

    /* method names */
    public static final TruffleString ANYFUNC = constant("anyfunc");
    public static final TruffleString AT = constant("at");
    public static final TruffleString COPY_WITHIN = constant("copyWithin");
    public static final TruffleString EVAL_FILE = constant("evalFile");
    public static final TruffleString EXTERNREF = constant("externref");
    public static final TruffleString FILL = constant("fill");
    public static final TruffleString FIND = constant("find");
    public static final TruffleString FIND_INDEX = constant("findIndex");
    public static final TruffleString FIND_LAST = constant("findLast");
    public static final TruffleString FIND_LAST_INDEX = constant("findLastIndex");
    public static final TruffleString FLAT = constant("flat");
    public static final TruffleString FLAT_MAP = constant("flatMap");
    public static final TruffleString INCLUDES = constant("includes");
    public static final TruffleString IS_VIEW = constant("isView");
    public static final TruffleString PARSE_INT = Strings.constant("parseInt");
    public static final TruffleString PARSE_FLOAT = Strings.constant("parseFloat");
    public static final TruffleString SLICE = constant("slice");
    public static final TruffleString STRING_MAX_LENGTH = constant("stringMaxLength");
    public static final TruffleString TO_JSON = constant("toJSON");
    public static final TruffleString TO_ISO_STRING = constant("toISOString");
    public static final TruffleString TO_LOCALE_STRING = constant("toLocaleString");
    public static final TruffleString TO_STRING = constant("toString");
    public static final String TO_STRING_JLS = toJavaString(TO_STRING);
    public static final TruffleString TO_UTC_STRING = constant("toUTCString");
    public static final TruffleString TO_GMT_STRING = constant("toGMTString");
    public static final TruffleString VALUE_OF = constant("valueOf");
    public static final String VALUE_OF_JLS = toJavaString(VALUE_OF);
    public static final TruffleString IMPORT_SCRIPT_ENGINE_GLOBAL_BINDINGS = constant("importScriptEngineGlobalBindings");
    public static final TruffleString HAS_INSTANCE = constant("hasInstance");
    public static final TruffleString IS_CONCAT_SPREADABLE = constant("isConcatSpreadable");
    public static final TruffleString ITERATOR = constant("iterator");
    public static final TruffleString ASYNC_ITERATOR = constant("asyncIterator");
    public static final TruffleString MATCH = constant("match");
    public static final TruffleString MATCH_ALL = constant("matchAll");
    public static final TruffleString REPLACE = constant("replace");
    public static final TruffleString SEARCH = constant("search");
    public static final TruffleString SPECIES = constant("species");
    public static final TruffleString SPLIT = constant("split");
    public static final TruffleString TO_STRING_TAG = constant("toStringTag");
    public static final TruffleString TO_PRIMITIVE = constant("toPrimitive");
    public static final TruffleString UNSCOPABLES = constant("unscopables");
    public static final TruffleString TO_REVERSED = constant("toReversed");
    public static final TruffleString TO_SORTED = constant("toSorted");
    public static final TruffleString TO_SPLICED = constant("toSpliced");

    public static final TruffleString UC_ARRAY = constant("Array");
    public static final TruffleString UC_OBJECT = constant("Object");

    public static final TruffleString CAPS_ID = constant("ID");
    public static final TruffleString CAPS_JSON = constant("JSON");
    public static final TruffleString CAPS_PWD = constant("PWD");

    /* globals */
    public static final TruffleString DOLLAR_ENV = constant("$ENV");

    /* hint words */
    public static final TruffleString HINT_STRING = constant("string");
    public static final TruffleString HINT_NUMBER = constant("number");
    public static final TruffleString HINT_DEFAULT = constant("default");

    /* html */
    public static final TruffleString HTML_QUOT = constant("&quot;");

    /* hex */
    public static final TruffleString UC_0X = constant("0X");
    public static final TruffleString LC_0X = constant("0x");

    public static final TruffleString PARENS_THIS = constant("(this)");
    public static final TruffleString SYMBOL_PAREN_OPEN = constant("Symbol(");

    /* Boolean */
    public static final TruffleString LC_BOOLEAN = constant("boolean");
    public static final TruffleString UC_BOOLEAN = constant("Boolean");
    public static final TruffleString BOOLEAN_PROTOTYPE = constant("Boolean.prototype");
    public static final TruffleString TRUE = constant("true");
    public static final TruffleString FALSE = constant("false");

    /* get / set */
    public static final TruffleString GET = constant("get");
    public static final TruffleString SET = constant("set");
    public static final TruffleString GET_SPC = constant("get ");
    public static final TruffleString SET_SPC = constant("set ");
    public static final TruffleString IS = constant("is");

    /* Letters */
    public static final TruffleString G = constant("g");
    public static final TruffleString N = constant("n");
    public static final TruffleString NFC = constant("NFC");
    public static final TruffleString NFD = constant("NFD");
    public static final TruffleString NFKC = constant("NFKC");
    public static final TruffleString NFKD = constant("NFKD");
    public static final TruffleString MS = constant("ms");
    public static final TruffleString Y = constant("y");

    /* Time */
    public static final TruffleString SECOND = constant("second");
    public static final TruffleString SECONDS = constant("seconds");
    public static final TruffleString MINUTE = constant("minute");
    public static final TruffleString MINUTES = constant("minutes");
    public static final TruffleString HOUR = constant("hour");
    public static final TruffleString HOURS = constant("hours");
    public static final TruffleString DAYS = constant("days");
    public static final TruffleString DAY = constant("day");
    public static final TruffleString WEEKDAY = constant("weekday");
    public static final TruffleString WEEKS = constant("weeks");
    public static final TruffleString WEEK = constant("week");
    public static final TruffleString MONTHS = constant("months");
    public static final TruffleString MONTH = constant("month");
    public static final TruffleString QUARTERS = constant("quarters");
    public static final TruffleString QUARTER = constant("quarter");
    public static final TruffleString YEAR = constant("year");
    public static final TruffleString YEARS = constant("years");

    public static final TruffleString DATE = constant("date");
    public static final TruffleString TIME = constant("time");

    public static final TruffleString DATE_STYLE = constant("dateStyle");
    public static final TruffleString TIME_STYLE = constant("timeStyle");

    public static final TruffleString NUMERIC = constant("numeric");

    public static final TruffleString OK = constant("ok");
    public static final TruffleString NOT_EQUAL = constant("not-equal");
    public static final TruffleString TIMED_OUT = constant("timed-out");

    /* snippets */
    public static final TruffleString ARRAY_PAREN_OPEN = constant("Array(");
    public static final TruffleString BOUND_SPC = constant("bound ");
    public static final TruffleString BRACKET_OBJECT_SPC = constant("[object ");
    public static final TruffleString BRACKET_BOOLEAN_SPC = constant("[Boolean ");
    public static final TruffleString BRACKET_DATE_SPC = constant("[Date ");
    public static final TruffleString COMMA_ANONYMOUS_BRACKETS = constant(", <anonymous>");
    public static final TruffleString FUNCTION_NATIVE_CODE_BODY = constant("() { [native code] }");
    public static final TruffleString FUNCTION_BODY_DOTS = constant("() {...}");
    public static final TruffleString FUNCTION_BODY_OMITTED = constant("...<omitted>...\n}");
    public static final TruffleString FUNCTION_SPC = constant("function ");
    public static final TruffleString FUNCTION_NATIVE_CODE = constant("function () { [native code] }");
    public static final TruffleString PROXY_PAREN = constant("Proxy(");

    public static final TruffleString ASYNC_SPC = constant("async ");
    public static final TruffleString ASYNC_PROMISE_ALL_BEGIN = constant("async Promise.all (index ");

    public static final TruffleString SPACE_PAREN_OPEN = constant(" (");
    public static final TruffleString NEW_SPACE = constant("new ");

    /* escaped chars */
    public static final TruffleString BACKSLASH_U = constant("\\u");
    public static final TruffleString BACKSLASH_U00 = constant("\\u00");
    public static final TruffleString BACKSLASH_UD = constant("\\ud");
    public static final TruffleString BACKSLASH_B = constant("\\b");
    public static final TruffleString BACKSLASH_F = constant("\\f");
    public static final TruffleString BACKSLASH_N = constant("\\n");
    public static final TruffleString BACKSLASH_R = constant("\\r");
    public static final TruffleString BACKSLASH_T = constant("\\t");
    public static final TruffleString BACKSLASH_BACKSLASH = constant("\\\\");
    public static final TruffleString BACKSLASH_DOUBLE_QUOTE = constant("\\\"");

    public static final TruffleString ESCAPE_B = constant("\\b");
    public static final TruffleString ESCAPE_F = constant("\\f");
    public static final TruffleString ESCAPE_N = constant("\\n");
    public static final TruffleString ESCAPE_R = constant("\\r");
    public static final TruffleString ESCAPE_T = constant("\\t");
    public static final TruffleString ESCAPE_U_00 = constant("\\u00");
    public static final TruffleString ESCAPE_BACKSLASH = constant("\\\\");
    public static final TruffleString ESCAPE_QUOTE = constant("\\\"");

    /* WASM */
    public static final TruffleString I_64 = constant("i64");
    public static final TruffleString I_32 = constant("i32");
    public static final TruffleString F_32 = constant("f32");
    public static final TruffleString F_64 = constant("f64");

    /* JSProxy */
    public static final TruffleString PROXY = constant("proxy");
    public static final TruffleString REVOKE = constant("revoke");
    public static final TruffleString REVOCABLE = constant("revocable");

    /* GlobalBuiltins */
    public static final TruffleString EVAL_OBJ_FILE_NAME = constant("name");
    public static final TruffleString EVAL_OBJ_SOURCE = constant("script");

    /* CommonJS */
    public static final TruffleString FILENAME_VAR_NAME = constant("__filename");
    public static final TruffleString DIRNAME_VAR_NAME = constant("__dirname");
    public static final TruffleString MODULE_PROPERTY_NAME = constant("module");
    public static final TruffleString EXPORTS_PROPERTY_NAME = constant("exports");
    public static final TruffleString REQUIRE_PROPERTY_NAME = constant("require");
    public static final TruffleString RESOLVE_PROPERTY_NAME = constant("resolve");
    public static final TruffleString LOADED_PROPERTY_NAME = constant("loaded");
    public static final TruffleString FILENAME_PROPERTY_NAME = constant("filename");
    public static final TruffleString ID_PROPERTY_NAME = constant("id");
    public static final TruffleString ENV_PROPERTY_NAME = constant("env");
    public static final TruffleString JS_EXT = constant(".js");
    public static final TruffleString JSON_EXT = constant(".json");
    public static final TruffleString NODE_EXT = constant(".node");
    public static final TruffleString PACKAGE_JSON_MAIN_PROPERTY_NAME = constant("main");
    public static final TruffleString PACKAGE_JSON_TYPE_PROPERTY_NAME = constant("type");
    public static final TruffleString PACKAGE_JSON_MODULE_VALUE = constant("module");

    /* Test262 */
    public static final TruffleString CREATE_REALM = constant("createRealm");
    public static final TruffleString DETACH_ARRAY_BUFFER = constant("detachArrayBuffer");
    public static final TruffleString EVAL_SCRIPT = constant("evalScript");
    public static final TruffleString GC = constant("gc");
    public static final TruffleString AGENT = constant("agent");
    public static final TruffleString START = constant("start");
    public static final TruffleString BROADCAST = constant("broadcast");
    public static final TruffleString GET_REPORT = constant("getReport");
    public static final TruffleString SLEEP = constant("sleep");
    public static final TruffleString MONOTONIC_NOW = constant("monotonicNow");
    public static final TruffleString RECEIVE_BROADCAST = constant("receiveBroadcast");
    public static final TruffleString REPORT = constant("report");
    public static final TruffleString LEAVING = constant("leaving");
    public static final TruffleString AGENT_START = constant("agentStart");
    public static final TruffleString AGENT_BROADCAST = constant("agentBroadcast");
    public static final TruffleString AGENT_GET_REPORT = constant("agentGetReport");
    public static final TruffleString AGENT_SLEEP = constant("agentSleep");
    public static final TruffleString AGENT_RECEIVE_BROADCAST = constant("agentReceiveBroadcast");
    public static final TruffleString AGENT_REPORT = constant("agentReport");
    public static final TruffleString AGENT_LEAVING = constant("agentLeaving");

    /* globals */
    public static final TruffleString LOAD = constant("load");
    public static final TruffleString LOAD_WITH_NEW_GLOBAL = constant("loadWithNewGlobal");
    public static final TruffleString GLOBAL_THIS = constant("globalThis");
    public static final TruffleString EXIT = constant("exit");
    public static final TruffleString QUIT = constant("quit");
    public static final TruffleString PARSE_TO_JSON = constant("parseToJSON");
    public static final TruffleString PRINT = constant("print");
    public static final TruffleString PRINT_ERR = constant("printErr");
    public static final TruffleString GRAAL = constant("Graal");
    public static final TruffleString LANGUAGE = constant("language");
    public static final TruffleString VERSION_GRAAL_VM = constant("versionGraalVM");
    public static final TruffleString VERSION_ECMA_SCRIPT = constant("versionECMAScript");
    public static final TruffleString IS_GRAAL_RUNTIME = constant("isGraalRuntime");
    public static final TruffleString SET_UNHANDLED_PROMISE_REJECTION_HANDLER = constant("setUnhandledPromiseRejectionHandler");
    public static final TruffleString UC_PACKAGES = constant("Packages");
    public static final TruffleString JAVA = constant("java");
    public static final TruffleString JAVAFX = constant("javafx");
    public static final TruffleString JAVAX = constant("javax");
    public static final TruffleString COM = constant("com");
    public static final TruffleString ORG = constant("org");
    public static final TruffleString EDU = constant("edu");
    public static final TruffleString CONSOLE = constant("console");
    public static final TruffleString EXEC = constant("exec");
    public static final TruffleString READ_FULLY = constant("readFully");
    public static final TruffleString READ_LINE = constant("readLine");
    public static final TruffleString $_EXEC = constant("$EXEC");
    public static final TruffleString $_EXIT = constant("$EXIT");
    public static final TruffleString $_OUT = constant("$OUT");
    public static final TruffleString $_ERR = constant("$ERR");
    public static final TruffleString UC_REALM = constant("Realm");
    public static final TruffleString GLOBAL__LINE__ = constant("__LINE__");
    public static final TruffleString GLOBAL__FILE__ = constant("__FILE__");
    public static final TruffleString GLOBAL__DIR__ = constant("__DIR__");
    public static final TruffleString _TIMEZONE = constant("_timezone");
    public static final TruffleString _SCRIPTING = constant("_scripting");
    public static final TruffleString _COMPILE_ONLY = constant("_compile_only");
    public static final TruffleString $_OPTIONS = constant("$OPTIONS");
    public static final TruffleString $_ARG = constant("$ARG");

    /* units */
    public static final TruffleString ACRE = constant("acre");
    public static final TruffleString BIT = constant("bit");
    public static final TruffleString BYTE = constant("byte");
    public static final TruffleString CELSIUS = constant("celsius");
    public static final TruffleString CENTIMETER = constant("centimeter");
    public static final TruffleString DEGREE = constant("degree");
    public static final TruffleString FAHRENHEIT = constant("fahrenheit");
    public static final TruffleString FLUID_OUNCE = constant("fluid-ounce");
    public static final TruffleString FOOT = constant("foot");
    public static final TruffleString GALLON = constant("gallon");
    public static final TruffleString GIGABIT = constant("gigabit");
    public static final TruffleString GIGABYTE = constant("gigabyte");
    public static final TruffleString GRAM = constant("gram");
    public static final TruffleString HECTARE = constant("hectare");
    public static final TruffleString INCH = constant("inch");
    public static final TruffleString KILOBIT = constant("kilobit");
    public static final TruffleString KILOBYTE = constant("kilobyte");
    public static final TruffleString KILOGRAM = constant("kilogram");
    public static final TruffleString KILOMETER = constant("kilometer");
    public static final TruffleString LITER = constant("liter");
    public static final TruffleString MEGABIT = constant("megabit");
    public static final TruffleString MEGABYTE = constant("megabyte");
    public static final TruffleString METER = constant("meter");
    public static final TruffleString MILE = constant("mile");
    public static final TruffleString MILE_SCANDINAVIAN = constant("mile-scandinavian");
    public static final TruffleString MILLILITER = constant("milliliter");
    public static final TruffleString MILLIMETER = constant("millimeter");
    public static final TruffleString MILLISECOND = constant("millisecond");
    public static final TruffleString OUNCE = constant("ounce");
    public static final TruffleString PERCENT = constant("percent");
    public static final TruffleString PETABYTE = constant("petabyte");
    public static final TruffleString POUND = constant("pound");
    public static final TruffleString STONE = constant("stone");
    public static final TruffleString TERABIT = constant("terabit");
    public static final TruffleString TERABYTE = constant("terabyte");
    public static final TruffleString YARD = constant("yard");

    public static final TruffleString GREGORY = constant("gregory");
    public static final TruffleString MEMORY = constant("memory");
    public static final TruffleString TABLE = constant("table");
    public static final TruffleString STATUS = constant("status");
    public static final TruffleString REASON = constant("reason");
    public static final TruffleString OPERATOR = constant("operator");
    public static final TruffleString TYPE = constant("type");
    public static final TruffleString DOT_PROTOTYPE = constant(".prototype");
    public static final TruffleString EVAL = constant("eval");
    public static final TruffleString A = constant("a");
    public static final TruffleString BIG = constant("big");
    public static final TruffleString BLINK = constant("blink");
    public static final TruffleString B = constant("b");
    public static final TruffleString TT = constant("tt");
    public static final TruffleString FONT = constant("font");
    public static final TruffleString COLOR = constant("color");
    public static final TruffleString I = constant("i");
    public static final TruffleString HREF = constant("href");
    public static final TruffleString SIZE = constant("size");
    public static final TruffleString SMALL = constant("small");
    public static final TruffleString STRIKE = constant("strike");
    public static final TruffleString SUB = constant("sub");
    public static final TruffleString SUP = constant("sup");
    public static final TruffleString TO_STRING_VALUE_NULL = constant("[object Null]");
    public static final TruffleString TO_STRING_VALUE_UNDEFINED = constant("[object Undefined]");
    public static final TruffleString TO_STRING_VALUE_ARRAY = constant("[object Array]");
    public static final TruffleString TO_STRING_VALUE_FUNCTION = constant("[object Function]");
    public static final TruffleString TO_STRING_VALUE_DATE = constant("[object Date]");
    public static final TruffleString TO_STRING_VALUE_OBJECT = constant("[object Object]");
    public static final TruffleString FULFILLED = constant("fulfilled");
    public static final TruffleString REJECTED = constant("rejected");

    public static final TruffleString CAPTURE_STACK_TRACE = constant("captureStackTrace");
    public static final TruffleString JAVA_CLASS_BRACKET = constant("JavaClass[");
    public static final TruffleString JAVA_OBJECT_BRACKET = constant("JavaObject[");
    public static final TruffleString RESOLVED = constant("resolved");
    public static final TruffleString PENDING = constant("pending");
    public static final TruffleString PROMISE_STATUS = constant("PromiseStatus");
    public static final TruffleString PROMISE_VALUE = constant("PromiseValue");
    public static final TruffleString TEST = constant("test");
    public static final TruffleString UNKNOWN_FILENAME = constant("<unknown>");
    public static final TruffleString DYNAMIC_FUNCTION_NAME = constant("anonymous");
    public static final TruffleString ASYNC = constant("async");
    public static final TruffleString GROUP = constant("group");
    public static final TruffleString GROUP_TO_MAP = constant("groupToMap");
    public static final TruffleString UC_M = constant("M");
    public static final TruffleString UC_M0 = constant("M0");
    public static final TruffleString UC_Z = constant("Z");
    public static final TruffleString Z = constant("z");
    public static final TruffleString UC_ETC = constant("Etc");
    public static final TruffleString UNICODE_MINUS_SIGN = constant("\u2212");

    public static final TruffleString MUTABLE = constant("mutable");
    public static final TruffleString ELEMENT = constant("element");
    public static final TruffleString INITIAL = constant("initial");
    public static final TruffleString MAXIMUM = constant("maximum");

    public static final TruffleString STATIC = constant("static");
    public static final TruffleString PRIVATE = constant("private");
    public static final TruffleString INIT = constant("init");

    /* end of constants */

    public static boolean isTString(Object string) {
        return string instanceof TruffleString;
    }

    public static TruffleString constant(String s) {
        TruffleString ret = fromJavaString(s);
        ret.hashCodeUncached(TruffleString.Encoding.UTF_16);
        return ret;
    }

    public static TruffleString fromJavaString(String str) {
        return fromJavaString(TruffleString.FromJavaStringNode.getUncached(), str);
    }

    public static TruffleString fromJavaString(TruffleString.FromJavaStringNode node, String str) {
        if (str == null) {
            return null;
        }
        return node.execute(str, TruffleString.Encoding.UTF_16);
    }

    public static TruffleString fromLong(long longValue) {
        return fromLong(TruffleString.FromLongNode.getUncached(), longValue);
    }

    public static TruffleString fromLong(TruffleString.FromLongNode node, long longValue) {
        return node.execute(longValue, TruffleString.Encoding.UTF_16, true);
    }

    public static TruffleString[] constantArray(String... strings) {
        TruffleString[] ret = new TruffleString[strings.length];
        for (int i = 0; i < strings.length; i++) {
            ret[i] = constant(strings[i]);
        }
        return ret;
    }

    public static int length(TruffleString s) {
        return s.byteLength(TruffleString.Encoding.UTF_16) >> 1;
    }

    public static boolean isEmpty(TruffleString s) {
        return length(s) == 0;
    }

    public static char charAt(TruffleString s, int i) {
        return charAt(TruffleString.ReadCharUTF16Node.getUncached(), s, i);
    }

    public static char charAt(TruffleString.ReadCharUTF16Node readRawNode, TruffleString s, int i) {
        return readRawNode.execute(s, i);
    }

    public static int codePointAt(TruffleString.CodePointAtByteIndexNode node, TruffleString s, int i) {
        return node.execute(s, i << 1, TruffleString.Encoding.UTF_16);
    }

    public static TruffleString concat(TruffleString s1, TruffleString s2) {
        return concat(TruffleString.ConcatNode.getUncached(), s1, s2);
    }

    public static TruffleString concat(TruffleString.ConcatNode node, TruffleString s1, TruffleString s2) {
        return node.execute(s1, s2, TruffleString.Encoding.UTF_16, JSConfig.LazyStrings);
    }

    public static TruffleString concatAll(TruffleString s, TruffleString... concat) {
        int len = length(s);
        for (TruffleString c : concat) {
            len += length(c);
        }
        var sb = builderCreate(len);
        TruffleStringBuilder.AppendStringNode.getUncached().execute(sb, s);
        for (TruffleString c : concat) {
            TruffleStringBuilder.AppendStringNode.getUncached().execute(sb, c);
        }
        return TruffleStringBuilder.ToStringNode.getUncached().execute(sb);
    }

    public static TruffleString lazySubstring(TruffleString s, int fromIndex) {
        return lazySubstring(s, fromIndex, length(s) - fromIndex);
    }

    public static TruffleString lazySubstring(TruffleString s, int fromIndex, int length) {
        return lazySubstring(TruffleString.SubstringByteIndexNode.getUncached(), s, fromIndex, length);
    }

    /**
     * Create a lazy substring, unconditionally. Use this method instead of
     * {@link #substring(JSContext, TruffleString, int, int)} when the resulting string is known to
     * never escape into user code, or the amount of memory leaked is constant and very small.
     */
    public static TruffleString lazySubstring(TruffleString.SubstringByteIndexNode node, TruffleString s, int fromIndex, int length) {
        return substring(true, node, s, fromIndex, length);
    }

    public static TruffleString substring(JSContext context, TruffleString s, int fromIndex) {
        return substring(context, s, fromIndex, length(s) - fromIndex);
    }

    public static TruffleString substring(JSContext context, TruffleString.SubstringByteIndexNode node, TruffleString s, int fromIndex) {
        return substring(context, node, s, fromIndex, length(s) - fromIndex);
    }

    public static TruffleString substring(JSContext context, TruffleString s, int fromIndex, int length) {
        return substring(context, TruffleString.SubstringByteIndexNode.getUncached(), s, fromIndex, length);
    }

    public static TruffleString substring(JSContext context, TruffleString.SubstringByteIndexNode node, TruffleString s, int fromIndex, int length) {
        return substring(context.getLanguageOptions().stringLazySubstrings(), node, s, fromIndex, length);
    }

    /**
     * Create a substring. If {@code lazy} is {@code true}, the internal array of string {@code s}
     * is re-used, without copying. This is a memory leak, since the resulting string's internal
     * array is never trimmed.
     */
    public static TruffleString substring(boolean lazy, TruffleString.SubstringByteIndexNode node, TruffleString s, int fromIndex, int length) {
        return length == 0 ? Strings.EMPTY_STRING : node.execute(s, fromIndex << 1, length << 1, TruffleString.Encoding.UTF_16, lazy);
    }

    public static boolean startsWith(TruffleString s1, TruffleString s2) {
        return startsWith(s1, s2, 0);
    }

    public static boolean startsWith(TruffleString s1, TruffleString s2, int startPos) {
        return startsWith(TruffleString.RegionEqualByteIndexNode.getUncached(), s1, s2, startPos);
    }

    public static boolean startsWith(TruffleString.RegionEqualByteIndexNode regionEqualsNode, TruffleString s1, TruffleString s2) {
        return startsWith(regionEqualsNode, s1, s2, 0);
    }

    public static boolean startsWith(TruffleString.RegionEqualByteIndexNode regionEqualsNode, TruffleString s1, TruffleString s2, int startPos) {
        return length(s1) - startPos >= length(s2) && regionEquals(regionEqualsNode, s1, startPos, s2, 0, length(s2));
    }

    public static boolean regionEquals(TruffleString s1, int offset1, TruffleString s2, int offset2, int length) {
        return regionEquals(TruffleString.RegionEqualByteIndexNode.getUncached(), s1, offset1, s2, offset2, length);
    }

    public static boolean regionEquals(TruffleString.RegionEqualByteIndexNode regionEqualsNode, TruffleString s1, int offset1, TruffleString s2, int offset2, int length) {
        return regionEqualsNode.execute(s1, offset1 << 1, s2, offset2 << 1, length << 1, TruffleString.Encoding.UTF_16);
    }

    public static boolean endsWith(TruffleString s1, TruffleString s2) {
        return endsWith(TruffleString.RegionEqualByteIndexNode.getUncached(), s1, s2);
    }

    public static boolean endsWith(TruffleString.RegionEqualByteIndexNode regionEqualsNode, TruffleString s1, TruffleString s2) {
        return length(s1) >= length(s2) && regionEquals(regionEqualsNode, s1, length(s1) - length(s2), s2, 0, length(s2));
    }

    public static boolean contains(TruffleString s, char c) {
        return indexOf(s, c) >= 0;
    }

    public static int indexOf(TruffleString string, char c) {
        return string.charIndexOfAnyCharUTF16Uncached(0, length(string), new char[]{c});
    }

    public static int indexOfAny(TruffleString s, char... chars) {
        return s.charIndexOfAnyCharUTF16Uncached(0, length(s), chars);
    }

    public static int indexOfAny(TruffleString.CharIndexOfAnyCharUTF16Node node, TruffleString s, char... chars) {
        return node.execute(s, 0, length(s), chars);
    }

    public static int indexOf(TruffleString.ByteIndexOfCodePointNode node, TruffleString s, int codepoint) {
        return indexOf(node, s, codepoint, 0);
    }

    public static int indexOf(TruffleString s, int codepoint, int fromIndex) {
        return indexOf(TruffleString.ByteIndexOfCodePointNode.getUncached(), s, codepoint, fromIndex);
    }

    public static int indexOf(TruffleString.ByteIndexOfCodePointNode node, TruffleString s, int codepoint, int fromIndex) {
        if (fromIndex >= length(s)) {
            return -1;
        }
        return node.execute(s, codepoint, fromIndex << 1, length(s) << 1, TruffleString.Encoding.UTF_16) >> 1;
    }

    public static int indexOf(TruffleString s1, TruffleString s2) {
        return indexOf(s1, s2, 0);
    }

    public static int indexOf(TruffleString.ByteIndexOfStringNode node, TruffleString s1, TruffleString s2) {
        return indexOf(node, s1, s2, 0);
    }

    public static int indexOf(TruffleString s1, TruffleString s2, int fromIndex) {
        return indexOf(TruffleString.ByteIndexOfStringNode.getUncached(), s1, s2, fromIndex);
    }

    public static int indexOf(TruffleString s1, TruffleString s2, int fromIndex, int toIndex) {
        return indexOf(TruffleString.ByteIndexOfStringNode.getUncached(), s1, s2, fromIndex, toIndex);
    }

    public static int indexOf(TruffleString.ByteIndexOfStringNode node, TruffleString s1, TruffleString s2, int fromIndex) {
        return indexOf(node, s1, s2, fromIndex, length(s1));
    }

    public static int indexOf(TruffleString.ByteIndexOfStringNode node, TruffleString s1, TruffleString s2, int fromIndex, int toIndex) {
        int fromIndexPos = Math.max(fromIndex, 0);
        if (length(s2) == 0) {
            return fromIndexPos;
        }
        return length(s1) - fromIndexPos >= length(s2) ? node.execute(s1, s2, fromIndexPos << 1, toIndex << 1, TruffleString.Encoding.UTF_16) >> 1 : -1;
    }

    public static int lastIndexOf(TruffleString.LastByteIndexOfStringNode lastIndexOfNode, TruffleString s1, TruffleString s2, int fromIndex) {
        return lastIndexOfNode.execute(s1, s2, Math.min(fromIndex + length(s2), length(s1)) << 1, 0, TruffleString.Encoding.UTF_16) >> 1;
    }

    public static int lastIndexOf(TruffleString s, int codePoint) {
        return lastIndexOf(TruffleString.LastByteIndexOfCodePointNode.getUncached(), s, codePoint);
    }

    public static int lastIndexOf(TruffleString.LastByteIndexOfCodePointNode node, TruffleString s, int codePoint) {
        return node.execute(s, codePoint, length(s) << 1, 0, TruffleString.Encoding.UTF_16) >> 1;
    }

    public static boolean equals(TruffleString s1, TruffleString s2) {
        return equals(TruffleString.EqualNode.getUncached(), s1, s2);
    }

    public static boolean equals(TruffleString.EqualNode node, TruffleString s1, TruffleString s2) {
        if (s1 == null) {
            return s2 == null;
        }
        if (!isTString(s2)) {
            return false;
        }
        return node.execute(s1, s2, TruffleString.Encoding.UTF_16);
    }

    public static TruffleString replace(TruffleString s, TruffleString search, TruffleString replace) {
        int pos = indexOf(s, search);
        if (pos < 0) {
            return s;
        }
        if (length(s) == length(search)) {
            return replace;
        }
        TruffleStringBuilder sb = builderCreate(length(s));
        int lastEndPos = 0;
        do {
            builderAppend(sb, s, lastEndPos, pos);
            builderAppend(sb, replace);
            lastEndPos = pos + length(search);
            pos = indexOf(s, search, lastEndPos);
        } while (pos >= 0);
        builderAppend(sb, s, lastEndPos, length(s));
        return builderToString(sb);
    }

    public static int compareTo(TruffleString a, TruffleString b) {
        return TruffleString.CompareCharsUTF16Node.getUncached().execute(a, b);
    }

    public static int compareTo(TruffleString.CompareCharsUTF16Node node, TruffleString a, TruffleString b) {
        return node.execute(a, b);
    }

    public static String toJavaString(TruffleString s) {
        return toJavaString(TruffleString.ToJavaStringNode.getUncached(), s);
    }

    public static String toJavaString(TruffleString.ToJavaStringNode node, TruffleString s) {
        return s == null ? null : node.execute(s);
    }

    public static TruffleString toUpperCase(TruffleString s, Locale locale) {
        return fromJavaString(javaStringToUpperCase(toJavaString(s), locale));
    }

    @TruffleBoundary
    public static String javaStringToLowerCase(String s, Locale locale) {
        return s.toLowerCase(locale);
    }

    @TruffleBoundary
    public static String javaStringToUpperCase(String s, Locale locale) {
        return s.toUpperCase(locale);
    }

    public static TruffleString lazyTrim(TruffleString s) {
        int end = length(s);
        int start = 0;
        while ((start < end) && (charAt(s, start) <= ' ')) {
            start++;
        }
        while ((start < end) && (charAt(s, end - 1) <= ' ')) {
            end--;
        }
        return ((start > 0) || (end < length(s))) ? lazySubstring(s, start, end - start) : s;
    }

    public static long parseLong(TruffleString s) throws TruffleString.NumberFormatException {
        return parseLong(s, 10);
    }

    public static long parseLong(TruffleString s, int radix) throws TruffleString.NumberFormatException {
        return parseLong(TruffleString.ParseLongNode.getUncached(), s, radix);
    }

    public static long parseLong(TruffleString.ParseLongNode node, TruffleString s, int radix) throws TruffleString.NumberFormatException {
        return node.execute(s, radix);
    }

    public static double parseDouble(TruffleString s) throws TruffleString.NumberFormatException {
        return parseDouble(TruffleString.ParseDoubleNode.getUncached(), s);
    }

    public static double parseDouble(TruffleString.ParseDoubleNode node, TruffleString s) throws TruffleString.NumberFormatException {
        return node.execute(s);
    }

    public static TruffleString fromCodePoint(int c) {
        return fromCodePoint(TruffleString.FromCodePointNode.getUncached(), c);
    }

    public static TruffleString fromCodePoint(TruffleString.FromCodePointNode node, int c) {
        assert c >= 0;
        return node.execute(c, TruffleString.Encoding.UTF_16, true);
    }

    public static TruffleString fromBoolean(boolean b) {
        return b ? TRUE : FALSE;
    }

    public static TruffleString fromInt(int intValue) {
        return fromLong(intValue);
    }

    public static TruffleString fromDouble(double d) {
        return TruffleString.FromJavaStringNode.getUncached().execute(doubleToJavaString(d), TruffleString.Encoding.UTF_16);
    }

    @TruffleBoundary
    private static String doubleToJavaString(double d) {
        return String.valueOf(d);
    }

    public static TruffleString fromNumber(Number number) {
        if (number instanceof Integer) {
            return fromInt(number.intValue());
        }
        if (number instanceof Long) {
            return fromLong(number.longValue());
        }
        if (number instanceof Double) {
            return fromDouble(number.doubleValue());
        }
        throw CompilerDirectives.shouldNotReachHere();
    }

    public static TruffleString fromBigInt(BigInt bi) {
        return fromJavaString(bi.toString());
    }

    public static TruffleString fromBigInt(BigInt bi, int radix) {
        return fromJavaString(bi.toString(radix));
    }

    public static TruffleString fromObject(Object o) {
        return fromJavaString(objectToJavaString(o));
    }

    @TruffleBoundary
    private static String objectToJavaString(Object o) {
        return String.valueOf(o);
    }

    public static TruffleString fromCharArray(char[] chars) {
        return fromCharArray(chars, 0, chars.length);
    }

    public static TruffleString fromCharArray(TruffleString.FromCharArrayUTF16Node node, char[] chars) {
        return fromCharArray(node, chars, 0, chars.length);
    }

    public static TruffleString fromCharArray(char[] chars, int fromIndex, int length) {
        return fromCharArray(TruffleString.FromCharArrayUTF16Node.getUncached(), chars, fromIndex, length);
    }

    public static TruffleString fromCharArray(TruffleString.FromCharArrayUTF16Node node, char[] chars, int fromIndex, int length) {
        return node.execute(chars, fromIndex, length);
    }

    public static TruffleString intToHexString(char i) {
        return fromJavaString(Integer.toHexString(i));
    }

    public static TruffleString flatten(TruffleString.MaterializeNode materializeNode, TruffleString value) {
        materializeNode.execute(value, TruffleString.Encoding.UTF_16);
        return value;
    }

    public static String interopAsString(Object key) throws UnsupportedMessageException {
        return interopAsString(InteropLibrary.getUncached(), key);
    }

    public static String interopAsString(InteropLibrary stringInterop, Object key) throws UnsupportedMessageException {
        return key instanceof String ? (String) key : stringInterop.asString(key);
    }

    public static TruffleString interopAsTruffleString(Object key) {
        return interopAsTruffleString(key, InteropLibrary.getUncached(), TruffleString.SwitchEncodingNode.getUncached());
    }

    public static TruffleString interopAsTruffleString(Object key, InteropLibrary stringInterop) {
        return interopAsTruffleString(key, stringInterop, TruffleString.SwitchEncodingNode.getUncached());
    }

    public static TruffleString interopAsTruffleString(Object key, InteropLibrary stringInterop, TruffleString.SwitchEncodingNode switchEncodingNode) {
        assert stringInterop.isString(key) : key;
        TruffleString truffleString;
        if (key instanceof TruffleString) {
            truffleString = (TruffleString) key;
        } else {
            try {
                truffleString = stringInterop.asTruffleString(key);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(key, e, "asTruffleString", stringInterop);
            }
        }
        return switchEncodingNode.execute(truffleString, TruffleString.Encoding.UTF_16);
    }

    public static BigInt parseBigInt(TruffleString s) {
        return BigInt.valueOf(toJavaString(s));
    }

    public static BigInteger parseBigInteger(TruffleString s, int radix) {
        return new BigInteger(toJavaString(s), radix);
    }

    public static TruffleStringBuilderUTF16 builderCreate() {
        return TruffleStringBuilder.createUTF16();
    }

    public static TruffleStringBuilderUTF16 builderCreate(int capacity) {
        return TruffleStringBuilder.createUTF16(capacity << 1);
    }

    public static void builderAppend(TruffleStringBuilder sb, char chr) {
        TruffleStringBuilder.AppendCharUTF16Node.getUncached().execute(sb, chr);
    }

    public static void builderAppend(TruffleStringBuilder.AppendCharUTF16Node node, TruffleStringBuilder sb, char chr) {
        node.execute(sb, chr);
    }

    public static void builderAppend(TruffleStringBuilder sb, int i) {
        TruffleStringBuilder.AppendIntNumberNode.getUncached().execute(sb, i);
    }

    public static void builderAppend(TruffleStringBuilder.AppendIntNumberNode node, TruffleStringBuilder sb, int i) {
        node.execute(sb, i);
    }

    public static void builderAppend(TruffleStringBuilder sb, long i) {
        TruffleStringBuilder.AppendLongNumberNode.getUncached().execute(sb, i);
    }

    public static void builderAppend(TruffleStringBuilder.AppendLongNumberNode node, TruffleStringBuilder sb, long i) {
        node.execute(sb, i);
    }

    public static void builderAppend(TruffleStringBuilder sb, String str) {
        TruffleStringBuilder.AppendJavaStringUTF16Node.getUncached().execute(sb, str, 0, str.length());
    }

    public static void builderAppend(TruffleStringBuilder sb, TruffleString str) {
        builderAppendLen(sb, str, 0, length(str));
    }

    public static void builderAppend(TruffleStringBuilder.AppendStringNode node, TruffleStringBuilder sb, TruffleString str) {
        node.execute(sb, str);
    }

    public static void builderAppend(TruffleStringBuilder sb, TruffleString str, int start, int end) {
        builderAppendLen(sb, str, start, end - start);
    }

    public static void builderAppend(TruffleStringBuilder.AppendSubstringByteIndexNode node, TruffleStringBuilder sb, TruffleString str, int start, int end) {
        builderAppendLen(node, sb, str, start, end - start);
    }

    public static void builderAppendLen(TruffleStringBuilder sb, TruffleString str, int start, int len) {
        builderAppendLen(TruffleStringBuilder.AppendSubstringByteIndexNode.getUncached(), sb, str, start, len);
    }

    public static void builderAppendLen(TruffleStringBuilder.AppendSubstringByteIndexNode node, TruffleStringBuilder sb, TruffleString str, int start, int len) {
        node.execute(sb, str, start << 1, len << 1);
    }

    public static TruffleString builderToString(TruffleStringBuilder sb) {
        return builderToString(TruffleStringBuilder.ToStringNode.getUncached(), sb);
    }

    public static TruffleString builderToString(TruffleStringBuilder.ToStringNode node, TruffleStringBuilder sb) {
        return node.execute(sb);
    }

    public static String builderToJavaString(TruffleStringBuilder sb) {
        return toJavaString(builderToString(sb));
    }

    public static int builderLength(TruffleStringBuilder sb) {
        return sb.byteLength() >> 1;
    }

    public static TruffleString[] convertJavaStringArray(String[] array) {
        TruffleString[] ret = new TruffleString[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = fromJavaString(array[i]);
        }
        return ret;
    }

    public static TruffleString addBrackets(TruffleString str) {
        return Strings.concatAll(Strings.BRACKET_OPEN, str, Strings.BRACKET_CLOSE);
    }

    public static TruffleString format(String formatString, Object... args) {
        return fromJavaString(String.format(formatString, args));
    }

    @TruffleBoundary
    public static TruffleString[] split(JSContext context, TruffleString str, TruffleString delimiter) {
        if (isEmpty(str)) {
            return new TruffleString[0];
        }
        int pos = indexOf(str, delimiter);
        if (pos < 0) {
            return new TruffleString[]{str};
        }
        ArrayList<TruffleString> ret = new ArrayList<>();
        int lastEnd = 0;
        do {
            ret.add(substring(context, str, lastEnd, pos - lastEnd));
            lastEnd = pos + length(delimiter);
            pos = indexOf(str, delimiter, lastEnd);
        } while (pos >= 0);
        ret.add(substring(context, str, lastEnd, length(str) - lastEnd));
        return ret.toArray(new TruffleString[0]);
    }
}
