/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

/**
 * Fake foreign object used for testing.
 */
public class ForeignTestObject implements TruffleObject {

    @Override
    public ForeignAccess getForeignAccess() {
        return ForeignTestObjectMessageResolutionForeign.ACCESS;
    }
}
