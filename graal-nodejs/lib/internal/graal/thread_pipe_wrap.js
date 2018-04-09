/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
var PipeBinding = process.binding('pipe_wrap').Pipe
var PipeWrap = process.binding('pipe_wrap').PipeConnectWrap;
var WriteWrap = process.binding('stream_wrap').WriteWrap;

var N = Java.type("com.oracle.truffle.trufflenode.threading.GraalJSInstanceRunner")

const sock = "/tmp/nodesock"
const util = require('util');


function BasePipeIPC() {
	this._pipe = new PipeBinding(true);
	this._wq = [];
	this._connected = false;
}

BasePipeIPC.prototype.ref = function() {
	// nop
}

BasePipeIPC.prototype.unref = function() {
	// nop
}

BasePipeIPC.prototype.close = function() {
	// nop
}

BasePipeIPC.prototype.bind = function(addr) {
	// nop
}

BasePipeIPC.prototype.listen = function() {
	// nop
}

BasePipeIPC.prototype.readStart = function() {
	// nop
}

BasePipeIPC.prototype.writeUtf8String = function(a,b,c) {
	if (!this._connected) {
		this._wq.push([a,b,c]);
		return 0;
	} else {
		return this._doWrite(a,b,c);
	}
}

BasePipeIPC.prototype.flushWrites = function() {
	var w = this._wq.shift();
	while (w != undefined) {
		this._doWrite(w[0],w[1],w[2]);
		w = this._wq.shift();
	}
}

function ServerPipe() {
	BasePipeIPC.call(this);
}

util.inherits(ServerPipe, BasePipeIPC);

ServerPipe.prototype.initServerIPC = function(fd) {
	this.completed = true;

	var realPipe = this;

	this._pipe.onconnection = function(err, client) {
		client.onread = function(nread, buff, handle) {
			realPipe.onread(nread, buff, handle);
		}
		realPipe._client = client;
		realPipe._connected = true;
		realPipe.flushWrites();
		realPipe._client.readStart();
	}
	// TODO(db) find a cleaner way ro rm the virtual socket
	var fs = require('fs');
	var filePath = sock+fd;
	try {
		fs.unlinkSync(filePath);
	} catch (e) {}

	this._pipe.buffering = false;
	this._pipe.bind(filePath);
	this._pipe.listen(1);
}

ServerPipe.prototype._doWrite = function(a,b,c) {
	return this._client.writeUtf8String(a,b,c);
}

function ClientPipe() {
	BasePipeIPC.call(this);
}

util.inherits(ClientPipe, BasePipeIPC);

ClientPipe.prototype._doWrite = function(a,b,c) {
	return this._pipe.writeUtf8String(a,b,c);
}

ClientPipe.prototype.open = function(fd) {
	var req = new PipeWrap();
	var wreq = new WriteWrap();
	wreq.async = false;

	this._pipe.buffering = false;
	req.buffering = false;

	var self = this;
	var realPipe = this._pipe;

	// dual of 'onconnection'
	req.oncomplete = function(status,req) {
		realPipe.onread = function(nread, buff, handle) {
			self.onread(nread, buff, handle);
		}
		self._connected = true;
		self._client = req;
		self.flushWrites();
		self._client.readStart();
	}

	this._pipe.connect(req, sock+fd);
	this._pipe.readStart();
}

function Pipe(ipc, server) {
	if (ipc === true) {
		if (server === true) {
			return new ServerPipe();
		} else {
			return new ClientPipe();
		}
	} else {
		return new PipeBinding(ipc);
	}
}

module.exports = {
	Pipe : Pipe
}
