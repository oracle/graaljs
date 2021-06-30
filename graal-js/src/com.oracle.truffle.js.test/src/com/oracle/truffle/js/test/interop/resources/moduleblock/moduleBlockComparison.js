(async function() {
    let moduleBlock = module {
        export let y = 1;
    };

    let moduleExports = await import(moduleBlock);
    console.assert(moduleExports.y === 1, "Module block import/export failed.");

    console.assert(await import(moduleBlock) === moduleExports, "Not equal.");
})();

(async function() {
    const arr = new Array(2);

    for (let i = 0; i < 2; i++) {
        arr[i] = module {};
    }

    console.assert(arr[0] !== arr[1], "Different module blocks are the same.");
    console.assert(await import(arr[0]) !== await import(arr[1]), "Different imported module blocks are the same.");
})();

(async function() {
    const m1 = module {};
    const m2 = m1;

    console.assert(await import(m1) === await import(m2), "The same module block imported twice is not the same.");
})();

// This test can be conducted as soon as the realms proposal: https://github.com/tc39/proposal-realms is implemented
/*

(async function() {
    let moduleBlock = module {
        export let o = Object;
    };

    let m = await import(moduleBlock);
    console.assert(m.o === Object, "O export is not an object.");

    let r1 = new Realm();
    let m1 = await r1.import(moduleBlock);
    console.assert(m1.o === r1.globalThis.Object, "Realm o export is not an object.");
    console.assert(m1.o !== Object, "Realm o export is old realm object.");
    console.assert(m.o !== m1.o, "Both o's are equal.");
})();

*/