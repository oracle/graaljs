/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import org.graalvm.options.OptionValues;

public interface ParserOptions {
    int getEcmaScriptVersion();

    ParserOptions putOptions(OptionValues options);
}
