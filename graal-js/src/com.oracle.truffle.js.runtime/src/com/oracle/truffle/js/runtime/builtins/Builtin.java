/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSAttributes;

public interface Builtin {
    String getName();

    Object getKey();

    int getLength();

    int getECMAScriptVersion();

    boolean isAnnexB();

    boolean isWritable();

    boolean isEnumerable();

    boolean isConfigurable();

    default int getAttributeFlags() {
        return JSAttributes.fromConfigurableEnumerableWritable(isConfigurable(), isEnumerable(), isWritable());
    }

    JSFunctionData createFunctionData(JSContext context);
}
