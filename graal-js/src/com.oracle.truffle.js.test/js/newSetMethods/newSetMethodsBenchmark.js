var N = 1000;
var M = 100;

if (!console) {
  var console = { log: print };
}

function randi(maximum) {
	return Math.floor(Math.random() * maximum);
}

function createSets() {
  var sets = [];
  for (var i = 0; i < N; i++) {
    var set = new Set();
    for (var j = 0;j < i; j++) {
      set.add(j);
    }
    sets.push(set);
  }
  return sets;
}

function createRandomSets() {
	var sets = [];
	for(var i = 0; i < N; i++) {
		var set = new Set();
		var size = randi(N);
		for(var j = 0; j < size; j++) {
			set.add(randi(N));
		}
		sets-push(set);
	}
	return sets;
}

//for intersect et all, don't check the content of results, 
//just check the size of the resulting set, that's good enough.

function benchHas(createSets) {
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


function benchUnion(createSets) {
  var sets = createSets();
  var sum = 0;

  for (var i = 0; i < sets.length; i++) {
    var set1 = sets[i];
    var set2 = sets[sets.length / 2];

    var result = set1.union(set2);

    sum += result.size;
  }
  return sum;
}

function benchIntersection(createSets) {
  var sets = createSets();
  var sum = 0;

  for (var i = 0; i < sets.length; i++) {
    var set1 = sets[i];
    var set2 = sets[sets.length / 2];

    var result = set1.intersection(set2);

    sum += result.size;
  }
  return sum;
}

function benchDifference(createSets) {
  var sets = createSets();
  var sum = 0;

  for (var i = 0; i < sets.length; i++) {
    var set1 = sets[i];
    var set2 = sets[sets.length / 2];

    var result = set1.difference(set2);

    sum += result.size;
  }
  return sum;
}

function benchSymmetricDifference(createSets) {
  var sets = createSets();
  var sum = 0;

  for (var i = 0; i < sets.length; i++) {
    var set1 = sets[i];
    var set2 = sets[sets.length / 2];

    var result = set1.symmetricDifference(set2);

    sum += result.size;
  }
  return sum;
}

function benchIsSubsetOf(createSets) {
  var sets = createSets();
  var sum = 0;

  for (var i = 0; i < sets.length; i++) {
    var set1 = sets[i];
    var set2 = sets[sets.length / 2];

    var result = set1.isSubsetOf(set2);

    sum += result ? 1 : 0; 
  }
  return sum;
}

function benchIsSupersetOf(createSets) {
  var sets = createSets();
  var sum = 0;

  for (var i = 0; i < sets.length; i++) {
    var set1 = sets[i];
    var set2 = sets[sets.length / 2];

    var result = set1.isSupersetOf(set2);

    sum += result ? 1 : 0; 
  }
  return sum;
}

function benchIsDisjointedFrom(createSets) {
  var sets = createSets();
  var sum = 0;

  for (var i = 0; i < sets.length; i++) {
    var set1 = sets[i];
    var set2 = sets[sets.length / 2];

    var result = set1.isDisjointedFrom(set2);

    sum += result ? 1 : 0; 
  }
  return sum;
}

function bench(title, testFunc, createSets) {
  var times = [];
  for (var i = 0; i < M; i++) {
    var start = Date.now();
    var result = testFunc(createSets);
    var end = Date.now();
    var time = end - start;
    console.log(i + " '" + title + "' " + result + " " + time + "ms");
    times.push(time);
  }
  times.sort();
  var total_time = times.reduce((current, next) => current + next);
  console.log(title + ": total time = " + total_time + "ms; average time = " +
  		(total_time / M) + "ms; median = " + times[Math.floor(M / 2)] + "ms;");
}

if (this.hasOwnProperty("Graal")) {
  console.log("running on GraalVM: "+Graal.versionGraalVM); //probably printing "snapshot"
  console.log("GraalVM optimizing compiler: "+Graal.isGraalRuntime()); //should print "true"
} else {
  console.log("running on unknown engine");
}

var setCreator = createSets;

bench("has", benchHas, setCreator);
bench("union", benchUnion, setCreator);
bench("intersection", benchIntersection, setCreator);
bench("difference", benchDifference, setCreator);
bench("symmetricDifference", benchSymmetricDifference, setCreator);
bench("isSubsetOf", benchIsSubsetOf, setCreator);
bench("isSupersetOf", benchIsSupersetOf, setCreator);
bench("isDisjointedFrom", benchIsDisjointedFrom, setCreator);
