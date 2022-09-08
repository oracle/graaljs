/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of the Headers class
 */

load('../assert.js');

(function shouldConformToIDL() {
    const h = new Headers();
    assertTrue(Reflect.has(h, 'append'));
    assertTrue(Reflect.has(h, 'delete'));
    assertTrue(Reflect.has(h, 'get'));
    assertTrue(Reflect.has(h, 'has'));
    assertTrue(Reflect.has(h, 'set'));
    assertTrue(Reflect.has(h, 'forEach'));
})();

(function shouldProvideAppendMethod() {
    const h = new Headers();
    h.append('a', '1');
    assertSame('1', h.get('a'));
})();

(function shouldProvideDeleteMethod() {
    const h = new Headers({a: '1'});
    h.delete('a');
    assertFalse(h.has('a'));
})();

(function shouldProvideGetMethod() {
    const h = new Headers({a: '1'});
    assertSame('1', h.get('a'));
})();

(function shouldProvideSetMethod() {
    const h = new Headers();
    h.set('a', '1');
    assertSame('1', h.get('a'));
})();

(function shouldSetOverwrite() {
    const h = new Headers({a: '1'});
    h.set('a', '2');
    assertSame('2', h.get('a'));
})();

(function shouldProvideHasMethod() {
    const h = new Headers({a: '1'});
    assertTrue(h.has('a'));
    assertFalse(h.has('foo'));
})();

(function shouldCreateListWhenAppending() {
    const h = new Headers({'a': '1'});
    h.append('a', '2');
    h.append('a', '3');
    assertSame('1, 2, 3', h.get('a'));
})();

(function shouldConvertHeaderNamesToLowercase() {
    const h = new Headers({'A': '1', 'a': '2'});
    h.append('A', '3');
    h.append('a', '4');
    h.set('Content-Type', 'application/json');

    assertTrue(h.has('A') && h.has('a'));
    assertSame('1, 2, 3, 4', h.get('a'));

    assertSame('application/json', h.get('content-type'));
    assertSame('application/json', h.get('Content-Type'));
})();

(function shouldAllowIteratingWithForEach() {
    const headers = new Headers({'a': '1', 'b': '2', 'c': '3'});
    const result = [];
    headers.forEach((val, key, _) => {
        result.push(`${key}: ${val}`);
    });
    assertSame("a: 1", result[0]);
    assertSame("b: 2", result[1]);
    assertSame("c: 3", result[2]);
})();

(function thisShouldBeUndefinedInForEach() {
    const headers = new Headers();
    headers.forEach(function() {
       assertSame(undefined, this);
    });
})();

(function shouldAcceptThisArgArgumentInForEach() {
    const headers = new Headers();
    const thisArg = {};
    headers.forEach(function() {
        assertSame(thisArg, this);
    }, thisArg);
})();

(function shouldBeSortedByHeaderName() {
    const h = new Headers({'c': '3', 'a' : '1', 'd': '4'});
    h.append('b', '2');

    const result = []
    h.forEach((v, k) => {
        result.push(`${k}: ${v}`);
    })

    assertSame('a: 1', result[0]);
    assertSame('b: 2', result[1]);
    assertSame('c: 3', result[2]);
    assertSame('d: 4', result[3]);
})();

(function shouldValidateHeaders() {
    // invalid header
    assertThrows(() => new Headers({'': 'ok'}), TypeError);
    assertThrows(() => new Headers({'HE y': 'ok'}), TypeError);
    assertThrows(() => new Headers({'Hé-y': 'ok'}), TypeError);
    // invalid value
    assertThrows(() => new Headers({'HE-y': 'ăk'}), TypeError);
})();

(function shouldValidateHeadersInMethods() {
    const headers = new Headers();
    assertThrows(() => headers.append('', 'ok'), TypeError);
    assertThrows(() => headers.append('Hé-y', 'ok'), TypeError);
    assertThrows(() => headers.append('HE-y', 'ăk'), TypeError);
    assertThrows(() => headers.delete('Hé-y'), TypeError);
    assertThrows(() => headers.get('Hé-y'), TypeError);
    assertThrows(() => headers.has('Hé-y'), TypeError);
    assertThrows(() => headers.set('Hé-y', 'ok'), TypeError);
    assertThrows(() => headers.set('HE-y', 'ăk'), TypeError);
})();

(function shouldNormalizeValues() {
    const headers = new Headers({'a': ' 1', });
    headers.append('b', '2 ');
    headers.set('c', ' 3 ');
    assertSame('1', headers.get('a'));
    assertSame('2', headers.get('b'));
    assertSame('3', headers.get('c'));
})();

(function shouldWrapHeadersObject() {
    const h1 = new Headers({'a': '1'});

    const h2 = new Headers(h1);
    h2.set('b', '1');

    const h3 = new Headers(h2);
    h3.append('a', '2');

    assertFalse(h1.has('b'));
    assertTrue(h2.has('a'));
    assertSame('1, 2', h3.get('a'));
})();

(function shouldRejectIncorrectConstructorArguments() {
    assertThrows(() => new Headers(''), TypeError);
    assertThrows(() => new Headers(0), TypeError);
    assertThrows(() => new Headers(false), TypeError);
})();
