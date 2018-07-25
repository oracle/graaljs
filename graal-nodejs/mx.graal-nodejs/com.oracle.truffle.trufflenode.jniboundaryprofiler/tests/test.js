function purejs(from) {
	var to = {
		bar: 0
	};
	for (var i = 0; i < from.length; i++) {
		to.bar += from[i];
	}
	return to;
}

function micro(fun, lbl) {
	console.time(lbl);
	var output = fun(input);
	console.timeEnd(lbl);
	return output;
}

var CALLS = 5;
var ITERATIONS = 1000000;
var input = [];

for (var i = 0; i < ITERATIONS; i++) {
	input.push(i + 1);
}

var jsResult = 0;
for (var i = 0; i < CALLS; i++) {
	jsResult += micro(purejs, 'js').bar;
}

const addon = require('./build/Release/nodeaddon');
var cppResult = 0;
for (var i = 0; i < CALLS; i++) {
	cppResult += micro(addon.execute, 'native').bar;
}

if (typeof Packages === 'undefined') {
	throw "Must run with Java-JS interop enabled!";
}

const profiler = Java.type("com.oracle.truffle.trufflenode.jniboundaryprofiler.ProfilingAgent");
if (typeof profiler.dumpCounters !== 'function') {
	throw "Profiler not enabled!";
}

const assert = require('assert');
assert.equal(cppResult, jsResult, "Native module had a different result than JS one");

const nativeLabel = "executeFunction1: execute";
assert.equal(CALLS, +profiler.getNativeCalls(nativeLabel));
assert.equal(CALLS, +profiler.getJniCalls(nativeLabel, "[com/oracle/truffle/trufflenode/GraalJSAccess] arrayLength"));
assert.equal(CALLS, +profiler.getJniCalls(nativeLabel, "[com/oracle/truffle/trufflenode/GraalJSAccess] objectNew"));
assert.equal(CALLS * ITERATIONS, +profiler.getJniCalls(nativeLabel, "[com/oracle/truffle/trufflenode/GraalJSAccess] objectGetIndex"));
assert.equal(CALLS * ITERATIONS, +profiler.getJniCalls(nativeLabel, "[com/oracle/truffle/trufflenode/GraalJSAccess] objectGet"));
assert.equal((CALLS * ITERATIONS) + CALLS, +profiler.getJniCalls(nativeLabel, "[com/oracle/truffle/trufflenode/GraalJSAccess] objectSet"));

console.log("Test OK");
