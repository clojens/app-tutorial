Digest of page found at:
<http://alexmarandon.com/articles/web_widget_jquery/>

> What is a web widget?

A web widget is a component, a “chunk of web page” that you provide for other people to display data coming from your site on their own pages. A widget typically contains a slightly complex mixture of HTML, CSS and JavaScript. You will want to hide that complexity and make it as easy as possible for people to include your widget on their pages.

The widget developed in this tutorial can be embedded in a web page with just two HTML tags: one script tag to load the widget and one container tag (usually a div) to indicate where we want to put the widget on the page:

<script src="http://example.com/widget/script.js" type="text/javascript"></script>
<div id="example-widget-container"></div>

> Can’t it be even simpler?

It is technically possible to create a widget that doesn’t require a destination container by using document.write() within the widget’s code. Although some widget providers do use that approach, and it can reduce the code necessary on the host page to just one script tag, we believe it’s not worth it because:

* document.write() cannot be called once a page has loaded.
* by using a separate tag which will contain the widget on the page, we are free to place our script tag anywhere.

---

Now before I continue lets place a few remarks:

* essentially this would be the same for CSS, either it also adds the <link> or this is done through js <script>

---

Code isolation

Because you can’t predict what JavaScript code will be running on the page which uses our widget, we need a way to ensure that it doesn’t clash with any other JavaScript code included on the host page. To do that, we just enclose all our code within an anonymous function and we call that function. The variables we create in our functions won’t interfere with the rest of the page.

---

Yup already knew that.

