/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;

public class RegExpFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<RegExpFunctionBuiltins.RegExpFunction> {

    public static final RegExpFunctionBuiltins BUILTINS = new RegExpFunctionBuiltins();

    protected RegExpFunctionBuiltins() {
        super(JSRegExp.CLASS_NAME, RegExpFunction.class);
    }

    public enum RegExpFunction implements BuiltinEnum<RegExpFunction> {
        escape;

        @Override
        public int getLength() {
            return 1;
        }

        @Override
        public int getECMAScriptVersion() {
            return JSConfig.StagingECMAScriptVersion;
        }

        @Override
        public Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
            return RegExpFunctionBuiltinsFactory.JSRegExpEscapeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
    }

    abstract static class JSRegExpEscapeNode extends JSBuiltinNode {

        JSRegExpEscapeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        TruffleString escapeString(TruffleString input,
                        @Cached(parameters = "getContext().getStringLengthLimit()") StringBuilderProfile builderProfile,
                        @Cached TruffleString.ByteLengthOfCodePointNode lengthOfCodePointNode,
                        @Cached TruffleString.CodePointAtByteIndexNode codePointAtNode,
                        @Cached TruffleString.ReadCharUTF16Node readCharNode,
                        @Cached TruffleString.ByteIndexOfCodePointNode indexOfCodePointNode,
                        @Cached TruffleStringBuilder.AppendJavaStringUTF16Node appendJavaStringNode,
                        @Cached TruffleStringBuilder.AppendCodePointNode appendCodePointNode,
                        @Cached TruffleStringBuilder.ToStringNode toStringNode) {
            int length = Strings.length(input);
            int initialCapacity = Math.min(Math.max(length + 16, length + (length >> 1)), getContext().getStringLengthLimit());
            TruffleStringBuilderUTF16 escaped = builderProfile.newStringBuilder(initialCapacity);
            for (int index = 0; index < length; index += Strings.lengthOfCodePointAt(lengthOfCodePointNode, input, index)) {
                int cp = Strings.codePointAt(codePointAtNode, input, index);
                if (StringBuilderProfile.length(escaped) == 0 && ((cp >= '0' && cp <= '9') || (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z'))) {
                    builderProfile.append(appendJavaStringNode, escaped, "\\x");
                    builderProfile.append(appendJavaStringNode, escaped, Boundaries.integerToString(cp, 16));
                } else if (Strings.indexOf(indexOfCodePointNode, Strings.REGEXP_SYNTAX_CHARS_WITH_SOLIDUS, cp) >= 0) {
                    // SyntaxCharacter or U+002F (SOLIDUS)
                    builderProfile.append(appendCodePointNode, escaped, '\\');
                    builderProfile.append(appendCodePointNode, escaped, cp);
                } else if (cp == '\t') {
                    builderProfile.append(appendJavaStringNode, escaped, "\\t");
                } else if (cp == '\n') {
                    builderProfile.append(appendJavaStringNode, escaped, "\\n");
                } else if (cp == 0x0b) {
                    builderProfile.append(appendJavaStringNode, escaped, "\\v");
                } else if (cp == '\f') {
                    builderProfile.append(appendJavaStringNode, escaped, "\\f");
                } else if (cp == '\r') {
                    builderProfile.append(appendJavaStringNode, escaped, "\\r");
                } else if (Strings.indexOf(indexOfCodePointNode, Strings.REGEXP_OTHER_PUNCTUATORS, cp) >= 0 || JSRuntime.isWhiteSpaceOrLineTerminator(cp) || (cp >= 0xd800 && cp <= 0xdfff)) {
                    if (cp <= 0xff) {
                        builderProfile.append(appendJavaStringNode, escaped, "\\x");
                        leftPad(builderProfile, appendCodePointNode, appendJavaStringNode, escaped, Boundaries.integerToString(cp, 16), 2);
                    } else {
                        int numCodeUnits = Strings.lengthOfCodePointAt(lengthOfCodePointNode, input, index);
                        for (int i = index; i < index + numCodeUnits; i++) {
                            char cu = Strings.charAt(readCharNode, input, i);
                            builderProfile.append(appendJavaStringNode, escaped, "\\u");
                            leftPad(builderProfile, appendCodePointNode, appendJavaStringNode, escaped, Boundaries.integerToString(cu, 16), 4);
                        }
                    }
                } else {
                    builderProfile.append(appendCodePointNode, escaped, cp);
                }
            }
            return StringBuilderProfile.toString(toStringNode, escaped);
        }

        private static void leftPad(StringBuilderProfile builderProfile, TruffleStringBuilder.AppendCodePointNode appendCodePointNode,
                        TruffleStringBuilder.AppendJavaStringUTF16Node appendJavaStringNode,
                        TruffleStringBuilderUTF16 sb, String str, int padSize) {
            int padding = padSize - str.length();
            if (padding > 0) {
                builderProfile.repeat(appendCodePointNode, sb, '0', padding);
            }
            builderProfile.append(appendJavaStringNode, sb, str);
        }

        @Fallback
        TruffleString escapeNotString(Object input) {
            throw Errors.createTypeErrorNotAString(input);
        }
    }
}
