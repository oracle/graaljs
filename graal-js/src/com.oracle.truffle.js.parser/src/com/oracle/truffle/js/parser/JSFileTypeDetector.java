/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

public final class JSFileTypeDetector extends FileTypeDetector {
    @Override
    public String probeContentType(Path path) throws IOException {
        if (path.getFileName().toString().endsWith(".js")) {
            return "application/javascript";
        }
        return null;
    }
}
