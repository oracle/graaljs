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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.js.builtins.PolyglotBuiltins.PolyglotInternalBuiltins;
import com.oracle.truffle.js.builtins.math.MathBuiltins;
import com.oracle.truffle.js.builtins.simd.SIMDBoolFunctionBuiltins;
import com.oracle.truffle.js.builtins.simd.SIMDBuiltins;
import com.oracle.truffle.js.builtins.simd.SIMDFloatFunctionBuiltins;
import com.oracle.truffle.js.builtins.simd.SIMDIntFunctionBuiltins;
import com.oracle.truffle.js.builtins.simd.SIMDSmallIntFunctionBuiltins;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins;
import com.oracle.truffle.js.builtins.simd.SIMDTypePrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSFunctionLookup;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.js.runtime.builtins.SIMDType;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDBool16x8;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDBool32x4;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDBool8x16;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDFloat32x4;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDInt16x8;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDInt32x4;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDInt8x16;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDTypeFactory;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDUint16x8;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDUint32x4;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDUint8x16;

/**
 * Central registry of JavaScript built-in functions.
 *
 * @see JSFunctionLookup
 * @see JSContext
 */
public class JSDefaultBuiltinLookup extends JSBuiltinLookup {

    public JSDefaultBuiltinLookup() {
        long time = JSTruffleOptions.ProfileTime ? System.nanoTime() : 0L;
        defineBuiltins(new MathBuiltins());

        defineBuiltins(new StringPrototypeBuiltins());
        defineBuiltins(new StringFunctionBuiltins());

        defineBuiltins(new ArrayPrototypeBuiltins());
        defineBuiltins(new ArrayFunctionBuiltins());

        defineBuiltins(new ObjectPrototypeBuiltins());
        defineBuiltins(new ObjectFunctionBuiltins());

        defineBuiltins(new NumberPrototypeBuiltins());
        defineBuiltins(new NumberFunctionBuiltins());

        defineBuiltins(new BooleanPrototypeBuiltins());

        defineBuiltins(new FunctionPrototypeBuiltins());

        defineBuiltins(new DatePrototypeBuiltins());
        defineBuiltins(new DateFunctionBuiltins());

        defineBuiltins(new RegExpPrototypeBuiltins());

        defineBuiltins(new ErrorPrototypeBuiltins());
        defineBuiltins(new ErrorFunctionBuiltins());
        defineBuiltins(new CallSitePrototypeBuiltins());

        defineBuiltins(new JSONBuiltins());

        defineBuiltins(JSGlobalObject.CLASS_NAME, new GlobalBuiltins());

        defineBuiltins(JSConstructor.BUILTINS, new ConstructorBuiltins());

        defineBuiltins(new ArrayBufferPrototypeBuiltins());
        defineBuiltins(new ArrayBufferFunctionBuiltins());
        defineBuiltins(new TypedArrayPrototypeBuiltins());
        defineBuiltins(new TypedArrayFunctionBuiltins());
        defineBuiltins(new DataViewPrototypeBuiltins());

        defineBuiltins(new EnumerateIteratorPrototypeBuiltins());
        if (JSTruffleOptions.MaxECMAScriptVersion >= 6) {
            defineBuiltins(new MapPrototypeBuiltins());
            defineBuiltins(new SetPrototypeBuiltins());
            defineBuiltins(new WeakMapPrototypeBuiltins());
            defineBuiltins(new WeakSetPrototypeBuiltins());
            defineBuiltins(new SymbolFunctionBuiltins());
            defineBuiltins(new SymbolPrototypeBuiltins());
            defineBuiltins(new GeneratorPrototypeBuiltins());
            defineBuiltins(new ReflectBuiltins());
            defineBuiltins(new ProxyFunctionBuiltins());
            defineBuiltins(new ArrayIteratorPrototypeBuiltins());
            defineBuiltins(new SetIteratorPrototypeBuiltins());
            defineBuiltins(new MapIteratorPrototypeBuiltins());
            defineBuiltins(new StringIteratorPrototypeBuiltins());
            defineBuiltins(new PromisePrototypeBuiltins());
            defineBuiltins(new PromiseFunctionBuiltins());
        }

        if (JSTruffleOptions.MaxECMAScriptVersion >= 8) {
            defineBuiltins(new SharedArrayBufferPrototypeBuiltins());
            defineBuiltins(new SharedArrayBufferFunctionBuiltins());
            defineBuiltins(new AtomicsBuiltins());
        }
        if (JSTruffleOptions.MaxECMAScriptVersion >= 9) {
            defineBuiltins(new AsyncFromSyncIteratorPrototypeBuiltins());
            defineBuiltins(new AsyncGeneratorPrototypeBuiltins());
        }

        if (JSTruffleOptions.Test262Mode) {
            defineBuiltins(new Test262Builtins());
        }
        if (JSTruffleOptions.TestV8Mode) {
            defineBuiltins(new TestV8Builtins());
        }
        if (JSTruffleOptions.TestNashornMode) {
            defineBuiltins(new TestNashornBuiltins());
        }
        if (JSTruffleOptions.TruffleInterop) {
            defineBuiltins(new PolyglotBuiltins());
            defineBuiltins(new PolyglotInternalBuiltins());
        }
        defineBuiltins(new DebugBuiltins());
        if (JSTruffleOptions.Extensions) {
            defineBuiltins(new PerformanceBuiltins());
        }
        defineBuiltins(new RealmFunctionBuiltins());
        if (!JSTruffleOptions.SubstrateVM) {
            defineJavaInterop();
        }
        if (JSTruffleOptions.ProfileTime) {
            System.out.println("JSDefaultBuiltinLookup: " + (System.nanoTime() - time) / 1000000);
        }
        if (JSTruffleOptions.SIMDJS) {
            defineBuiltins(new SIMDBuiltins());
            defineBuiltins(new SIMDTypePrototypeBuiltins());

            for (SIMDTypeFactory<? extends SIMDType> factory : SIMDType.FACTORIES) {
                SIMDType t = factory.createSimdType();
                if (t.getClass().equals(SIMDFloat32x4.class)) {
                    defineBuiltins(new SIMDFloatFunctionBuiltins(factory.getName(), t));
                } else if (t.getClass().equals(SIMDBool32x4.class) || t.getClass().equals(SIMDBool16x8.class) || t.getClass().equals(SIMDBool8x16.class)) {
                    defineBuiltins(new SIMDBoolFunctionBuiltins(factory.getName(), t));
                } else if (t.getClass().equals(SIMDInt16x8.class) || t.getClass().equals(SIMDInt8x16.class) || t.getClass().equals(SIMDUint16x8.class) || t.getClass().equals(SIMDUint8x16.class)) {
                    defineBuiltins(new SIMDSmallIntFunctionBuiltins(factory.getName(), t));
                } else if (t.getClass().equals(SIMDInt32x4.class) || t.getClass().equals(SIMDUint32x4.class)) {
                    defineBuiltins(new SIMDIntFunctionBuiltins(factory.getName(), t));
                } else {
                    defineBuiltins(new SIMDTypeFunctionBuiltins(factory.getName(), t));
                }
            }
        }

        defineBuiltins(new CollatorPrototypeBuiltins());
        defineBuiltins(new CollatorFunctionBuiltins());

        defineBuiltins(new NumberFormatPrototypeBuiltins());
        defineBuiltins(new NumberFormatFunctionBuiltins());
        defineBuiltins(new DateTimeFormatPrototypeBuiltins());
        defineBuiltins(new DateTimeFormatFunctionBuiltins());
        defineBuiltins(new PluralRulesPrototypeBuiltins());
        defineBuiltins(new PluralRulesFunctionBuiltins());
        defineBuiltins(new IntlBuiltins());
    }

    /* In a separate method for Substrate VM support. */
    private void defineJavaInterop() {
        defineBuiltins(new JavaBuiltins());
        defineBuiltins(new JavaInteropWorkerPrototypeBuiltins());
    }
}
