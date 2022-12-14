/**
 * Symbols as WeakMap keys proposal.
 *
 * @option ecmascript-version=staging
 */


load('assert.js');
//Testing valid symbol as key
const weak = new WeakMap();


const key = Symbol('my ref');
const obj = {};

weak.set(key, obj);

assertSame(obj, weak.get(key));

//Testing for invalid, registered symbol
const key2 = Symbol.for('name');

assertThrows(() => weak.set(key2, obj), TypeError);


