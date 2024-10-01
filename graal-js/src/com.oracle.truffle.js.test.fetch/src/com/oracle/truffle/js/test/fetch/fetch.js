/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Experimental fetch API shim backed by Java 11 HttpClient.
 */

(class Fetch {
    #private;

    static {
        // Fail early if we cannot resolve any of the required classes.
        const URI = Java.type('java.net.URI');
        const HttpClient = Java.type('java.net.http.HttpClient');
        const HttpClient_Redirect = Java.type('java.net.http.HttpClient.Redirect');
        const HttpRequest = Java.type('java.net.http.HttpRequest');
        const HttpResponse = Java.type('java.net.http.HttpResponse');
        const HttpRequest_BodyPublishers = Java.type('java.net.http.HttpRequest.BodyPublishers');
        const HttpResponse_BodyHandlers = Java.type('java.net.http.HttpResponse.BodyHandlers');
        const ConnectException = Java.type('java.net.ConnectException');
        const java_lang_String = Java.type('java.lang.String');
        const ByteBuffer = Java.type('java.nio.ByteBuffer');
        const Base64 = Java.type('java.util.Base64');
        const StandardCharsets = Java.type('java.nio.charset.StandardCharsets');

        function parseAndValidateURL(url) {
            try {
                let uri = new URI(url);
                if (!uri.isAbsolute()) {
                    throw new TypeError("URI is not absolute");
                }
                if (uri.getUserInfo() != null) {
                    throw new TypeError(`${url} is an url with embedded credentials`);
                }
                return uri;
            } catch (e) { // URISyntaxException
                throw new TypeError("Invalid URL: " + e.message);
            }
        }

        function validateHeaderName(name) {
            const validHttpToken = /^[\^_`a-zA-Z\-0-9!#$%&'*+.|~]+$/;
            if (!validHttpToken.test(name)) {
                throw new TypeError(`Header name must be a valid HTTP token: [${name}]`);
            }
            return name;
        }

        function validateHeaderValue(name, value) {
            const invalidFieldValueChar = /[^\t\x20-\x7e\x80-\xff]/;
            if (invalidFieldValueChar.exec(value) !== null) {
                throw new TypeError(`Invalid character in header field [${name}: ${value}]`)
            }
            return value;
        }
        function normalizeHeaderValue(value) {
            return value.trim();
        }
        function defineToStringTag(constructor) {
            Object.defineProperty(constructor.prototype, Symbol.toStringTag, {value: constructor.name, configurable: true, writable: true, enumerable: false});
        }

        // Wraps a byte[] to facilitate type checking in Blob constructor.
        class ByteArrayWrapper extends Fetch {
            constructor(byteArray) {
                super();
                this.#private = byteArray;
            }
        }

        class Blob {
            #size = 0;
            #byteArray;
            #text;
            #buffer;
            #type;

            constructor(array, {type} = {}) {
                if (array instanceof Blob) {
                    this.#size = array.#size;
                    this.#byteArray = array.#byteArray;
                    this.#type = array.#type;
                } else if (array instanceof ByteArrayWrapper) {
                    let byteArray = array.#private;
                    this.#size = byteArray.length;
                    this.#byteArray = byteArray;
                    this.#type = String(type ?? 'text/plain;charset=utf-8');
                } else if (typeof array === 'string') {
                    // TODO string to utf-8 bytes
                    this.#size = array.length;
                    this.#text = array;
                    this.#type = String(type ?? 'text/plain;charset=utf-8');
                } else if (array instanceof ArrayBuffer) {
                    // TODO string from utf-8 bytes
                    this.#size = array.byteLength;
                    this.#buffer = array;
                    this.#type = String(type ?? 'text/plain;charset=utf-8');
                } else if (array == null) {
                    this.#size = 0;
                    this.#type = String(type ?? '');
                } else {
                    throw new TypeError(`Unsupported array type`);
                }
            }
            get size() {
                return this.#size;
            }
            get type() {
                return this.#type;
            }
            async arrayBuffer() {
                if (this.size === 0) {
                    return new ArrayBuffer(0);
                } else if (this.#buffer != undefined) {
                    return this.#buffer;
                } else if (this.#byteArray != undefined) {
                    return new ArrayBuffer(ByteBuffer.wrap(this.#byteArray));
                } else {
                    throw new TypeError(`Unsupported body type`);
                }
            }
            async text() {
                if (this.size === 0) {
                    return '';
                } else if (this.#text != undefined) {
                    return this.#text;
                } else if (this.#byteArray != undefined) {
                    let charset = /\bcharset=([\w-]+)\b/.exec(this.#type)?.[1];
                    if (charset) {
                        try {
                            return String(new java_lang_String(this.#byteArray, charset));
                        } catch {
                            // ignore UnsupportedEncodingException
                        }
                    }
                    return String(new java_lang_String(this.#byteArray, StandardCharsets.UTF_8));
                } else {
                    throw new TypeError(`Unsupported body type`);
                }
            }
        }
        defineToStringTag(Blob);

        function asBlob(body) {
            return body instanceof Blob ? body : new Blob(body);
        }

        function fillHeaders(headers, init) {
            if (init instanceof Headers) {
                init = init.entries();
            } else if (Array.isArray(init)) {
                init = init;
            } else if (typeof init === 'object' && init !== null) {
                init = init[Symbol.iterator]?.() ?? Object.entries(init);
            } else {
                throw new TypeError("Failed to construct 'Headers': The provided value has incorrect type");
            }
            for (const [k, v] of init) {
                headers.append(k, v);
            }
        }

        class Headers {
            #m = new Map();
            #needsSort = false;
            #lastAdded = '';

            constructor(init = {}) {
                fillHeaders(this, init);
            }

            append(name, value) {
                name = String(name);
                value = String(value);
                validateHeaderName(name);
                validateHeaderValue(name, value);
                const lowerName = name.toLowerCase();
                const normalizedValue = normalizeHeaderValue(String(value));
                let values = this.#m.get(lowerName);
                if (values === undefined) {
                    values = []
                    this.#m.set(lowerName, values);
                    this.#needsSort = this.#needsSort || lowerName < this.#lastAdded;
                    this.#lastAdded = lowerName;
                }
                values.push(normalizedValue);
            }
            get(name) {
                name = String(name);
                validateHeaderName(name);
                const lowerName = name.toLowerCase();
                let values = this.#m.get(lowerName);
                if (values === undefined) {
                    return null;
                } else {
                    return values.join(', ');
                }
            }
            has(name) {
                name = String(name);
                validateHeaderName(name);
                const lowerName = name.toLowerCase();
                return this.#m.has(lowerName);
            }
            delete(name) {
                name = String(name);
                validateHeaderName(name);
                const lowerName = name.toLowerCase();
                this.#m.delete(lowerName)
            }
            set(name, value) {
                name = String(name);
                value = String(value);
                validateHeaderName(name);
                validateHeaderValue(name, value);
                const lowerName = name.toLowerCase();
                const normalizedValue = normalizeHeaderValue(value);
                if (!this.#m.has(lowerName)) {
                    this.#needsSort = this.#needsSort || lowerName < this.#lastAdded;
                    this.#lastAdded = lowerName;
                }
                this.#m.set(lowerName, [normalizedValue])
            }
            getSetCookie() {
                const cookies = this.#m.get('set-cookie');
                return cookies ? [...cookies] : [];
            }
            #sortedEntries() {
                if (this.#needsSort) {
                    this.#m = new Map([...this.#m.entries()].sort(([a], [b]) => a < b ? -1 : a == b ? 0 : 1));
                    this.#needsSort = false;
                }
                return this.#m.entries();
            }
            forEach(callback, thisArg = undefined) {
                for (const [name, values] of this.#sortedEntries()) {
                    for (const value of values) {
                        Reflect.apply(callback, thisArg, [value, name, this]);
                    }
                }
            }

            // [Symbol.iterator] == entries
            *entries() {
                for (const [name, values] of this.#sortedEntries()) {
                    for (const value of values) {
                        yield [name, value];
                    }
                }
            }
            *keys() {
                for (const [name, values] of this.#sortedEntries()) {
                    for (const value of values) {
                        yield name;
                    }
                }
            }
            *values() {
                for (const [name, values] of this.#sortedEntries()) {
                    for (const value of values) {
                        yield value;
                    }
                }
            }
        }
        Object.defineProperty(Headers.prototype, Symbol.iterator, {value: Headers.prototype.entries, configurable: true, writable: true, enumerable: false});
        defineToStringTag(Headers);

        function normalizeMethod(method) {
            method = String(method);
            // https://fetch.spec.whatwg.org/#concept-method-normalize
            if (/^(?:DELETE|GET|HEAD|OPTIONS|POST|PUT)$/.test(method)) {
                return method;
            } else if (/^(?:DELETE|GET|HEAD|OPTIONS|POST|PUT)$/i.test(method)) {
                return method.toUpperCase();
            } else {
                if (/^(?:CONNECT|TRACE|TRACK)$/i.test(method)) {
                    throw new TypeError(`Forbidden method name: ${method}`);
                }
                return method;
            }
        }

        function makeRequest(init, uri, url, source) {
            // https://fetch.spec.whatwg.org/#requests
            let referrer = init.referrer;
            if (referrer == undefined) {
                referrer = source?.referrer ?? 'client';
            } else {
                referrer = String(referrer);
                if (referrer == '') {
                    referrer = 'no-referrer';
                } else {
                    parseAndValidateURL(referrer);
                }
            }
            let referrerPolicy = String(init.referrerPolicy ?? source?.referrerPolicy ?? '');
            let redirect = String(init.redirect ?? source?.redirect ?? 'follow');
            let method = normalizeMethod(init.method ?? source?.method ?? 'GET');
            let headers = new Headers(init.headers ?? source?.headers);
            let body = init.body ?? source?.body ?? null;
            if (body !== null && (method == 'GET' || method == 'HEAD')) {
                throw new TypeError("Request with GET/HEAD method cannot have body");
            }
            let follow = (init.follow ?? 20) | 0;
            return {
                method,
                body,
                referrer,
                redirect,
                headers,
                uri,
                url,
                redirectCount: 0,
                follow,
                referrerPolicy,
                mode: 'no-cors',
                credentials: 'same-origin',
                cache: 'default',
            };
        }

        function validateBody(body) {
            if (typeof body !== 'string' && typeof body !== 'object' && typeof init !== 'undefined') {
                throw new TypeError(`Excepted body to be one of: Null, Undefined, String, Object.`);
            }
        }
        function validateInit(init) {
            if (typeof init !== 'object' && typeof init !== 'undefined') {
                throw new TypeError(`Excepted init to be one of: Null, Undefined, Object.`);
            }
        }
        function validateStatus(code) {
            code = code | 0;
            if (code === 0) { // allow 0 for Response.error()
                return code;
            }
            if (code < 200 || code > 599) {
                throw new RangeError(`init["status"] must be in the range of 200 to 599, inclusive.`)
            }
            return code;
        }

        let getRequestPrivate; // Private field accessor

        class Request {
            #req;
            #bodyUsed = false;

            static {
                getRequestPrivate = (req) => req.#req;
            }

            constructor(input, init = {}) {
                let url;
                let uri;
                let source;
                const isRequest = typeof input === 'object' && #req in input;
                if (isRequest) {
                    url = input.#req.url;
                    uri = input.#req.uri;
                    source = input.#req;
                    validateInit(init);
                } else {
                    url = String(input);
                    validateInit(init);
                    uri = parseAndValidateURL(url);
                }
                this.#req = makeRequest(init, uri, url, source);
            }

            get url() {
                return this.#req.url;
            }
            get method() {
                return this.#req.method;
            }
            get referrer() {
                let referrer = this.#req.referrer;
                if (referrer == 'no-referrer') {
                    return '';
                } else if (referrer == 'client') {
                    return 'about:client';
                }
                return referrer;
            }
            get referrerPolicy() {
                return this.#req.referrerPolicy;
            }
            get redirect() {
                return this.#req.redirect;
            }
            get headers() {
                return this.#req.headers;
            }

            get body() {
                return this.#req.body;
            }
            get bodyUsed() {
                return this.#bodyUsed;
            }

            get uri() {
                return this.#req.uri;
            }

            clone() {
                if (this.#bodyUsed) {
                    throw new TypeError("Body has already been consumed");
                }
                return new Request(this);
            }

            #consumeBody() {
                if (!this.#bodyUsed) {
                    this.#bodyUsed = true;
                    return this.#req.body;
                } else {
                    throw new TypeError("Body already used");
                }
            }

            // body
            async text() {
                let body = this.#consumeBody();
                return await asBlob(body).text();
            }
            async json() {
                return JSON.parse(await this.text());
            }
            async arrayBuffer() {
                let body = this.#consumeBody();
                return await asBlob(body).arrayBuffer();
            }
            async blob() {
                let body = this.#consumeBody();
                return asBlob(body);
            }
        }
        defineToStringTag(Request);

        function makeResponse(init) {
            return {
                type: String(init.type ?? 'default'),
                status: validateStatus(init.status ?? 200),
                statusText: String(init.statusText ?? ''),
                url: String(init.url ?? ''),
                headers: new Headers(init.headers),
                redirectCount: (init.redirectCount ?? 0) | 0,
            };
        }

        class Response {
            #res;
            #body;
            #bodyUsed = false;

            constructor(body = null, init = {}) {
                validateBody(body);
                validateInit(init);
                this.#body = body;
                this.#res = makeResponse(init);
            }

            static error() {
                return new Response(null, {type: 'error', status: 0, statusText: ''});
            }

            static json(body, init = {}) {
                body = JSON.stringify(body);
                let headers = new Headers({'Content-Type': 'application/json'});
                if (init.headers != null) {
                    fillHeaders(headers, init.headers);
                }
                return new Response(body, {...init, headers});
            }

            static redirect(url, status) {
                url = String(url);
                status = status | 0;
                parseAndValidateURL(url);
                if (!isRedirect(status)) {
                    throw new RangeError(`Invalid status code ${status}`);
                }
                return new Response(null, {status, statusText: getStatusText(status), headers: {'Location': url}});
            }

            get url() {
                return this.#res.url;
            }
            get type() {
                return this.#res.type;
            }
            get status() {
                return this.#res.status;
            }
            get statusText() {
                return this.#res.statusText;
            }
            get ok() {
                const statusCode = this.#res.status;
                return 200 <= statusCode && statusCode <= 206;
            }
            get headers() {
                return this.#res.headers;
            }
            get redirected() {
                return this.#res.redirectCount > 0;
            }

            get body() {
                return this.#body;
            }
            get bodyUsed() {
                return this.#bodyUsed;
            }

            clone() {
                if (this.#bodyUsed) {
                    throw new TypeError("Body has already been consumed");
                }
                let clone = new Response(this.#body);
                clone.#res = makeResponse(this.#res);
                return clone;
            }

            #consumeBody() {
                if (!this.#bodyUsed) {
                    this.#bodyUsed = true;
                    return this.#body;
                } else {
                    throw new TypeError("Body already used");
                }
            }

            // body
            async text() {
                let body = this.#consumeBody();
                return await asBlob(body).text();
            }
            async json() {
                return JSON.parse(await this.text());
            }
            async arrayBuffer() {
                let body = this.#consumeBody();
                return await asBlob(body).arrayBuffer();
            }
            async blob() {
                let body = this.#consumeBody();
                return asBlob(body);
            }
        }
        defineToStringTag(Response);

        function FetchError(message, code) {
            const error = new TypeError(message);
            if (code) {
                error.code = code;
            }
            return error;
        }

        const PRIVATE_HEADERS = [
            "authorization",
            "www-authenticate",
            "cookie",
            "cookie2",
        ];
        const CONTENT_HEADERS = [
            "content-length",
            "content-type",
            "content-location",
            "content-language",
            "content-encoding",
        ];
        const VALID_POLICY = [
            "",
            "no-referrer",
            "no-referrer-when-downgrade",
            "same-origin",
            "origin",
            "strict-origin",
            "origin-when-cross-origin",
            "strict-origin-when-cross-origin",
            "unsafe-url",
        ];

        const HTTP_MOVED_PERM = 301;
        const HTTP_MOVED_TEMP = 302;
        const HTTP_SEE_OTHER = 303;

        async function fetch(input, init={}) {
            let request = new Request(input, init);
            let requestPrivate = getRequestPrivate(request);

            let scheme = requestPrivate.uri.getScheme();
            switch (scheme) {
                case "http":
                case "https":
                    return await httpFetch(request);
                case "data":
                    return await dataFetch(request);
                default:
                    throw new TypeError("fetch cannot load " + request.url + ". Scheme not supported: " + scheme);
            }
        }

        async function dataFetch(request) {
            let requestPrivate = getRequestPrivate(request);
            let dataUri = requestPrivate.uri;
            // 1. Assert: dataUrl scheme is "data"
            if (requestPrivate.uri.getScheme() !== "data") {
                return null;
            }

            // 2. Let input be the result of running the URL serializer on dataURL with exclude fragment
            // set to true.
            if (dataUri.getFragment() != null) {
                dataUri = new URI(dataUri.getScheme(), dataUri.getSchemeSpecificPart(), null);
            }

            let input = dataUri.toString();

            // 3. Remove the leading "data:" from input.
            input = input.substring(5);

            // 5. Let mimeType be the result of collecting a sequence of code points
            // that are not equal to U+002C (,), given position.
            let commaPos = input.indexOf(',');
            let mimeType = input.substring(0, commaPos);

            // 6. Strip leading and trailing ASCII whitespace from mimeType.
            mimeType = mimeType.trim();

            // 8. Advance position by 1.
            // 9. Let encodedBody be the remainder of input.
            let encodedBody = input.substring(commaPos + 1);
            let body;

            // 11. If mimeType ends with U+003B (;),
            // followed by zero or more U+0020 SPACE,
            // followed by an ASCII case-insensitive match for "base64",
            // then:
            let match = /;\u0020*base64$/i.exec(mimeType);
            if (match != null) {
                // 11.2. Set body to the forgiving-base64 decode of stringBody.
                let decodedBytes = Base64.getDecoder().decode(encodedBody.trim());
                body = new Blob(new ByteArrayWrapper(decodedBytes));
                // 11.4 Remove the last 6 code points from mimeType.
                // 11.5 Remove trailing U+0020 SPACE code points from mimeType, if any.
                // 11.6 Remove the last U+003B (;) from mimeType.
                mimeType = mimeType.slice(0, -match[0].length);
            } else {
                body = decodeURI(encodedBody);
            }

            // 12. If mimeType starts with ";", then prepend "text/plain" to mimeType.
            if (mimeType.startsWith(";")) {
                mimeType = "text/plain" + mimeType;
            }

            // 14. If mimeTypeRecord is failure, then set mimeTypeRecord to text/plain;charset=US-ASCII.
            if (mimeType === '') {
                mimeType = "text/plain;charset=US-ASCII";
            }

            let response = new Response(body, {
                status: 200,
                statusText: 'OK',
                url: request.url,
                headers: {'Content-Type': mimeType},
            });
            return response;
        }

        function isByteArrayLike(body) {
            if (body instanceof ArrayBuffer) {
                return true;
            } else if (ArrayBuffer.isView(body)) {
                return true;
            } else {
                return false;
            }
        }
        function makeByteArray(body) {
            if (body instanceof ArrayBuffer) {
                return new Int8Array(body);
            } else if (ArrayBuffer.isView(body)) {
                if (body instanceof Int8Array) {
                    return body;
                }
                return new Int8Array(body.buffer.slice(body.byteOffset, body.byteOffset + body.byteLength));
            } else {
                throw new TypeError();
            }
        }

        async function httpFetch(request) {
            let requestPrivate = getRequestPrivate(request);

            let httpClient = HttpClient.newBuilder()
                            .followRedirects(HttpClient_Redirect.NEVER) // don't follow automatically
                            .build();

            let httpRequestBuilder = HttpRequest.newBuilder(request.uri);
            let bodyPublisher;
            let requestBody = request.body;
            if (requestBody == null) {
                bodyPublisher = HttpRequest_BodyPublishers.noBody();
            } else if (isByteArrayLike(requestBody)) {
                bodyPublisher = HttpRequest_BodyPublishers.ofByteArray(makeByteArray(requestBody));
            } else {
                bodyPublisher = HttpRequest_BodyPublishers.ofString(String(requestBody));
                // set a default Content-Type if none given
                httpRequestBuilder.setHeader("Content-Type", "text/plain;charset=UTF-8");
            }
            httpRequestBuilder.method(request.method, bodyPublisher);
            httpRequestBuilder.setHeader("Accept", "*/*");
            httpRequestBuilder.setHeader("Accept-Encoding", ""); // compression not supported
            httpRequestBuilder.setHeader("User-Agent", "graaljs-fetch");
            if (requestPrivate.referrer !== 'client') {
                httpRequestBuilder.setHeader("Referer", requestPrivate.referrer); // [sic]
            }
            // add user-specified headers
            let lastName;
            for (const [name, value] of request.headers.entries()) {
                if (name !== lastName) {
                    httpRequestBuilder.setHeader(name, value);
                } else {
                    httpRequestBuilder.header(name, value);
                }
                lastName = name;
            }
            let httpRequest = httpRequestBuilder.build();

            let httpResponse;
            try {
                httpResponse = httpClient.send(httpRequest, HttpResponse_BodyHandlers.ofByteArray());
            } catch (e) {
                if (e instanceof ConnectException) {
                    throw new FetchError("Connection refused", "ECONNREFUSED");
                }
                throw new TypeError(e.message);
            }

            let status = httpResponse.statusCode();

            if (isRedirect(status)) {
                // Par. 4.3 Http fetch step 8
                switch (request.redirect) {
                    case "manual":
                        // return response as is
                        break;
                    case "error":
                        throw new FetchError("uri requested responds with a redirect, redirect mode is set to error", "no-redirect");
                    case "follow": {
                        let locationURI = null;
                        let location = httpResponse.headers().firstValue("Location");
                        try {
                            // HTTP-redirect fetch step 3
                            if (location.isPresent()) {
                                locationURI = new URI(requestPrivate.url).resolve(location.get());
                            }
                        } catch { // URISyntaxException
                            throw new FetchError("invalid url in location header", "unsupported-redirect");
                        }

                        // HTTP-redirect fetch step 4
                        if (locationURI == null) {
                            break;
                        }

                        // HTTP-redirect fetch step 7
                        if (requestPrivate.redirectCount >= Math.min(20, requestPrivate.follow)) {
                            throw new FetchError("maximum redirect reached at: " + request.url, "max-redirect");
                        }

                        // HTTP-redirect fetch step 8
                        requestPrivate.redirectCount++;

                        // remove sensitive headers if redirecting to a new domain or protocol changes
                        if (!isDomainOrSubDomain(locationURI, requestPrivate.uri) || !isSameProtocol(locationURI, requestPrivate.uri)) {
                            PRIVATE_HEADERS.forEach(k => {
                                request.headers.delete(k);
                            });
                        }

                        // HTTP-redirect fetch step 11
                        if (status !== HTTP_SEE_OTHER && requestPrivate.body != null) {
                            throw new FetchError("Cannot follow redirect with body", "unsupported-redirect");
                        }

                        // HTTP-redirect fetch step 12
                        if (status === HTTP_SEE_OTHER ||
                            ((status === HTTP_MOVED_PERM || status === HTTP_MOVED_TEMP) && request.method === "POST")) {
                            requestPrivate.method = "GET";
                            requestPrivate.body = null;
                            CONTENT_HEADERS.forEach(k => {
                                request.headers.delete(k);
                            });
                        }

                        // HTTP-redirect fetch step 17
                        requestPrivate.uri = locationURI;
                        requestPrivate.url = locationURI.toString();

                        // HTTP-redirect fetch step 18
                        // https://w3c.github.io/webappsec-referrer-policy/#set-requests-referrer-policy-on-redirect
                        let policyTokens = httpResponse.headers().allValues("referrer-policy");
                        if (policyTokens.length > 0) {
                            policyTokens.forEach(token => {
                                if (VALID_POLICY.includes(token)) {
                                    requestPrivate.referrerPolicy = token;
                                }
                            });
                        }

                        // HTTP-redirect fetch step 19 invoke fetch, following the redirect
                        return await httpFetch(request);
                    }
                    default:
                        throw new FetchError(`Redirect option ${request.redirect} is not a valid value of RequestRedirect`);
                }
            }

            let headers = new Headers();
            for (const [name, values] of new Map(httpResponse.headers().map())) {
                if (name === ":status") {
                    // Skip HTTP/2 response pseudo-header fields
                    continue;
                }
                for (const value of values) {
                    headers.append(name, value);
                }
            }
            let body = new Blob(new ByteArrayWrapper(httpResponse.body()), {type: headers.get('content-type')});

            let response = new Response(body, {
                status,
                headers,
                url: request.url,
                statusText: getStatusText(status),
                redirectCount: requestPrivate.redirectCount
            });
            return response;
        }

        function isDomainOrSubDomain(/*URI*/ destination, /*URI*/ origin) {
            return destination.getHost() === origin.getHost() || origin.getHost().endsWith("." + destination.getHost());
        }

        function isSameProtocol(/*URI*/ destination, /*URI*/ origin) {
            return destination.getScheme() === origin.getScheme();
        }

        function isRedirect(statusCode) {
            switch (statusCode) {
                case 301:
                case 302:
                case 303:
                case 307:
                case 308:
                    return true;
                default:
                    return false;
            }
        }

        function getStatusText(statusCode) {
            switch (statusCode) {
                /* 2XX: generally "OK" */
                case 200: return "OK";
                case 201: return "Created";
                case 202: return "Accepted";
                case 203: return "Non-Authoritative Information";
                case 204: return "No Content";
                case 205: return "Reset Content";
                case 206: return "Partial Content";

                /* 3XX: relocation/redirect */
                case 300: return "Multiple Choices";
                case 301: return "Moved Permanently";
                case 302: return "Found"; // previously: Moved Temporarily
                case 303: return "See Other";
                case 304: return "Not Modified";
                case 305: return "Use Proxy";
                case 306: return "Switch Proxy";
                case 307: return "Temporary Redirect";
                case 308: return "Permanent Redirect";

                /* 4XX: client error */
                case 400: return "Bad Request";
                case 401: return "Unauthorized";
                case 402: return "Payment Required";
                case 403: return "Forbidden";
                case 404: return "Not Found";
                case 405: return "Method Not Allowed";
                case 406: return "Not Acceptable";
                case 407: return "Proxy Authentication Required";
                case 408: return "Request Timeout";
                case 409: return "Conflict";
                case 410: return "Gone";
                case 411: return "Length Required";
                case 412: return "Precondition Failed";
                case 413: return "Payload Too Large"; // previously: Request Entity Too Large
                case 414: return "URI Too Long"; // previously: Request-URI Too Long
                case 415: return "Unsupported Media Type";
                case 416: return "Range Not Satisfiable";
                case 417: return "Expectation Failed";
                case 418: return "I'm a teapot";

                /* 5XX: server error */
                case 500: return "Internal Server Error";
                case 501: return "Not Implemented";
                case 502: return "Bad Gateway";
                case 503: return "Service Unavailable";
                case 504: return "Gateway Timeout";
                case 505: return "HTTP Version Not Supported";

                default: return "";
            };
        }

        [fetch, Headers, Request, Response].forEach((fn) => {
            Object.defineProperty(globalThis, fn.name, {value: fn, configurable: true, writable:true, enumerable: false});
        });
    }
});
