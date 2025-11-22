/**
 * @option js.extractors
 */

load('../assert.js');

class Point {
    constructor(x, y) {
        this.x = x;
        this.y = y;
    }

    static [Symbol.customMatcher](point) {
        return [point.x, point.y]
    }
}

{
    function f(Point(x, y)) {
        return { x, y };
    }

    const { x, y } = f(new Point(-Infinity, 1337));
    assertSame(-Infinity, x);
    assertSame(1337, y);
}