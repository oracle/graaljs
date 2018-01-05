/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
