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
    mx_js_benchmarks.add_vm(GraalJsVm('no-comp-oops', ['-XX:-UseCompressedOops']), _suite)
    mx_js_benchmarks.add_vm(GraalJsVm('no-splitting', ['-Dgraal.TruffleSplitting=false']), _suite)
    mx_js_benchmarks.add_vm(GraalJsVm('limit-truffle-inlining', ['-Dgraal.TruffleMaximumRecursiveInlining=2']), _suite)
    mx_js_benchmarks.add_vm(GraalJsVm('no-splitting-limit-truffle-inlining', ['-Dgraal.TruffleSplitting=false', '-Dgraal.TruffleMaximumRecursiveInlining=2']), _suite)
except ImportError:
    pass
