#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2023, Oracle and/or its affiliates. All rights reserved.
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

import mx, mx_benchmark, mx_graal_js, mx_sdk_vm
from mx_benchmark import GuestVm
from mx_benchmark import JMHDistBenchmarkSuite
from mx_benchmark import add_bm_suite

from os.path import join

_suite = mx.suite('graal-js')

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
        return mx_benchmark.java_vm_registry

    def with_host_vm(self, host_vm):
        return self.__class__(self.config_name(), self._options, host_vm)

    def run_aux_cache(self, cwd, args, runs, extra_args):
        assert not self._options
        cache_file = join(cwd, self.bmSuite.currently_running_benchmark() + '.img')
        for _ in range(runs):
            code, out, _ = self.host_vm().run_launcher('js', ['--experimental-options', '--engine.TraceCache', '--engine.Cache=' + cache_file] + extra_args + args, cwd)
            if code != 0:
                return code, out, {}
        return self.host_vm().run_launcher('js', ['--experimental-options', '--engine.CacheLoad=' + cache_file] + args, cwd)

    def run(self, cwd, args):
        if hasattr(self.host_vm(), 'run_launcher'):
            if self.config_name() == 'trace-cache':
                return self.run_aux_cache(cwd, args, 1, [])
            if self.config_name() == 'trace-cache-3-runs':
                return self.run_aux_cache(cwd, args, 3, [])
            if self.config_name() == 'trace-cache-10-runs':
                return self.run_aux_cache(cwd, args, 10, [])
            if self.config_name() == 'trace-cache-executed':
                return self.run_aux_cache(cwd, args, 1, ['--engine.CacheCompile=executed'])
            else:
                return self.host_vm().run_launcher('js', self._options + args, cwd)
        else:
            return self.host_vm().run(cwd, mx_graal_js.graaljs_cmd_line(self._options + args))

def register_js_vms():
    for config_name, options, priority in [
        ('default', [], 10),
        ('interpreter', ['--experimental-options', '--engine.Compilation=false'], 100),
        ('trace-cache', [], 110),
        ('trace-cache-3-runs', [], 120),
        ('trace-cache-10-runs', [], 130),
        ('trace-cache-executed', [], 140),
    ]:
        if mx.suite('js-benchmarks', fatalIfMissing=False):
            import mx_js_benchmarks
            mx_js_benchmarks.add_vm(GraalJsVm(config_name, options), _suite, priority)
        mx_benchmark.js_vm_registry.add_vm(GraalJsVm(config_name, options), _suite, priority)


class JMHDistGraalJsBenchmarkSuite(JMHDistBenchmarkSuite):
    def name(self):
        return "js-interop-jmh"

    def group(self):
        return "Graal"

    def subgroup(self):
        return "graal-js"

add_bm_suite(JMHDistGraalJsBenchmarkSuite())

# --env ce-js-bench
ce_components = ['cmp', 'gvm', 'icu4j', 'js', 'jsl', 'jss', 'lg', 'rgx', 'sdk', 'sdkc', 'sdkl', 'sdkni', 'svm', 'svmsl', 'svmt', 'tfl', 'tfla', 'tflc', 'tflm', 'tflsm']

# --env ee-js-bench
ee_components = ['cmp', 'cmpee', 'gvm', 'icu4j', 'js', 'jsl', 'jss', 'lg', 'rgx', 'sdk', 'sdkc', 'sdkl', 'sdkni', 'svm', 'svmee', 'svmeegc', 'svmsl', 'svmt', 'svmte', 'tfl', 'tfla', 'tflc', 'tfle', 'tflllm', 'tflm', 'tflsm']
# svmeegc is only available on linux
if not mx.is_linux():
    ee_components.remove('svmeegc')

mx_sdk_vm.register_vm_config('ce', ce_components, _suite)
mx_sdk_vm.register_vm_config('ee', ee_components, _suite)
