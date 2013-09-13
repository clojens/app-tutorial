
I wrote the little theoretical essay below to explore my thoughts on how dynamic (Clojure)
CSS (garden) could still be useful in a dynamic way and would fall into the framework of Pedestal.

This started with some of my reserves with solutions in the CSS playing field of modern day web.
I think with this I've solved some of my problems with component.js although I'm still
not able to grasp the entire scope of everything just yet.  This is a very raw draft / work in
progress.

;; The story, a start

Probably the easiest to start with, also as a reader, is to envision how you'd normally
work with something that we'll name 'themes'. Themes really pose a web developer for
some fundamental issues. Pre-mature optimization also plays a role, of course.

The idea is as follow: we want to have a clean structure in our files, so we separate out
the structure, which still has content, from the style. We kill tables because feel this
is often more stylish of nature, compared to a div. Why, is hard to tell and we still allow
the style attribute in our structure. Instead we lift out the style to its own file and
for some time, everything is a-o-k. But we notice weird stuff apply to CASCADING style
rules, is that that little artifact 'style' attribute in our structural elements, supersedes
the ones we have in our files. And of course we still know the <style> element which
is placed somewhere in between. Even worse, our idea of #ID is often twisted, better
it is (so we are told) to abolish them completely from our lives, and use classes instead.
The advantage is clear, #id overides .classes so often when working with other peoples
stuff, its a cause of unexpected behavior since a lot of developers/tools aren't aware of
the cascading nature of style sheets. For most modern, average scale developer needs, the
story pretty much ends here. Once a site grows better, more components are added to it,
often jquery plugins, which have their own (sometimes rather large) style definitions.
This is where stuff, for that designer, really can become a pain. Different customers
want their own corporative design/style and thus everything has to be changed to reflect
that. At the least, this means changing colors, logos and such that it fits.

I've read some experiences of people working on the HTML of big companies/organizations
like Mozilla. You weren't allowed to touch any of the markup and this was logical
of course. Any system will break at sensitive points and since you don't want your
styles scattered all over the place, the next logical thing was to hang everything
on classes in the markup and changing little things could mess up the whole view
of the web site (what the outside world/layman would at least define as 'the web site').

The CSS is posed as having less problems by itself, on the edges with the HTML it might
break, but the style rules don't have to change too often once in place. This is good
for the people maintaining it, but bad for the ones who have to write it for a living,
since those would move from styling job to styling job. Then there are people who do
everything themselves as well, I like to think the very best of them use Clojure.
It's a niche definitely.

The Clojure side of things

Its obvious. Traditional side of web development has advanced since its ground days,
and a lot of things have indeed 'cleaned up' this way. I've done it a few times using
site generators in Node.js, Ruby etc. It's tempting and almost I would have sticked,
if optimization didn't stick its head around the corner. Finally it was time to shift
my focus from 'workflow' optimization (from Windows, Visual Studio, I went to Linux,
from bash to zsh, from vim to Light Table/Sublime, Debian to Arch and Xmonad now)
to finally start thinking about optimization and the conclusion was simply:
no way in hell node.js can live up to what Clojure and ClojureScript are doing.
I had to write many stuff for the whole build process that I was far from happy with,
single threaded, too much libraries made by amateurs, non optimized JS to begin with,
basically just another jungle, and a suboptimal end result.

See, what differs one site from another is easily seen when looking at the source code.
I was proud to custom build and name web sites (classes, styles etc.) and found Twitter
Bootstrap still to carry tons of stuff in a monolythic jquery like way but then for
widgets in CSS. Although I can partially relate to that (from ease of use, timesaving
perspective, DRY) it's still very much non-optimized. A online form is supposed to
fill that gap where you can select the widgets you want, then generate a file based
on that. Pretty much that would be a equivalent of some of the things we are trying to
do here.

Clojure widgets with ClojureScript, Garden and Hiccup really make sense in my book,
to what extent and how exactly is left to explore and the options you'd be able to
express that in Pedestal vary somewhat. Really what we need is to have some meaningful
discussion to see what might be good ideas.

Sane defaults, computation, styling and structure, plus content, computed through
graphs will really be able to succintly capture the things, abstractions, that make
up a very abstract concept of 'web site', and break that apart in a useful, dynamic
way.



;; Physical access and caching

For one I imagine it is easiest
to access CSS styles based on a path that can be automagically generates. The
path can influince the generated CSS because we can extract the query properties
from the request, we can use that easily for benefit of e.g. a certain theme.
http://localhost:8080/assets/css/main?theme=basic ~> ring ~> dispatch ~> graph <- css-gen
Given that one such path wouldn't change too often, the browser caching would work
like normal and this method shouldn't pose a bottleneck. One could always just serve
static file content through means of either reading or sharing but what fun is that?

Now one problem would be that when you would change the CSS, it would be additionally
extra HTTP calls to GET it, and thats why many developers cram everything in 1 single
CSS file that is concatenated (optimized) or not but often generic enough to be able
and use it in a single site perhaps even with different styles for subdomains.
Often this boils down to how often the CSS changes. Highly modern frontier / fashion
oriented sites would change as fashion changes or their subcultures ideal does.

In fact, its probably better to speak of a single monolithic CSS file in those cases,
but many sites e.g. those that have demo code would use multiple files to seperate those
concerns for the author/maintainer of those demonstrations. Other sites use different
<link> elements in the head to reference external files that have theme based styling
and so on.

Of course, most of this could also be done on the client side of things:
http://www.javascriptkit.com/javatutors/loadjavascriptcss.shtml
Which relies on client side DOM manipulation through JavaScript in the browser and
adds new <link> elements to the <head> node, or removes others.

But since I've been out of this game for long, and only know Pedestal for a bit, I'm not
entirely sure about some other things. Essentially, Pedestal also is about DOM manipulation
be it through some sophisticated means of messaging vectors and decoupling the main
parts of what make up abstract concepts like 'a web site' into what engineers really were
looking for in all those cases they found their creations (when scaled and advanced)
become a terrible mess (the soup).

So I feel we could essentially do, if you'd like, it in a very similiar manner with styles.
There is a big difference to the front-end though. You'll notice it once you venture in
domains of Google Closure Library - basically it means modify or die. Either you follow
their rules, or choke that the JS won't work in compiled form since you are missing the
references. One advantage HTML has always had, was that it facilitated cargo-cult coding
(copy/paste, no thinking required). Since that doesn't seem to fit our potential audience
too much, I'll leave this point to be for now. We've basically given that up when using
hiccup over HTML, garden over CSS doesn't change that.

I have to shift my thoughts to try and place myself in my developer role again though.
Is this something I would seek from a RAD perspective? Copy/paste (with full understanding)
does save us a lot of time and it might not be something we need to do as original work
every time. It really begs for widget-like structures to wrap and contain HTML/CSS/JS
and be able to have them work as tags placeable on the big board that is a page. Moreover
I've become convinced (more dogmatic I guess) that there *is* a difference in the CSS grid
approach. Mere simple fact: I have to use less markup if I use the semantic.gs grid although,
I must admit, Twitter Bootstrap did try to clean it up a bit more by doing
http://getbootstrap.com/css/#grid-less

https://github.com/component/component did address part of this problem as well, although
now I am more inclined to feel a bit different when looking at it from the Pedestal perspective.
This is simple: component is motivated by universal interoperability. HTML/CSS/JS. We just
concluded this might not necessarily be the right approach for Clojure. We use ClojureScript
instead to generate JS, by extension we do the same to HTML/CSS when we use Garden/Hiccup.

How does Pedestal fit in all this? Well, what Pedestal does is that it transforms 'the DOM'
based on messages sent to components. The tools you use, are files (JS/HTML but not CSS)
generated by the tools components, but what the developer would use for HTML are templates
with often a single point of dynamic entry (the edges) and a regular static CSS file served
through a public folder.

Perhaps a next logical step would be to control the construction of widgets, through dynamic
input in a factory perhaps, and allow for those to be modified by messages. These widgets in turn
would prove very much copy-paste friendly and can be bundled for reuse within Pedestal.

With that in place, you could do the whole round trip, with or without a service to generate
the templates, the messages could do that and you'd have a generic template-service in the
air to listen and respond to MESSAGES, instead of raw code like I did here. The CSS sheet is,
after all, just another tree we can play around with freely.





