/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.parser;

import static com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE;
import static com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage.MODULE_SOURCE_NAME_SUFFIX;
import static com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage.SCRIPT_SOURCE_NAME_SUFFIX;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

public final class JSFileTypeDetector extends FileTypeDetector {
    @Override
    public String probeContentType(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(SCRIPT_SOURCE_NAME_SUFFIX) || fileName.endsWith(MODULE_SOURCE_NAME_SUFFIX)) {
            return APPLICATION_MIME_TYPE;
        }
        return null;
    }
}
