/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.interop.ForeignAccess;

public class JSInteropRuntime {
    private final ForeignAccess foreignAccess;
    private final ForeignAccess interopObjectForeignAccess;

    public JSInteropRuntime(ForeignAccess foreignAccess, ForeignAccess interopObjectForeignAccess) {
        this.foreignAccess = foreignAccess;
        this.interopObjectForeignAccess = interopObjectForeignAccess;
    }

    public ForeignAccess getForeignAccessFactory() {
        return foreignAccess;
    }

    public ForeignAccess getInteropObjectForeignAccess() {
        return interopObjectForeignAccess;
    }
}
