var N = 1000;
var M = 100;

if (!console) {
  var console = { log: print };
}

function createSets() {
  var sets = [];
  for (var i=0;i<N;i++) {
    var set = new Set();
    for (var j=0;j<i;j++) {
      set.add(j);
    }
    sets.push(set);
  }
  return sets;
}

//for intersect et all, don't check the content of results, 
//just check the size of the resulting set, that's good enough.

function benchHas() {
  var sets = createSets();
  var sum=0;

  for (var i=0;i<sets.length;i++) {
    var set1=sets[i];
    var set2=sets[sets.length/2];

    var has1 = set1.has(i);
    var has2 = set2.has(i);

    sum += has1?1:0 + has2?1:0; 
  }
  return sum;
}


function benchUnion(sets) {
  var sets = createSets();
  var sum = 0;

  for (var i = 0; i < sets.length; i++) {
    var set1 = sets[i];
    var set2 = sets[sets.length / 2];

    var union = set1.union(set2);

    sum += has1 ? 1: 0 + has2 ? 1 : 0;
  }
  return sum;
}


function bench(title, fn) {
  for (var i=0;i<M;i++) {
    var start=Date.now();
    var result = fn();
    var end=Date.now();
    console.log(i+" '"+title+"' "+result+" "+(end-start)+"ms");
  }
}

if (this.hasOwnProperty("Graal")) {
  console.log("running on GraalVM: "+Graal.versionGraalVM); //probably printing "snapshot"
  console.log("GraalVM optimizing compiler: "+Graal.isGraalRuntime()); //should print "true"
} else {
  console.log("running on unknown engine");
}

bench("has", benchHas);


