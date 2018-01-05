/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.interop.ForeignAccess;

public class JSInteropRuntime {
    private final ForeignAccess foreignAccessFactory;
    private final TruffleGlobalScopeImpl multilanguageGlobal;

    public JSInteropRuntime(ForeignAccess foreignAccess, TruffleGlobalScopeImpl global) {
        this.foreignAccessFactory = foreignAccess;
        this.multilanguageGlobal = global;
    }

    public ForeignAccess getForeignAccessFactory() {
        return foreignAccessFactory;
    }

    public TruffleGlobalScopeImpl getMultilanguageGlobal() {
        return multilanguageGlobal;
    }
}
