#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
# ----------------------------------------------------------------------------------------------------

import mx, mx_graal_js
from mx_benchmark import GuestVm, java_vm_registry

class GraalJsVm(GuestVm):
    def __init__(self, config_name, options, host_vm=None):
        super(GraalJsVm, self).__init__(host_vm=host_vm)
        self._config_name = config_name
        self._options = options

    def name(self):
        return 'graal-js'

    def config_name(self):
        return self._config_name

    def hosting_registry(self):
        return java_vm_registry

    def with_host_vm(self, host_vm):
        return self.__class__(self.config_name(), self._options, host_vm)

    def run(self, cwd, args):
        args += self._options
        code, out, dims = self.host_vm().run(cwd, mx_graal_js.graaljs_cmd_line(args))
        dims.update({'config.name': self.config_name()})
        return code, out, dims

try:
    import mx_js_benchmarks
    _suite = mx.suite('graal-js')
    mx_js_benchmarks.add_vm(GraalJsVm('default', []), _suite, 10)
except ImportError:
    pass
