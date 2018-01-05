/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.source.Source;

public interface JSModuleLoader {
    JSModuleRecord resolveImportedModule(JSModuleRecord referencingModule, String specifier);

    JSModuleRecord loadModule(Source moduleSource);
}
