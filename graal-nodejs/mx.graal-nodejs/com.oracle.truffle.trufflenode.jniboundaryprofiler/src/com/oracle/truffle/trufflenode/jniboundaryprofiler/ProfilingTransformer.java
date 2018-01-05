/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.jniboundaryprofiler;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class ProfilingTransformer implements ClassFileTransformer {

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] instrumentedClass = classfileBuffer;
        try {
            // Instrument any call to a JSFunction object performed via NativeAccess
            instrumentedClass = JSFunctionCallsInstrumenter.maybeInstrumentClass(className, classfileBuffer);
            // Instrument any call to a Java method performed via GraalJSAccess
            instrumentedClass = JavaCallsInstrumenter.maybeInstrumentClass(className, instrumentedClass);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return instrumentedClass;
    }

}