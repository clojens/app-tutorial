# cors demo

In this sample app, we will demonstrate an implementation of [cross-origin resource sharing](http://en.wikipedia.org/wiki/Cross-origin_resource_sharing), using of course...Pedestal.

In order to fully demonstrate this feature, as the name sort of implies, we must first create
a situation where we have separate domain origins. We will do this by simply starting a server
and service on the local machine, one using port `8080`, the other `8081`.

The file `src/blob.html` is a static page with embedded javascript, and is served from `http://localhost:8080/js`. It consumes an Server-side events (SSE) EventSource found at `http://localhost:8081/`. Note that many browsers might actually block remote script execution on your local machine, hence we host the `eventsource.js` file locally from `resources/public` and can be accessed as such. The combination of information found in the `project.clj` file, the vector of string paths at key `:resource-paths` in particular, as well as the map found in `service.clj` by that same name, namely `::bootstrap/resource-path "/public"` combined will give Pedestal enough information to take it from there. Also be advised, when looking at `blob.html` that the document root as far as the static web pages/server goes, is located at `resources/public`, not at `src/`.

In `src/cors/service.clj`, you will find a definition of `cors-interceptor` that adds CORS headers when the origin matches the authorized origin. That's really the magic going on.

## Usage

### From the REPL and browser

To run this sample application, probably the easiest way most people are comfortable with, is to open a (virtual) terminal emulator window
and change the present working directory to this project folder. Then:

1. Type and execute the commands `lein run 8080` and `lein run 8081`
1. Open a browser of your choice.
1. Open the Javascript console, as all output from the sample will be displayed there.
1. Visit [localhost:8080/js](http://localhost:8080/js) to load the event consume, watch the JavaScript console.

The inline JavaScript returned will attempt to access a service on port `8081`, which qualifies as a different origin.
If allowed, the event source passes back an event containing the thread id, which is consumed and displayed in the console.

### From the Light Table editor

What you are expected to see should closely resemble the output as captured in these screenshots, regardless of environment, the response and general
output messages should coincide.

* [screenshot 1](https://github.com/clojens/app-tutorial/blob/refactor/media/new-samples/cors/resources/images/screen1.png?raw=true)
* [screenshot 2](https://github.com/clojens/app-tutorial/blob/refactor/media/new-samples/cors/resources/images/screen2.png?raw=true)
* [screenshot 3](https://github.com/clojens/app-tutorial/blob/refactor/media/new-samples/cors/resources/images/screen3.png?raw=true)

As you may have noticed, instead of using the traditional method of the terminal and a good old `lein run` or `lein repl`, instead I did:

1. added the project folder to the workspace
1. navigated to `src/cors/service.clj` file
1. evaluated the entire editor content (ctrl+shift+enter) so the namespaces get loaded in memory (and the editor connected to the project since this is the first eval we do in the project)
1. next open up the `src/cors/server.clj` file and evaluate all expressions in it (ctrl+shift+enter)
1. finally (temporarily) in the `server.clj` file loaded namespace, evaluate the `-main` expressions as `(-main 8080)` and `(-main 8081)`
1. thats it, open up a browser and navigate to those locations to see the result

## Configuration

To configure logging see config/logback.xml. By default, the app logs to stdout and logs/.
To learn more about configuring Logback, read its [documentation](http://logback.qos.ch/documentation.html).

## Links

* [Other examples](https://github.com/pedestal/samples)

## Credits

This samples uses the `EventSource` polyfill from [Yaffle/EventSource](https://github.com/Yaffle/EventSource).

## License

*Copyright 2013 Relevance, Inc.*

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
which can be found in the file epl-v10.html at the root of this distribution.

By using this software in any fashion, you are agreeing to be bound by
the terms of this license.

You must not remove this notice, or any other, from this software.
