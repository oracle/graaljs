/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//according to test262/INTERPRET.md
// see also DebugJSAgent.java

var $262 = {};

$262.createRealm = function createRealm() {
	var newGlobalObj = Test262.createRealm();
	newGlobalObj.$262 = {};
	newGlobalObj.$262.detachArrayBuffer = $262.detachArrayBuffer;
	newGlobalObj.$262.evalScript = $262.evalScript;
	newGlobalObj.$262.global = newGlobalObj;
	return newGlobalObj.$262;
};

$262.detachArrayBuffer = function detachArrayBuffer(buffer) {
	Test262.typedArrayDetachBuffer(buffer);
}

$262.evalScript = function evalScript(string) {
	return eval(string);
}

$262.global = this;

$262.agent = {
	start: function agentStart(string) {
		// Will create $262.agent for the new agent.
		return Test262.agentStart(string);
	},

	broadcast: function agentBroadcast(sab, num) {
		return Test262.agentBroadcast(sab, num);
	},

	getReport: function agentGetReport(string) {
		return Test262.agentGetReport(string);
	},

	sleep: function agentSleep(time) {
		return Test262.agentSleep(time);
	}
};
