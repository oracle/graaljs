/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
function Process() {}

Process.prototype.spawn = function(options) {
	var N = Java.type("com.oracle.truffle.trufflenode.threading.GraalJSInstanceRunner");
	// Open an IPC server on *this* process
	var uid = N.newLoopId.incrementAndGet();
	options.ipc.initServerIPC(uid);

	var stringArray = Java.type("java.lang.String[]");
	var javaArgs = new stringArray(options.args.length-1);

	for (var a=0; a<options.args.length-1; a++) {
		javaArgs[a] = options.args[a+1];
	}

	var newEnv = {};
	Object.keys(process.env).forEach(function(envKey) {
		var val = process.env[envKey];
		if (typeof val === 'string') {
			newEnv[envKey] = val;
		}
	});
	options.envPairs.forEach(function(envVar) {
		var idx = envVar.indexOf("=");
		if (idx != -1) {
			var key = envVar.substring(0,idx);
			newEnv[key] = envVar.substring(idx+1, envVar.length);
		}
	});
	newEnv.NODE_UNIQUE_ID = uid;
	newEnv.NODE_CHANNEL_FD = uid;

	// Start a worker event loop (will connect to our IPC pipe)
	N.startInNewThread(javaArgs, newEnv);
	// All good
	return 0;
}

Process.prototype.send = function() {
	throw "Not implemented";
}

Process.prototype.kill = function() {
	throw "Not implemented";
}

module.exports = {
	Process : Process
}
