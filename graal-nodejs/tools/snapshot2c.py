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
# This is a utility for encoding the binary snapshot of Node.js core modules
# into a C++ header file. The resulting snapshots are thus embedded in the resulting
# Node.js binary.

import os
import re
import sys


def ReadBinaryFile(filename):
  with open(filename, "rb") as file:
    contents = file.read()
  return contents


HEADER_TEMPLATE = """\
#ifndef node_snapshots_h
#define node_snapshots_h

#include <string>
#include <map>

namespace node_snapshots {

%(data_lines)s\

  struct byte_buffer_t {
    unsigned char* ptr;
    size_t len;
  };

  std::map<std::string, byte_buffer_t> compute_snapshots_map() {
    std::map<std::string, byte_buffer_t> snapshots_map;

%(record_lines)s

    return snapshots_map;
  }

  static const std::map<std::string, byte_buffer_t> snapshots = compute_snapshots_map();

}
#endif
"""


SNAPSHOT_DATA_DECLARATION = """\
  unsigned char %(escaped_id)s_snapshot[] = { %(data)s };
"""

SNAPSHOT_MAP_ENTRY = """\
    snapshots_map.insert({ "%(id)s", { %(escaped_id)s_snapshot, sizeof(%(escaped_id)s_snapshot) } });"""


def JS2C(modules, target):
  # Build source code lines
  data_lines = []
  record_lines = []

  for m in modules:
    contents = ReadBinaryFile(m)

    data = ','.join(str(c) for c in contents)

    # On Windows, "./foo.bar" in the .gyp file is passed as "foo.bar"
    # so don't assume there is always a slash in the file path.
    if '/' in m or '\\' in m:
      id = '/'.join(re.split('/|\\\\', m)[1:])
    else:
      id = m

    if id.endswith('.bin'):
      id = id[:-4]

    escaped_id = id.replace('.', '_').replace('-', '_').replace('/', '_')

    data_lines.append(SNAPSHOT_DATA_DECLARATION % {
      'escaped_id': escaped_id,
      'data': data
    })
    record_lines.append(SNAPSHOT_MAP_ENTRY % {
      'id': id,
      'escaped_id': escaped_id
    })

  # Emit result
  if os.path.exists(target):
    with open(target, "r") as t:
      old_content = t.read()
  else:
    old_content = ''

  new_content = HEADER_TEMPLATE % {
    'data_lines': "\n".join(data_lines),
    'record_lines': "\n".join(record_lines)
  }

  if new_content != old_content:
    print('creating %s' % target)
    with open(target, "w") as output:
      output.write(new_content)
  else:
    print('%s is already up-to-date' % target)


def main():
  target = sys.argv[1]
  snapshot_files = sys.argv[2:]
  JS2C(snapshot_files, target)


if __name__ == "__main__":
  main()
