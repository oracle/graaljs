local graalJs = import 'graal-js/ci.jsonnet';
local graalNodeJs = import 'graal-nodejs/ci.jsonnet';

{
  builds: graalJs.builds + graalNodeJs.builds,
}
