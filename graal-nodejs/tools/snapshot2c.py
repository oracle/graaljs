#!/usr/bin/env python
#
# Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
# ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
#
# This is a utility for encoding the binary snapshot of Node.js core modules
# into a C++ header file. The resulting snapshots are thus embedded in the resulting
# Node.js binary.

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

    data = ','.join(str(ord(c)) for c in contents)

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
  with open(target, "w") as output:
    output.write(HEADER_TEMPLATE % {
      'data_lines': "\n".join(data_lines),
      'record_lines': "\n".join(record_lines)
    })


def main():
  target = sys.argv[1]
  snapshot_files = sys.argv[2:]
  JS2C(snapshot_files, target)


if __name__ == "__main__":
  main()
