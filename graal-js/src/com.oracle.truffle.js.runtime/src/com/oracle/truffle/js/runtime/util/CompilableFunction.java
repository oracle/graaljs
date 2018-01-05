/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.util;

import java.util.function.Function;

/**
 * A functional interface, implementations of which are safe for partial evaluation.
 *
 * @see Function
 */
@FunctionalInterface
public interface CompilableFunction<T, R> extends Function<T, R> {
}
