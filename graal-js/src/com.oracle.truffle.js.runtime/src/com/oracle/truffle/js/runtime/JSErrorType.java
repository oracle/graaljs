/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

public enum JSErrorType {
    Error,

    /**
     * Currently not in use, only there for compatibility with previous versions of the
     * specification ECMA262[15.11.6.1].
     */
    EvalError,

    /**
     * Indicates a numeric value has exceeded the allowable range ECMA262[15.11.6.2].
     */
    RangeError,

    /**
     * Indicate that an invalid reference value has been detected ECMA262[15.11.6.3].
     */
    ReferenceError,

    /**
     * Indicates that a parsing error has occurred ECMA262[15.11.6.4].
     */
    SyntaxError,

    /**
     * Indicates the actual type of an operand is different than the expected type
     * ECMA262[15.11.6.5].
     */
    TypeError,

    /**
     * Indicates that one of the global URI handling functions was used in a way that is
     * incompatible with its definition ECMA262[15.11.6.6].
     */
    URIError
}
