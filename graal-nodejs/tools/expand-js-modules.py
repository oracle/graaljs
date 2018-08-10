#!/usr/bin/env python
#
# Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# The Universal Permissive License (UPL), Version 1.0
#
# Subject to the condition set forth below, permission is hereby granted to any
# person obtaining a copy of this software, associated documentation and/or
# data (collectively the "Software"), free of charge and under any and all
# copyright rights in the Software, and any and all patent rights owned or
# freely licensable by each licensor hereunder covering either (i) the
# unmodified Software as contributed to or provided by such licensor, or (ii)
# the Larger Works (as defined below), to deal in both
#
# (a) the Software, and
#
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
# one is included with the Software each a "Larger Work" to which the Software
# is contributed by such licensors),
#
# without restriction, including without limitation the rights to copy, create
# derivative works of, display, perform, and distribute the Software and make,
# use, sell, offer for sale, import, export, have made, and have sold the
# Software and the Larger Work(s), and to sublicense the foregoing rights on
# either these or other terms.
#
# This license is subject to the following condition:
#
# The above copyright notice and either this complete permission notice or at a
# minimum a reference to the UPL must be included in all copies or substantial
# portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#
# This is a utility for expanding macros in source code of JavaScript modules
# and for wrapping the modules in the (function (exports, ...) {...}) header
# as is done by NativeModule in lib/internal/bootstrap_node.js.

import errno
import js2c
import os
import sys

def WrapModule(module_code):
    return "(function (exports, require, module, __filename, __dirname) { " + module_code + "\n});"

def EnsureDirExists(dirpath):
    try:
        os.makedirs(dirpath)
    except OSError as err:
        if err.errno != errno.EEXIST:
            raise

def ProcessModules(sources, outdir):
    macro_lines = []
    modules = []
    for s in sources:
        if s.endswith('macros.py'):
            macro_lines.extend(js2c.ReadLines(s))
        else:
            modules.append(s)

    (consts, macros) = js2c.ReadMacros(macro_lines)

    for m in modules:
        contents = js2c.ReadFile(m)
        contents = js2c.ExpandConstants(contents, consts)
        contents = js2c.ExpandMacros(contents, macros)
        # bootstrap_node.js is the module that implements the module system,
        # therefore bootstrap_node.js does not use it and must not be wrapped.
        if not m.endswith('internal/bootstrap_node.js'):
            contents = WrapModule(contents)

        outpath = os.path.join(outdir, m)
        EnsureDirExists(os.path.split(outpath)[0])
        with open(outpath, 'w') as outfile:
            outfile.write(contents)

def main():
    outdir = sys.argv[1]
    source_files = sys.argv[2:]
    ProcessModules(source_files, outdir)

if __name__ == "__main__":
    main()
