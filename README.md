# Course

[rockthejvm.com](https://rockthejvm.com/).

## 14/07

### Here we are

1. Backend project set up
    1) Visual studio + Metals
    2) Doobie, Http4s
2. Add a health endpoint
3. Add minimal configuration
4. Create basic http server layout

### How to
0. check this one for inspiration [full-stack-typelevel-demo](https://github.com/rockthejvm/full-stack-typelevel-demo)
1. if you need something from cats or cats effects - import *, you will never remember, where this classes located
2. to make project constantly compile after save in VS
    new terminal -> sbt -> ~compile
   to run 
    new terminal -> sbt -> runMain com.toloka.cho.admin.Application
    or sbt "runMain com.toloka.cho.admin.Application"
   to make some requests
    new terminal -> http get localhost:8080/health


import pureconfig.generic.derivation.default.* to be able to derive ConfigReader


### Difficulties

1. Which dev env choose
2. Tecnology stack?
3. How do I run this locally to check? 
4. How to structure project

