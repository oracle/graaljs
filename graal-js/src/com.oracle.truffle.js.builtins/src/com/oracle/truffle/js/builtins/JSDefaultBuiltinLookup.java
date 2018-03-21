/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
        defineBuiltins(new ArrayBufferViewPrototypeBuiltins());
        defineBuiltins(new ArrayBufferViewFunctionBuiltins());

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
