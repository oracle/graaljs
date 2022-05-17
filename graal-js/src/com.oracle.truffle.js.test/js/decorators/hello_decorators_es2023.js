/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js');

var tick = 40;
function dec1() { tick++; }
function dec2() { tick++; }

@dec1
@dec2
class C1 {}

assertSame(42, tick);


/**
 * The following testcases assert that decorators do not regress on non-decorated classes.
 */
class C2 {
    p = 10;
    m() {}
    get x() {}
    set x(v) {}
}
class C3 {
    static get x() {}
}


//---------------------------------------------//
let c = new (class {
    #f() { return 21; }
    foo() { return 21 + this.#f(); }
});
assertSame(42, c.foo());


//---------------------------------------------//
let f = Object.getOwnPropertyDescriptor(
    class {
        static set f(a) {}
    }, 'f').set;
assertSame('function', typeof f);


//---------------------------------------------//
var yieldSet, C4, iter;
function* g() {
  C4 = class {
    get [yield]() {
      return 'get yield';
    };
    set [yield](param) {
      yieldSet = param;
    };
  };
}
iter = g()
iter.next()
iter.next('first')
iter.next('second')
assertSame('object', typeof C4.prototype)
assertSame('get yield', C4.prototype.first)


//---------------------------------------------//
var _;
var stringSet;
var C5 = class {
  get [_ = 'str' + 'ing']() { return 'get string'; }
  set [_ = 'str' + 'ing'](param) { stringSet = param; }
};
assertSame('get string', C5.prototype['string']);
C5.prototype['string'] = 'set string';
assertSame('set string', stringSet);


//---------------------------------------------//
var C6 = class {
    *m() { return 42; };
    get #m() { return 'foo'; };
    method() {
        return this.#m;
    };
};
assertSame(42, (new C6()).m().next().value);


//---------------------------------------------//
var C7 = class {
    get #m() { return 'foo'; };
    method() {
        return this.#m;
    };
};
assertSame('foo', (new C7()).method());


//---------------------------------------------//
class C8 {
    a() { return 'A'}
    get ['b']() { return 'Bx'; }
    c() { return 'C'; }
    b() { return 'B'; }
    d() { return 'D'; }
}
assertSame(0, Object.keys(C8.prototype).length);
assertSameContent(['constructor', 'a', 'b', 'c', 'd'], Object.getOwnPropertyNames(C8.prototype))


//---------------------------------------------//
class C9 {
  static a() { return 'A'}
  static ['b']() { return 'B'; }
  static c() { return 'C'; }
  static ['d']() { return 'D'; }
}
assertSameContent(['length', 'name', 'prototype', 'a', 'b', 'c', 'd'], Object.getOwnPropertyNames(C9));


//---------------------------------------------//
var privateFieldHit = 0;
(class { @(function() { privateFieldHit++ }) #x });
assertSame(1, privateFieldHit);


//---------------------------------------------//
tick = 0;
const aSymbol = Symbol()

function sym() { tick++ }
var C17 = class { @sym [aSymbol] }
assertSame(1, tick);


/**
 * The following testcases assert that decorated classes do not raise syntax errors.
 */
class C10 {
    static #foo() {};

    static {
        class K {}
    }
}


//---------------------------------------------//
var staticTick = 40;
class C11 {
    static #foo() {
        staticTick++;
    };

    static {
        @#foo
        class K {}
    }
}


//---------------------------------------------//
class C12 {
    static #\u2118() {
        staticTick++;
    };
    static {
        C12.#\u2118();
      }
}
assertSame(42, staticTick);


//---------------------------------------------//
class C13 {
    static #$; static #_; static #\u{6F}; static #\u2118; static #ZW_\u200C_NJ; static #ZW_\u200D_J; m() { return 42; }
    static $(value) {
        this.#$ = value;
        return this.#$;
    }
    static _(value) {
        this.#_ = value;
        return this.#_;
    }
    static \u{6F}(value) {
        this.#\u{6F} = value;
        return this.#\u{6F};
    }
    static \u2118(value) {
        this.#\u2118 = value;
        return this.#\u2118;
    }
    static ZW_\u200C_NJ(value) {
        this.#ZW_\u200C_NJ = value;
        return this.#ZW_\u200C_NJ;
    }
    static ZW_\u200D_J(value) {
        this.#ZW_\u200D_J = value;
        return this.#ZW_\u200D_J;
      };
}

new C13();
assertSame(1, C13.\u2118(1));
assertSame(2, C13.ZW_\u200C_NJ(2));
assertSame(3, C13.ZW_\u200D_J(3));


//---------------------------------------------//
class C14 {
    accessor foo = 42;
    accessor $;
    accessor _;
    accessor \u{6F};
    accessor \u2118;
    accessor ZW_\u200C_NJ;
    accessor ZW_\u200D_J;
}


//---------------------------------------------//
let foo = {
    x() {}
}
@foo.x
class C15 {}


//---------------------------------------------//
class C16 {
  get [ 'g' ] () {}
}

