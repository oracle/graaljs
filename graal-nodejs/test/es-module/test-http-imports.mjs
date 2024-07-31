// Flags: --experimental-network-imports --dns-result-order=ipv4first
import * as common from '../common/index.mjs';
import * as fixtures from '../common/fixtures.mjs';
import tmpdir from '../common/tmpdir.js';
import assert from 'assert';
import http from 'http';
import os from 'os';
import util from 'util';
import { describe, it } from 'node:test';

if (!common.hasCrypto) {
  common.skip('missing crypto');
}
tmpdir.refresh();

const https = (await import('https')).default;

const createHTTPServer = http.createServer;

// Needed to deal w/ test certs
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
const options = {
  key: fixtures.readKey('agent1-key.pem'),
  cert: fixtures.readKey('agent1-cert.pem')
};

const createHTTPSServer = https.createServer.bind(null, options);


const testListeningOptions = [
  {
    hostname: 'localhost',
    listenOptions: {
      host: '127.0.0.1'
    }
  },
];

const internalInterfaces = Object.values(os.networkInterfaces()).flat().filter(
  (iface) => iface?.internal && iface.address && !iface.scopeid
);
for (const iface of internalInterfaces) {
  testListeningOptions.push({
    hostname: iface?.family === 'IPv6' ? `[${iface?.address}]` : iface?.address,
    listenOptions: {
      host: iface?.address,
      ipv6Only: iface?.family === 'IPv6'
    }
  });
}

for (const { protocol, createServer } of [
  { protocol: 'http:', createServer: createHTTPServer },
  { protocol: 'https:', createServer: createHTTPSServer },
]) {
  const body = `
    export default (a) => () => a;
    export let url = import.meta.url;
  `;

  const base = 'http://127.0.0.1';
  for (const { hostname, listenOptions } of testListeningOptions) {
    const host = new URL(base);
    host.protocol = protocol;
    host.hostname = hostname;
    // /not-found is a 404
    // ?redirect causes a redirect, no body. JSON.parse({status:number,location:string})
    // ?mime sets the content-type, string
    // ?body sets the body, string
    const server = createServer(function(_req, res) {
      const url = new URL(_req.url, host);
      const redirect = url.searchParams.get('redirect');
      if (url.pathname === '/not-found') {
        res.writeHead(404);
        res.end();
        return;
      }
      if (redirect) {
        const { status, location } = JSON.parse(redirect);
        res.writeHead(status, {
          location
        });
        res.end();
        return;
      }
      res.writeHead(200, {
        'content-type': url.searchParams.get('mime') || 'text/javascript'
      });
      res.end(url.searchParams.get('body') || body);
    });

    const listen = util.promisify(server.listen.bind(server));
    await listen({
      ...listenOptions,
      port: 0
    });
    const url = new URL(host);
    url.port = server?.address()?.port;

    const ns = await import(url.href);
    assert.strict.deepStrictEqual(Object.keys(ns), ['default', 'url']);
    const obj = {};
    assert.strict.equal(ns.default(obj)(), obj);
    assert.strict.equal(ns.url, url.href);

    // Redirects have same import.meta.url but different cache
    // entry on Web
    const redirect = new URL(url.href);
    redirect.searchParams.set('redirect', JSON.stringify({
      status: 302,
      location: url.href
    }));
    const redirectedNS = await import(redirect.href);
    assert.strict.deepStrictEqual(
      Object.keys(redirectedNS),
      ['default', 'url']
    );
    assert.strict.notEqual(redirectedNS.default, ns.default);
    assert.strict.equal(redirectedNS.url, url.href);

    // Redirects have the same import.meta.url but different cache
    // entry on Web
    const relativeAfterRedirect = new URL(url.href + 'foo/index.js');
    const redirected = new URL(url.href + 'bar/index.js');
    redirected.searchParams.set('body', 'export let relativeDepURL = (await import("./baz.js")).url');
    relativeAfterRedirect.searchParams.set('redirect', JSON.stringify({
      status: 302,
      location: redirected.href
    }));
    const relativeAfterRedirectedNS = await import(relativeAfterRedirect.href);
    assert.strict.equal(
      relativeAfterRedirectedNS.relativeDepURL,
      url.href + 'bar/baz.js'
    );

    const unsupportedMIME = new URL(url.href);
    unsupportedMIME.searchParams.set('mime', 'application/node');
    unsupportedMIME.searchParams.set('body', '');
    await assert.rejects(
      import(unsupportedMIME.href),
      { code: 'ERR_UNKNOWN_MODULE_FORMAT' }
    );

    const notFound = new URL(url.href);
    notFound.pathname = '/not-found';
    await assert.rejects(
      import(notFound.href),
      { code: 'ERR_MODULE_NOT_FOUND' },
    );

    const jsonUrl = new URL(url.href + 'json');
    jsonUrl.searchParams.set('mime', 'application/json');
    jsonUrl.searchParams.set('body', '{"x": 1}');
    const json = await import(jsonUrl.href, { with: { type: 'json' } });
    assert.deepStrictEqual(Object.keys(json), ['default']);
    assert.strictEqual(json.default.x, 1);

    await describe('guarantee data url will not bypass import restriction', () => {
      it('should not be bypassed by cross protocol redirect', async () => {
        const crossProtocolRedirect = new URL(url.href);
        crossProtocolRedirect.searchParams.set('redirect', JSON.stringify({
          status: 302,
          location: 'data:text/javascript,'
        }));
        await assert.rejects(
          import(crossProtocolRedirect.href),
          { code: 'ERR_NETWORK_IMPORT_DISALLOWED' }
        );
      });

      it('should not be bypassed by data URL', async () => {
        const deps = new URL(url.href);
        deps.searchParams.set('body', `
        export {data} from 'data:text/javascript,export let data = 1';
        import * as http from ${JSON.stringify(url.href)};
        export {http};
      `);
        await assert.rejects(
          import(deps.href),
          { code: 'ERR_NETWORK_IMPORT_DISALLOWED' }
        );
      });

      it('should not be bypassed by encodedURI import', async () => {
        const deepDataImport = new URL(url.href);
        deepDataImport.searchParams.set('body', `
        import 'data:text/javascript,import${encodeURIComponent(JSON.stringify('data:text/javascript,import "os"'))}';
      `);
        await assert.rejects(
          import(deepDataImport.href),
          { code: 'ERR_NETWORK_IMPORT_DISALLOWED' }
        );
      });

      it('should not be bypassed by relative deps import', async () => {
        const relativeDeps = new URL(url.href);
        relativeDeps.searchParams.set('body', `
        import * as http from "./";
        export {http};
      `);
        const relativeDepsNS = await import(relativeDeps.href);
        assert.strict.deepStrictEqual(Object.keys(relativeDepsNS), ['http']);
        assert.strict.equal(relativeDepsNS.http, ns);
      });

      it('should not be bypassed by file dependency import', async () => {
        const fileDep = new URL(url.href);
        const { href } = fixtures.fileURL('/es-modules/message.mjs');
        fileDep.searchParams.set('body', `
        import ${JSON.stringify(href)};
        export default 1;`);
        await assert.rejects(
          import(fileDep.href),
          { code: 'ERR_NETWORK_IMPORT_DISALLOWED' }
        );
      });

      it('should not be bypassed by builtin dependency import', async () => {
        const builtinDep = new URL(url.href);
        builtinDep.searchParams.set('body', `
        import 'node:fs';
        export default 1;
      `);
        await assert.rejects(
          import(builtinDep.href),
          { code: 'ERR_NETWORK_IMPORT_DISALLOWED' }
        );
      });


      it('should not be bypassed by unprefixed builtin dependency import', async () => {
        const unprefixedBuiltinDep = new URL(url.href);
        unprefixedBuiltinDep.searchParams.set('body', `
        import 'fs';
        export default 1;
      `);
        await assert.rejects(
          import(unprefixedBuiltinDep.href),
          { code: 'ERR_NETWORK_IMPORT_DISALLOWED' }
        );
      });

      it('should not be bypassed by indirect network import', async () => {
        const indirect = new URL(url.href);
        indirect.searchParams.set('body', `
        import childProcess from 'data:text/javascript,export { default } from "node:child_process"'
        export {childProcess};
      `);
        await assert.rejects(
          import(indirect.href),
          { code: 'ERR_NETWORK_IMPORT_DISALLOWED' }
        );
      });

      it('data: URL can always import other data:', async () => {
        const data = new URL('data:text/javascript,');
        data.searchParams.set('body',
                              'import \'data:text/javascript,import \'data:\''
        );
        // doesn't throw
        const empty = await import(data.href);
        assert.ok(empty);
      });

      it('data: URL cannot import file: or builtin', async () => {
        const data1 = new URL(url.href);
        data1.searchParams.set('body',
                               'import \'file:///some/file.js\''
        );
        await assert.rejects(
          import(data1.href),
          { code: 'ERR_NETWORK_IMPORT_DISALLOWED' }
        );

        const data2 = new URL(url.href);
        data2.searchParams.set('body',
                               'import \'node:fs\''
        );
        await assert.rejects(
          import(data2.href),
          { code: 'ERR_NETWORK_IMPORT_DISALLOWED' }
        );
      });

      it('data: URL cannot import HTTP URLs', async () => {
        const module = fixtures.fileURL('/es-modules/import-data-url.mjs');
        try {
          await import(module);
        } catch (err) {
          // We only want the module to load, we don't care if the module throws an
          // error as long as the loader does not.
          assert.notStrictEqual(err?.code, 'ERR_MODULE_NOT_FOUND');
        }
        const data1 = new URL(url.href);
        const dataURL = 'data:text/javascript;export * from "node:os"';
        data1.searchParams.set('body', `export * from ${JSON.stringify(dataURL)};`);
        await assert.rejects(
          import(data1),
          { code: 'ERR_NETWORK_IMPORT_DISALLOWED' }
        );
      });
    });

    server.close();
  }
}
