##### Preview Warning

This preview document is a rough draft. It may contain incorrect or outdated information, so use at your own risk.

When this doc is published elsewhere, it will be deleted from these pages and replaced with a link to the polished version. Should a document be in need of subjected inspection / review, this will be clearly stated.

##### Table of changes

Notable changes while composing this document:
* upgrade to version 0.2, the separate `dev/dev.clj` and `dev/user.clj` bootstrapped files have probably vanished since this upgrade of pedestal in favor of inlining the code at `project.clj` and relying on a namespace in that portion. Procedure is now `lein repl` first then `(start)` and `README.md` files should probably reflect this soon.

#### Sample: [`auto-reload-server`](https://github.com/pedestal/samples/tree/master/auto-reload-server)
> 31-aug: `Ran 2 tests containing 4 assertions. 0 failures, 0 errors.`

* Has some [**4 tests defined already**](https://github.com/pedestal/samples/tree/master/auto-reload-server/test). Seems sufficient for now. In time perhaps things could be broken up in smaller tests or, perhaps even better, generate some generic/multi-functional tests for common items as web server (possibly different ones in the future though) but also internal components perhaps, like e.g. rendering, emitting etc. where applicable and it would make sense for a end-user to have as a good starting point for their pedestal app testing. This should also help in support issues, narrowing down problems to sub-areas where possible by having end-users execute the tests to see if/where it fails. In this case, if rendering would fail it *could* be the web server, but we wouldn't know for sure until we test for a functioning web server deployment :)

* However, the route/file-change aspect did not get a place in the tests, and let this be the part which was failing. A test, although I didn't run them at first and only really by accident discovered them not working, might have allowed for earlier notification of a problem area as this (if it is a problem see the last point).

* The reason it's not working is because that part which is supposed to keep an eye out, the `(watch)` function, is being  defined and called in the `-main` function - not of `service.clj` but rather in `dev/dev.clj` - and as such, since the `project.clj` file has its `:main` entry pointed at `service.clj`, will never get called.

* After some thought, it seems to me this is desirable. After all, there is a difference between development (when we are still changing files, and thus like to have routes reflect the latest changes on saving the file that defines them) and production (where we are unlikely to change files, and if so, it would be initially in a development environment) and so the `-main` startup sequence a JAR would use eventually, does not really have to have the watch on. Am I right?

* Either way, the README has the advertised method of `lein run` and not `lein repl` so my previous bullet might be a wrong assumption that auto-reloading is off by purpose on production aspects. If not, then why is it in the README? So this is a less obvious example of contradictions that emerge when studying some of the documents, which could benefit from a small, explicit note here and there.

#### Resources: [`images`](https://github.com/pedestal/samples/tree/master/images)

* Don't need tests do they? :)

#### Sample: [`chat-client`](https://github.com/pedestal/samples/tree/master/chat/chat-client) and [`chat-server`](https://github.com/pedestal/samples/tree/master/chat/chat-server)

* I ran a test of the Chat demo application, which works for a great deal up to the point I tried to add my name in the development front-end, then it got angry with me and threw something at my head, some internal error map. I assume you intended for a more complete sample so it's yet another bug/issue I've got to solve... no better way of learning stuff though, tbh I haven't dived in fully yet so I'll keep updating these posts.

* Here's a [screenshot of chat-client-dev.html and server REPL](https://f.cloud.github.com/assets/3727933/1056628/b2fbfdbc-115a-11e3-8c25-8581543eccc8.png) failing on me the first time yesterday. Here a second time today, same error on the sending of a heartbeat. [Screenshot](https://f.cloud.github.com/assets/3727933/1059063/4a845f98-1193-11e3-9b9f-983db53d3f3c.png)

#### Sample: [`CORS`](https://github.com/pedestal/samples/tree/master/cors)

* My browser (chromium) notified failure to execute the (GitHub hosted) `eventsource.js` file since it is being served as a text file (raw). This is a default setting I would assume, since I never set the value of the strict MIME checking manually. So this makes it rather likely others will experience the same difficulties running it as is. This can be solved by hosting the file locally.

* [Screenshot 1: oh noes, the cors sample giving me trouble, please I don't want no trouble cors app](https://f.cloud.github.com/assets/3727933/1059849/6b1b295c-11a3-11e3-902e-1899f4600fca.png)

* To establish this I added the "resources" string to the vector in `project.clj` under the `:resources` key. Next I created the path `resources/public` and downloaded the raw github `eventsource.js` to that folder locally.

* Note that I also had some EOF/Stream exceptions thrown at me several times until I read that this was to be expected with one side closing the connection, or holding the old values etc. So what I did was make sure that everything is done in proper order, pristine conditions so finally I split some terminals to try and everything seems to work fine (since I don't really know if there would be any other output to expect, can someone verify this is the correct behaviour?).

* [Screenshot 2: me chewing bubblegum and cors app back in gear holding hand over its ear](https://f.cloud.github.com/assets/3727933/1059852/84e20f0e-11a3-11e3-95be-13ef4e25e030.png)

* I've also added a few bits and pieces to the readme on my local/remote version, I'll probably run it again sometime soon and then have the other generic paragraphs for README ready to paste in the whole lot at once if I can, but one thing this readme had in particular was the mentioning of a **NOTES.md** file which is nowhere to be found. I guess I could remove it, unless someone knows where it is and/or if it's important enough to include :)

#### Sample: [`helloworld-app`](https://github.com/pedestal/samples/tree/master/helloworld-app)

* Desirable to perhaps move the copyright text blocks to a central, single location consistently throughout all sample project. Here it is all over the place, rather have it in either the README, or a separate LICENSE file, not like in files as simple/tiny as `user.clj` although, if you do prefer also inline blocks, only the user.clj file perhaps. Intermediate solution perhaps, is to just include a few meta fields to the namespace for author/copyright docstrings. Note the reasoning behind it be that these larger chunks of text take up valuable screen real-estate if you have say half a screen window, being on top of the file especially, makes it that you might often have to scroll down before you can start reading the code. And people probably wouldn't remove it (since it tells you not to) which can become a bit of a nuisance over time. Unless of course, you feel that fine grained labeling of this kind to be desirable, in that case, I'd copy them to every file *consistently* at least.

* Doesn't start in Light Table as the other, Clojure based, examples. This one holds everything in a single ClojureScript file which you need to approach differently. The problem is though, that you need to have a browser connection for the ClojureScript, for which `user.clj` bootstraps the whole shebang including the web server hosting the pages, you need to connect to a ClojureScript project (as what I understand of it). Since everything is connected to the `main` entry point, in this case being inside a `.cljs` file, we run into problems. 
[Screenshot: we couldn't connect in LT](https://f.cloud.github.com/assets/3727933/1063339/8a69fcc4-12a8-11e3-8842-df738807cc6a.png)

* I'm playing around a bit to see how nice unit tests for crossover Clojure(Script). One reason I could think of why you'd might want this, is because these could provide useful insights to people on how to actually do that using [cljs test](https://github.com/cemerick/clojurescript.test) for example. Not many projects actually do this, I think, but I don't have much time spent on cljs, let alone projects so no authority there. Actually, I'm thinking it might be a nice occasion to tinker with aspects.

* For the rest, I don't really see much testing besides the more generic facts of: *can we compile to cljs? can we fire a web server? does it fire up pedestal tools? can we navigate to (test)/dataUI or something?* Mentioned this earlier at auto-reload-server. More and more I'm thinking it could be nice to provide the user with some generic tests for common things found in everyday projects, perhaps some will be found in apps, others in services and so on.

#### Sample: [`ring-middleware`](https://github.com/pedestal/samples/tree/master/ring-middleware)

* Contains **1 test** `Ran 1 tests containing 1 assertions. 0 failures, 0 errors.` Does only check for rendering of the home page. Need some thought still perhaps (myself).
