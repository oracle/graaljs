#!/usr/bin/env python
#
# Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

import codecs
import errno
import os
import sys

from os.path import join

def WrapModule(module_path, module_code):
    delimiter = u'\u237f' # "random" character (vertical line with middle dot)
    wrapped_module_code = delimiter + module_code + delimiter
    # see LookupAndCompile() in node_builtins.cc
    if module_path == join('lib', 'internal', 'bootstrap', 'realm.js'):
        result = "(function (process, getLinkedBinding, getInternalBinding, primordials) {" + wrapped_module_code + "\n});"
    elif module_path.startswith(join('lib', 'internal', 'per_context')):
        result = "(function (exports, primordials) {" + wrapped_module_code + "\n});"
    elif module_path.startswith(join('lib', 'internal', 'main')) or module_path.startswith(join('lib', 'internal', 'bootstrap')):
        result = "(function (process, require, internalBinding, primordials) {" + wrapped_module_code + "\n});"
    else:
        result = "(function (exports, require, module, process, internalBinding, primordials) {" + wrapped_module_code + "\n});"
    return delimiter + result;

def EnsureDirExists(dirpath):
    try:
        os.makedirs(dirpath)
    except OSError as err:
        if err.errno != errno.EEXIST:
            raise

def ProcessModules(sources, outdir):
    macro_files = []
    modules = []
    for s in sources:
        if s.endswith('macros.py'):
            macro_files.append(s)
        else:
            modules.append(s)

    for m in modules:
        contents = ReadFile(m)
        contents = WrapModule(m, contents)

        outpath = os.path.join(outdir, m)
        EnsureDirExists(os.path.split(outpath)[0])
        with codecs.open(outpath, 'w', 'utf-8') as outfile:
            outfile.write(contents)

def ReadFile(filename):
  with codecs.open(filename, "r", "utf-8") as f:
    lines = f.read()
    return lines

def main():
    outdir = sys.argv[1]
    source_files = sys.argv[2:]
    ProcessModules(source_files, outdir)

if __name__ == "__main__":
    main()
