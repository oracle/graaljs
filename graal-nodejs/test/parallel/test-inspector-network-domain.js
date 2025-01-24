// Flags: --inspect=0 --experimental-network-inspection
'use strict';
const common = require('../common');

common.skipIfInspectorDisabled();

const assert = require('node:assert');
const { addresses } = require('../common/internet');
const fixtures = require('../common/fixtures');
const http = require('node:http');
const https = require('node:https');
const inspector = require('node:inspector/promises');

const session = new inspector.Session();
session.connect();

const requestHeaders = {
  'accept-language': 'en-US',
  'Cookie': ['k1=v1', 'k2=v2'],
  'age': 1000,
  'x-header1': ['value1', 'value2']
};

const setResponseHeaders = (res) => {
  res.setHeader('server', 'node');
  res.setHeader('etag', 12345);
  res.setHeader('Set-Cookie', ['key1=value1', 'key2=value2']);
  res.setHeader('x-header2', ['value1', 'value2']);
};

const httpServer = http.createServer((req, res) => {
  const path = req.url;
  switch (path) {
    case '/hello-world':
      setResponseHeaders(res);
      res.writeHead(200);
      res.end('hello world\n');
      break;
    default:
      assert(false, `Unexpected path: ${path}`);
  }
});

const httpsServer = https.createServer({
  key: fixtures.readKey('agent1-key.pem'),
  cert: fixtures.readKey('agent1-cert.pem')
}, (req, res) => {
  const path = req.url;
  switch (path) {
    case '/hello-world':
      setResponseHeaders(res);
      res.writeHead(200);
      res.end('hello world\n');
      break;
    default:
      assert(false, `Unexpected path: ${path}`);
  }
});

const terminate = () => {
  session.disconnect();
  httpServer.close();
  httpsServer.close();
  inspector.close();
};

const testHttpGet = () => new Promise((resolve, reject) => {
  session.on('Network.requestWillBeSent', common.mustCall(({ params }) => {
    assert.ok(params.requestId.startsWith('node-network-event-'));
    assert.strictEqual(params.request.url, 'http://127.0.0.1/hello-world');
    assert.strictEqual(params.request.method, 'GET');
    assert.strictEqual(typeof params.request.headers, 'object');
    assert.strictEqual(params.request.headers['accept-language'], 'en-US');
    assert.strictEqual(params.request.headers.cookie, 'k1=v1; k2=v2');
    assert.strictEqual(params.request.headers.age, '1000');
    assert.strictEqual(params.request.headers['x-header1'], 'value1, value2');
    assert.strictEqual(typeof params.timestamp, 'number');
    assert.strictEqual(typeof params.wallTime, 'number');
  }));
  session.on('Network.responseReceived', common.mustCall(({ params }) => {
    assert.ok(params.requestId.startsWith('node-network-event-'));
    assert.strictEqual(typeof params.timestamp, 'number');
    assert.strictEqual(params.type, 'Other');
    assert.strictEqual(params.response.status, 200);
    assert.strictEqual(params.response.statusText, 'OK');
    assert.strictEqual(params.response.url, 'http://127.0.0.1/hello-world');
    assert.strictEqual(typeof params.response.headers, 'object');
    assert.strictEqual(params.response.headers.server, 'node');
    assert.strictEqual(params.response.headers.etag, '12345');
    assert.strictEqual(params.response.headers['set-cookie'], 'key1=value1\nkey2=value2');
    assert.strictEqual(params.response.headers['x-header2'], 'value1, value2');
  }));
  session.on('Network.loadingFinished', common.mustCall(({ params }) => {
    assert.ok(params.requestId.startsWith('node-network-event-'));
    assert.strictEqual(typeof params.timestamp, 'number');
    resolve();
  }));

  http.get({
    host: '127.0.0.1',
    port: httpServer.address().port,
    path: '/hello-world',
    headers: requestHeaders
  }, common.mustCall());
});

const testHttpsGet = () => new Promise((resolve, reject) => {
  session.on('Network.requestWillBeSent', common.mustCall(({ params }) => {
    assert.ok(params.requestId.startsWith('node-network-event-'));
    assert.strictEqual(params.request.url, 'https://127.0.0.1/hello-world');
    assert.strictEqual(params.request.method, 'GET');
    assert.strictEqual(typeof params.request.headers, 'object');
    assert.strictEqual(params.request.headers['accept-language'], 'en-US');
    assert.strictEqual(params.request.headers.cookie, 'k1=v1; k2=v2');
    assert.strictEqual(params.request.headers.age, '1000');
    assert.strictEqual(params.request.headers['x-header1'], 'value1, value2');
    assert.strictEqual(typeof params.timestamp, 'number');
    assert.strictEqual(typeof params.wallTime, 'number');
  }));
  session.on('Network.responseReceived', common.mustCall(({ params }) => {
    assert.ok(params.requestId.startsWith('node-network-event-'));
    assert.strictEqual(typeof params.timestamp, 'number');
    assert.strictEqual(params.type, 'Other');
    assert.strictEqual(params.response.status, 200);
    assert.strictEqual(params.response.statusText, 'OK');
    assert.strictEqual(params.response.url, 'https://127.0.0.1/hello-world');
    assert.strictEqual(typeof params.response.headers, 'object');
    assert.strictEqual(params.response.headers.server, 'node');
    assert.strictEqual(params.response.headers.etag, '12345');
    assert.strictEqual(params.response.headers['set-cookie'], 'key1=value1\nkey2=value2');
    assert.strictEqual(params.response.headers['x-header2'], 'value1, value2');
  }));
  session.on('Network.loadingFinished', common.mustCall(({ params }) => {
    assert.ok(params.requestId.startsWith('node-network-event-'));
    assert.strictEqual(typeof params.timestamp, 'number');
    resolve();
  }));

  https.get({
    host: '127.0.0.1',
    port: httpsServer.address().port,
    path: '/hello-world',
    rejectUnauthorized: false,
    headers: requestHeaders,
  }, common.mustCall());
});

const testHttpError = () => new Promise((resolve, reject) => {
  session.on('Network.requestWillBeSent', common.mustCall());
  session.on('Network.loadingFailed', common.mustCall(({ params }) => {
    assert.ok(params.requestId.startsWith('node-network-event-'));
    assert.strictEqual(typeof params.timestamp, 'number');
    assert.strictEqual(params.type, 'Other');
    assert.strictEqual(typeof params.errorText, 'string');
    resolve();
  }));
  session.on('Network.responseReceived', common.mustNotCall());
  session.on('Network.loadingFinished', common.mustNotCall());

  http.get({
    host: addresses.INVALID_HOST,
  }, common.mustNotCall()).on('error', common.mustCall());
});


const testHttpsError = () => new Promise((resolve, reject) => {
  session.on('Network.requestWillBeSent', common.mustCall());
  session.on('Network.loadingFailed', common.mustCall(({ params }) => {
    assert.ok(params.requestId.startsWith('node-network-event-'));
    assert.strictEqual(typeof params.timestamp, 'number');
    assert.strictEqual(params.type, 'Other');
    assert.strictEqual(typeof params.errorText, 'string');
    resolve();
  }));
  session.on('Network.responseReceived', common.mustNotCall());
  session.on('Network.loadingFinished', common.mustNotCall());

  https.get({
    host: addresses.INVALID_HOST,
  }, common.mustNotCall()).on('error', common.mustCall());
});

const testNetworkInspection = async () => {
  await testHttpGet();
  session.removeAllListeners();
  await testHttpsGet();
  session.removeAllListeners();
  await testHttpError();
  session.removeAllListeners();
  await testHttpsError();
  session.removeAllListeners();
};

httpServer.listen(0, () => {
  httpsServer.listen(0, async () => {
    try {
      await session.post('Network.enable');
      await testNetworkInspection();
      await session.post('Network.disable');
    } catch (e) {
      assert.fail(e);
    } finally {
      terminate();
    }
  });
});
