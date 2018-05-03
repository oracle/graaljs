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
package com.oracle.truffle.js.test.sdk.tck;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.tck.InlineSnippet;
import org.graalvm.polyglot.tck.Snippet;
import org.graalvm.polyglot.tck.TypeDescriptor;
import org.graalvm.polyglot.tck.LanguageProvider;
import org.graalvm.polyglot.tck.ResultVerifier;
import org.junit.Assert;

import static org.graalvm.polyglot.tck.TypeDescriptor.ANY;

public class JavaScriptTCKLanguageProvider implements LanguageProvider {
    private static final String ID = "js";
    private static final String PATTERN_VALUE_FNC = "(function () {return %s;})";
    private static final String PATTERN_BIN_OP_FNC = "(function (a,b) {return a %s b;})";
    private static final String PATTERN_PREFIX_OP_FNC = "(function (a) {return %s a;})";
    private static final String PATTERN_POSTFIX_OP_FNC = "(function (a) {return a %s;})";
    private static final String[] PATTERN_STATEMENT = {
                    "(function () {let r; %s\n return r;})",
                    "(function (p1) {let r; %s\n return r;})",
                    "(function (p1, p2) {let r; %s\n return r;})",
                    "(function (p1, p2, p3) {let r; %s\n return r;})",
    };

    public JavaScriptTCKLanguageProvider() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Value createIdentityFunction(final Context context) {
        return eval(context, "(function (a) {return a;})");
    }

    @Override
    public Collection<? extends Snippet> createValueConstructors(final Context context) {
        final List<Snippet> vals = new ArrayList<>();
        // boolean
        vals.add(createValueConstructor(context, "false", TypeDescriptor.BOOLEAN));
        // number
        vals.add(createValueConstructor(context, "1", TypeDescriptor.NUMBER));
        vals.add(createValueConstructor(context, "1.1", TypeDescriptor.NUMBER));
        // string
        vals.add(createValueConstructor(context, "'test'", TypeDescriptor.STRING));
        // lazy string
        vals.add(createValueConstructor(context, "'0123456789' + '0123456789'", TypeDescriptor.STRING));
        // arrays
        final TypeDescriptor numArray = TypeDescriptor.intersection(
                        TypeDescriptor.OBJECT,
                        TypeDescriptor.array(TypeDescriptor.NUMBER));
        vals.add(createValueConstructor(context, "[1,2]", numArray));
        vals.add(createValueConstructor(context, "['A',65]",
                        TypeDescriptor.intersection(
                                        TypeDescriptor.OBJECT,
                                        TypeDescriptor.ARRAY)));
        vals.add(createValueConstructor(context, "new Uint8Array(2)", numArray));
        vals.add(createValueConstructor(context, "new Uint16Array(2)", numArray));
        vals.add(createValueConstructor(context, "new Uint32Array(2)", numArray));
        vals.add(createValueConstructor(context, "new Int8Array(2)", numArray));
        vals.add(createValueConstructor(context, "new Int16Array(2)", numArray));
        vals.add(createValueConstructor(context, "new Int32Array(2)", numArray));
        vals.add(createValueConstructor(context, "new Int32Array(2)", numArray));
        vals.add(createValueConstructor(context, "new Float32Array(2)", numArray));
        vals.add(createValueConstructor(context, "new Float64Array(2)", numArray));
        // object
        vals.add(createValueConstructor(context, "({'name':'test'})", TypeDescriptor.OBJECT));
        // function
        vals.add(createValueConstructor(context, "function(){}", TypeDescriptor.intersection(
                        TypeDescriptor.EXECUTABLE,
                        TypeDescriptor.OBJECT)));
        // proxy executable & object
        vals.add(createValueConstructor(
                        context,
                        "new Proxy(function() {}, {\n" +
                                        "    get : function(target, propKey) {\n" +
                                        "        if (propKey == Symbol.toPrimitive) {\n" +
                                        "            return function() {return '{?:42}'};\n" +
                                        "        } else if (propKey == Symbol.iterator) {\n" +
                                        "            return function() {return {next: function() {return {done:true};}};};\n" +
                                        "        } else {\n" +
                                        "            return 42;\n" +
                                        "        }\n" +
                                        "    },\n" +
                                        "    has : function(target, propKey) {return true;},\n" +
                                        "    apply: function(target, thisArg, argumentsList) {}\n" +
                                        "});",
                        TypeDescriptor.intersection(
                                        TypeDescriptor.EXECUTABLE,
                                        TypeDescriptor.OBJECT)));
        return Collections.unmodifiableList(vals);
    }

    @Override
    public Collection<? extends Snippet> createExpressions(final Context context) {
        final List<Snippet> ops = new ArrayList<>();
        final TypeDescriptor numericAndNull = TypeDescriptor.union(
                        TypeDescriptor.NUMBER,
                        TypeDescriptor.BOOLEAN,
                        TypeDescriptor.NULL);
        final TypeDescriptor noType = TypeDescriptor.intersection();
        final TypeDescriptor nonNumeric = TypeDescriptor.union(
                        TypeDescriptor.STRING,
                        TypeDescriptor.OBJECT,
                        TypeDescriptor.ARRAY,
                        TypeDescriptor.EXECUTABLE_ANY,
                        noType);
        // +
        ops.add(createBinaryOperator(context, "+", TypeDescriptor.NUMBER, numericAndNull, numericAndNull));
        ops.add(createBinaryOperator(context, "+", TypeDescriptor.STRING, nonNumeric, ANY));
        ops.add(createBinaryOperator(context, "+", TypeDescriptor.STRING, ANY, nonNumeric));
        // -
        ops.add(createBinaryOperator(context, "-", TypeDescriptor.NUMBER, ANY, ANY));
        // *
        ops.add(createBinaryOperator(context, "*", TypeDescriptor.NUMBER, ANY, ANY));
        // /
        ops.add(createBinaryOperator(context, "/", TypeDescriptor.NUMBER, ANY, ANY));
        // %
        ops.add(createBinaryOperator(context, "%", TypeDescriptor.NUMBER, ANY, ANY));
        // **
        ops.add(createBinaryOperator(context, "**", TypeDescriptor.NUMBER, ANY, ANY));
        // <
        ops.add(createBinaryOperator(context, "<", TypeDescriptor.BOOLEAN, ANY, ANY));
        // >
        ops.add(createBinaryOperator(context, ">", TypeDescriptor.BOOLEAN, ANY, ANY));
        // <=
        ops.add(createBinaryOperator(context, "<=", TypeDescriptor.BOOLEAN, ANY, ANY));
        // <=
        ops.add(createBinaryOperator(context, ">=", TypeDescriptor.BOOLEAN, ANY, ANY));
        // <<
        ops.add(createBinaryOperator(context, "<<", TypeDescriptor.NUMBER, ANY, ANY));
        // >>
        ops.add(createBinaryOperator(context, ">>", TypeDescriptor.NUMBER, ANY, ANY));
        // >>>
        ops.add(createBinaryOperator(context, ">>>", TypeDescriptor.NUMBER, ANY, ANY));
        // &
        ops.add(createBinaryOperator(context, "&", TypeDescriptor.NUMBER, ANY, ANY));
        // |
        ops.add(createBinaryOperator(context, "|", TypeDescriptor.NUMBER, ANY, ANY));
        // ^
        ops.add(createBinaryOperator(context, "^", TypeDescriptor.NUMBER, ANY, ANY));
        // &&
        ops.add(createBinaryOperator(context, "&&", ANY, ANY, ANY));
        // ||
        ops.add(createBinaryOperator(context, "||", ANY, ANY, ANY));
        // in
        // final Snippet in =
        createBinaryOperator(context, "in", TypeDescriptor.BOOLEAN,
                        ANY,
                        TypeDescriptor.union(TypeDescriptor.OBJECT, TypeDescriptor.ARRAY),
                        JavaScriptVerifier.nonEmptyArrayVerifier(null));
        // issue: GR-7222
        // ops.add(in);
        // +
        ops.add(createPrefixOperator(context, "+", TypeDescriptor.NUMBER, ANY));
        // -
        ops.add(createPrefixOperator(context, "-", TypeDescriptor.NUMBER, ANY));
        // ~
        ops.add(createPrefixOperator(context, "~", TypeDescriptor.NUMBER, ANY));
        // ++
        ops.add(createPrefixOperator(context, "++", TypeDescriptor.NUMBER, ANY));
        // --
        ops.add(createPrefixOperator(context, "--", TypeDescriptor.NUMBER, ANY));
        // ++
        ops.add(createPostfixOperator(context, "++", TypeDescriptor.NUMBER, ANY));
        // --
        ops.add(createPostfixOperator(context, "--", TypeDescriptor.NUMBER, ANY));
        return Collections.unmodifiableList(ops);
    }

    @Override
    public Collection<? extends Snippet> createStatements(final Context context) {
        final List<Snippet> res = new ArrayList<>();
        // if
        res.add(createStatement(context, "if", "if ({1}) {0}=true ; else {0}=false;", TypeDescriptor.BOOLEAN, ANY));
        // do
        res.add(createStatement(context, "do", "do break; while ({1});", TypeDescriptor.NULL, ANY));
        // while
        res.add(createStatement(context, "while", "while ({1}) break;", TypeDescriptor.NULL, ANY));
        // for
        res.add(createStatement(context, "for", "let guard = false; for (let i = {1}; {2} ; {3}) if (guard) break; else guard = true;",
                        TypeDescriptor.NULL,
                        ANY, ANY, ANY));
        // for in
        res.add(createStatement(context, "for-in", "for (let k in {1});",
                        TypeDescriptor.NULL,
                        JavaScriptVerifier.unsupportedDynamicObjectVerifier(null),
                        ANY));
        // for of
        final TypeDescriptor noType = TypeDescriptor.intersection();
        res.add(createStatement(context, "for-of", "for (let v of {1});",
                        TypeDescriptor.NULL,
                        JavaScriptVerifier.foreignOrHasIteratorVerifier(context, null),
                        TypeDescriptor.union(
                                        TypeDescriptor.STRING,
                                        TypeDescriptor.OBJECT,
                                        TypeDescriptor.ARRAY,
                                        TypeDescriptor.EXECUTABLE_ANY,
                                        TypeDescriptor.NULL,
                                        noType)));
        // with
        res.add(createStatement(context, "with", "with({1}) undefined",
                        TypeDescriptor.NULL,
                        JavaScriptVerifier.hasKeysVerifier(JavaScriptVerifier.unsupportedDynamicObjectVerifier(null)),
                        TypeDescriptor.ANY));

        // switch
        res.add(createStatement(context, "switch", "switch({1})'{' case true: break;'}'",
                        TypeDescriptor.NULL,
                        ANY));
        // throw
        res.add(createStatement(context, "throw", "try'{' throw {1};'}' catch(e)'{}'", TypeDescriptor.NULL, ANY));
        return Collections.unmodifiableList(res);
    }

    @Override
    public Collection<? extends Snippet> createScripts(final Context context) {
        final List<Snippet> res = new ArrayList<>();
        res.add(loadScript(
                        context,
                        "resources/arrayFactory.js",
                        TypeDescriptor.array(TypeDescriptor.OBJECT),
                        (snippetRun) -> {
                            ResultVerifier.getDefaultResultVerifier().accept(snippetRun);
                            final Value result = snippetRun.getResult();
                            Assert.assertEquals("Array size", 2, result.getArraySize());
                            Value p1 = result.getArrayElement(0);
                            Value p2 = result.getArrayElement(1);
                            Assert.assertEquals("res[0].x", 30, p1.getMember("x").asInt());
                            Assert.assertEquals("res[0].y", 15, p1.getMember("y").asInt());
                            Assert.assertEquals("res[1].x", 5, p2.getMember("x").asInt());
                            Assert.assertEquals("res[1].y", 7, p2.getMember("y").asInt());
                        }));
        res.add(loadScript(
                        context,
                        "resources/recursion.js",
                        TypeDescriptor.array(TypeDescriptor.NUMBER),
                        (snippetRun) -> {
                            ResultVerifier.getDefaultResultVerifier().accept(snippetRun);
                            final Value result = snippetRun.getResult();
                            Assert.assertEquals("Array size", 3, result.getArraySize());
                            Assert.assertEquals("res[0]", 3628800, result.getArrayElement(0).asInt());
                            Assert.assertEquals("res[1]", 55, result.getArrayElement(1).asInt());
                            Assert.assertEquals("res[2]", 125, result.getArrayElement(2).asInt());
                        }));
        return Collections.unmodifiableList(res);
    }

    @Override
    public Collection<? extends InlineSnippet> createInlineScripts(Context context) {
        final List<InlineSnippet> res = new ArrayList<>();
        res.add(createInlineSnippet(
                        context,
                        "resources/recursion.js",
                        23,
                        33,
                        "resources/recursion_inline1.js"));
        res.add(createInlineSnippet(
                        context,
                        "resources/recursion.js",
                        -1,
                        -1,
                        "resources/recursion_inline2.js"));
        // LanguageProviderSnippets#JsSnippets#createInlineScripts
        Snippet.Builder scriptBuilder = Snippet.newBuilder(
                        "factorial",
                        context.eval(
                                        "js",
                                        "(function (){\n" +
                                                        "  let factorial = function(n) {\n" +
                                                        "    let f = 1;\n" +
                                                        "    for (let i = 2; i <= n; i++) {\n" +
                                                        "      f *= i;\n" +
                                                        "    }\n" +
                                                        "  };\n" +
                                                        "  return factorial(10);\n" +
                                                        "})"),
                        TypeDescriptor.ANY);
        InlineSnippet.Builder builder = InlineSnippet.newBuilder(
                        scriptBuilder.build(),
                        "n * n").locationPredicate((SourceSection section) -> {
                            int line = section.getStartLine();
                            return 3 <= line && line <= 6;
                        });
        res.add(builder.build());
        builder = InlineSnippet.newBuilder(
                        scriptBuilder.build(),
                        "Math.sin(Math.PI)");
        res.add(builder.build());
        return Collections.unmodifiableList(res);
    }

    @Override
    public Collection<? extends Source> createInvalidSyntaxScripts(final Context context) {
        final List<Source> res = new ArrayList<>();
        // issue: GR-5786
        // res.add(createSource("resources/invalidSyntax01.js"));
        return Collections.unmodifiableList(res);
    }

    private static Snippet createValueConstructor(
                    final Context context,
                    final String value,
                    final TypeDescriptor type) {
        final Snippet.Builder opb = Snippet.newBuilder(
                        value,
                        eval(context, String.format(PATTERN_VALUE_FNC, value)),
                        type);
        return opb.build();
    }

    private static Snippet createPrefixOperator(
                    final Context context,
                    final String operator,
                    final TypeDescriptor type,
                    final TypeDescriptor rtype) {
        return createUnaryOperator(context, PATTERN_PREFIX_OP_FNC, operator, type, rtype);
    }

    private static Snippet createPostfixOperator(
                    final Context context,
                    final String operator,
                    final TypeDescriptor type,
                    final TypeDescriptor ltype) {
        return createUnaryOperator(context, PATTERN_POSTFIX_OP_FNC, operator, type, ltype);
    }

    private static Snippet createUnaryOperator(
                    final Context context,
                    final String template,
                    final String operator,
                    final TypeDescriptor type,
                    final TypeDescriptor paramType) {
        final Value fnc = eval(context, String.format(template, operator));
        final Snippet.Builder opb = Snippet.newBuilder(operator, fnc, type).parameterTypes(paramType);
        return opb.build();
    }

    private static Snippet createBinaryOperator(
                    final Context context,
                    final String operator,
                    final TypeDescriptor type,
                    final TypeDescriptor ltype,
                    final TypeDescriptor rtype) {
        return createBinaryOperator(context, operator, type, ltype, rtype, null);
    }

    private static Snippet createBinaryOperator(
                    final Context context,
                    final String operator,
                    final TypeDescriptor type,
                    final TypeDescriptor ltype,
                    final TypeDescriptor rtype,
                    final ResultVerifier verifier) {
        final Value fnc = eval(context, String.format(PATTERN_BIN_OP_FNC, operator));
        return Snippet.newBuilder(operator, fnc, type).parameterTypes(ltype, rtype).resultVerifier(verifier).build();
    }

    private static Snippet createStatement(
                    final Context context,
                    final String name,
                    final String expression,
                    final TypeDescriptor type,
                    final TypeDescriptor... paramTypes) {
        return createStatement(context, name, expression, type, null, paramTypes);
    }

    private static Snippet createStatement(
                    final Context context,
                    final String name,
                    final String expression,
                    final TypeDescriptor type,
                    final ResultVerifier resultVerifier,
                    final TypeDescriptor... paramTypes) {
        final String fncFormat = PATTERN_STATEMENT[paramTypes.length];
        final Object[] formalParams = new String[paramTypes.length + 1];
        formalParams[0] = "r";
        for (int i = 1; i < formalParams.length; i++) {
            formalParams[i] = "p" + i;
        }
        final String exprWithFormalParams = MessageFormat.format(expression, formalParams);
        final Value fnc = eval(context, String.format(fncFormat, exprWithFormalParams));
        final Snippet.Builder opb = Snippet.newBuilder(name, fnc, type).parameterTypes(paramTypes);
        if (resultVerifier != null) {
            opb.resultVerifier(resultVerifier);
        }
        return opb.build();
    }

    private static Snippet loadScript(
                    final Context context,
                    final String resourceName,
                    final TypeDescriptor type,
                    final ResultVerifier verifier) {
        final Source src = createSource(resourceName);
        return Snippet.newBuilder(src.getName(), context.eval(src), type).resultVerifier(verifier).build();
    }

    private static Source createSource(final String resourceName) {
        try {
            int slashIndex = resourceName.lastIndexOf('/');
            String scriptName = slashIndex >= 0 ? resourceName.substring(slashIndex + 1) : resourceName;
            final Reader in = new InputStreamReader(JavaScriptTCKLanguageProvider.class.getResourceAsStream(resourceName), "UTF-8");
            return Source.newBuilder(ID, in, scriptName).build();
        } catch (IOException ioe) {
            throw new AssertionError("IOException while creating a test script.", ioe);
        }
    }

    private static InlineSnippet createInlineSnippet(Context context, String sourceName, int l1, int l2, String snippetName) {
        Snippet script = loadScript(context, sourceName, TypeDescriptor.ANY, null);
        Predicate<SourceSection> locationPredicate;
        if (0 < l1 && l1 <= l2) {
            locationPredicate = (SourceSection ss) -> {
                return l1 <= ss.getStartLine() && ss.getEndLine() <= l2;
            };
        } else {
            locationPredicate = null;
        }
        InlineSnippet.Builder snippetBuilder = InlineSnippet.newBuilder(script, createSource(snippetName).getCharacters());
        if (locationPredicate != null) {
            snippetBuilder.locationPredicate(locationPredicate);
        }
        snippetBuilder.resultVerifier((ResultVerifier.SnippetRun snippetRun) -> {
            PolyglotException exception = snippetRun.getException();
            if (exception != null) {
                throw exception;
            }
            Value result = snippetRun.getResult();
            if (!result.isNumber()) {
                throw new AssertionError("Wrong value " + result.toString() + " from " + sourceName);
            }
        });
        return snippetBuilder.build();
    }

    private static Value eval(final Context context, final String statement) {
        return context.eval(ID, statement);
    }

    /**
     * Support for JavaScript {@link ResultVerifier}s. The JavaScript result verifiers can be
     * composed the last verifier is always the {@link ResultVerifier#getDefaultResultVerifier()}.
     */
    // Checkstyle: stop
    // Need to disable checkstyle reporting "Class JavaScriptVerifier should be declared as final"
    private static class JavaScriptVerifier implements ResultVerifier {
        // Checkstyle: resume
        private final ResultVerifier next;

        private JavaScriptVerifier(ResultVerifier next) {
            this.next = next == null ? ResultVerifier.getDefaultResultVerifier() : next;
        }

        @Override
        public void accept(SnippetRun snippetRun) throws PolyglotException {
            next.accept(snippetRun);
        }

        /**
         * Creates a {@link ResultVerifier} ignoring errors caused by empty arrays. Use this
         * verifier in case the operator accepts arrays but not an empty array.
         *
         * @param next the next {@link ResultVerifier} to be called, null for last one
         * @return the {@link ResultVerifier}
         */
        static ResultVerifier nonEmptyArrayVerifier(ResultVerifier next) {
            return new JavaScriptVerifier(next) {
                @Override
                public void accept(SnippetRun snippetRun) throws PolyglotException {
                    if (snippetRun.getException() != null) {
                        final Value objArg = snippetRun.getParameters().get(1);
                        if (objArg.hasArrayElements() && objArg.getArraySize() == 0) {
                            return;
                        }
                    }
                    super.accept(snippetRun);
                }
            };
        }

        /**
         * Creates a {@link ResultVerifier} ignoring errors caused unsupported foreign
         * DynamicObjects. Use this verifier in case the operator accepts foreign Objects but not
         * foreign DynamicObjects.
         *
         * @param next the next {@link ResultVerifier} to be called, null for last one
         * @return the {@link ResultVerifier}
         */
        static ResultVerifier unsupportedDynamicObjectVerifier(ResultVerifier next) {
            return new JavaScriptVerifier(next) {
                @Override
                public void accept(SnippetRun snippetRun) throws PolyglotException {
                    final PolyglotException exception = snippetRun.getException();
                    if (exception != null) {
                        if ("TypeError: Foreign DynamicObjects not supported".equals(exception.getMessage())) {
                            return;
                        }
                    }
                    super.accept(snippetRun);
                }
            };
        }

        /**
         * Creates a {@link ResultVerifier} ignoring errors caused by missing iterator method. Use
         * this verifier in case the operator accepts arbitrary foreign Objects for iteration but
         * requires iterator for JSObject.
         *
         * @param next the next {@link ResultVerifier} to be called, null for last one
         * @return the {@link ResultVerifier}
         */
        static ResultVerifier foreignOrHasIteratorVerifier(final Context context, ResultVerifier next) {
            return new JavaScriptVerifier(next) {
                @Override
                public void accept(SnippetRun snippetRun) throws PolyglotException {
                    if (snippetRun.getException() != null) {
                        final Value param = snippetRun.getParameters().get(0);
                        final Value paramMeta = param.getMetaObject();
                        final boolean jsObject = paramMeta.hasMember("type") &&
                                        "object".equals(param.getMetaObject().getMember("type").asString()) &&
                                        paramMeta.hasMember("className");
                        boolean hasIterator = false;
                        try {
                            hasIterator = !context.eval(ID, "(function(a) {return a[Symbol.iterator];})").execute(param).isNull();
                        } catch (Exception e) {
                        }
                        if (jsObject && !hasIterator) {
                            // Expected for not iterable
                            return;
                        }
                    }
                    super.accept(snippetRun);
                }
            };
        }

        /**
         * Creates a {@link ResultVerifier} ignoring errors caused by unsupported READ message.
         *
         * @param next the next {@link ResultVerifier} to be called, null for last one
         * @return the {@link ResultVerifier}
         */
        static ResultVerifier hasKeysVerifier(ResultVerifier next) {
            return new JavaScriptVerifier(next) {
                @Override
                public void accept(SnippetRun snippetRun) throws PolyglotException {
                    if (snippetRun.getException() != null) {
                        final Value arg = snippetRun.getParameters().get(0);
                        if (!arg.hasMembers()) {
                            return;
                        }
                    }
                    super.accept(snippetRun);
                }
            };
        }
    }
}
