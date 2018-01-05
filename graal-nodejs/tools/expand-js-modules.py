#!/usr/bin/env python
#
# Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
# ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
