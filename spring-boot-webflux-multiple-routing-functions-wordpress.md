In this post we will be looking at defining multiple router functions to different logical domains in Spring WebFlux. This might not be a problem if you are creating "Microservices" as you will most likely only be working within a single domain for each service, but if you are not then you will likely have the need to include multiple domains within your application that users or your own services can interact with. The code to do this is as simple as I hoped it would be and could be explained in a few sentences. To make this post a little more interesting we will look at some of the Spring code that makes this all possible.

If you are new to WebFlux I recommend having a look at my previous post, [Doing stuff with Spring WebFlux](https://lankydanblog.com/2018/03/15/doing-stuff-with-spring-webflux/), where I wrote some thorough examples and explanations on the subject.

So lets set the scene first. You have two different domains within your application, say people and locations. You might want to keep them separated from each other not only logically but also within your code. To do so you need a way to define your routes in isolation from each others domain. That is what we will look at in this post.

If you think you already know the answer to this problem, then you are probably right. It really is that simple. Let's work our way up to it though. To create routes for just the people domain create a <code>RouterFunction</code> bean that maps to the relevant handler functions, like below.

[gist https://gist.github.com/lankydan/7c8b64f38e9ebb5e56cd9aa05b934369 /]

This creates the routes to the various handler functions in the <code>PersonHandler</code>.

So, now we want to add the routes for the location logic. We could simply add the routes to this bean, like below.

[gist https://gist.github.com/lankydan/f12d8ee9a1d7753a21ceb0c48451c85a /]

The bean is now including a reference to the <code>LocationHandler</code> so the location route can be setup. The problem with this solution is that it requires the code to be coupled together. Furthermore, if you need to add even more handlers you are soon going to be overwhelmed with the amount of dependencies being injected into this bean.

The way around this, is to create multiple <code>RouterFunction</code> beans. That's it. So, if we create one in the people domain, say <code>PersonRouter</code> and one in the location domain named <code>LocationRouter</code>, each can define the routes that they need and Spring will do the rest. This works because Spring goes through the application context and finds or creates any <code>RouterFunction</code> beans and consolidates them into a single function for later use.

Using this information we can write the code below.

[gist https://gist.github.com/lankydan/648031b17d8124bb4398218e550cd8b0 /]

and

[gist https://gist.github.com/lankydan/ce6ec1f4517b5215efeb756b9da70632 /]

<code>PersonRouter</code> can be kept with other people / person related code and <code>LocationRouter</code> can do the same.

To make this more interesting, why does this work?

<code>RouterFunctionMapping</code> is the class that retrieves all the <code>RouterFunction</code> beans created within the application context. The <code>RouterFunctionMapping</code> bean is created within <code>WebFluxConfigurationSupport</code> which is the epicenter for Spring WebFlux configuration. By including the <code>@EnableWebFlux</code> annotation on a configuration class or by relying on auto-configuration, a chain of events starts and collecting all of our <code>RouterFunction</code>s is one of them.

Below is the <code>RouterFunctionMapping</code> class. I have removed it's constructors and a few methods to make the snippet here a bit easier to digest.

[gist https://gist.github.com/lankydan/0b6a3eea2335a9e63b6d517503581097 /]

The path to retrieving all the routes starts in <code>afterPropertiesSet</code> that is invoked after the <code>RouterFunctionMapping</code> bean is created. As it's internal <code>RouterFunction</code> is <code>null</code> it calls <code>initRouterFunctions</code> triggering a series of methods leading to the execution of <code>routerFunctions</code>. A new <code>SortedRouterFunctionsContainer</code> is constructed (private static class) setting it's <code>routerFunctions</code> field by injecting all <code>RouterFunction</code>s from the application context. This works since Spring will inject all beans that of type <code>T</code> when a <code>List&lt;T&gt;</code> is being injected. The now retrieved <code>RouterFunction</code>s are combined together to make a single <code>RouterFunction</code> that is used from now on to route all incoming requests to the appropriate handler.

That's all there is to it. In conclusion defining multiple <code>RouterFunction</code>s for different business domains is very simple as you just create them in whatever area they most make sense and Spring will go off and fetch them all. To demistify some of the magic we looked into <code>RouterFunctionMapping</code> to see how the <code>RouterFunction</code>s we create are collected and combined so that they can be used to route requests to handlers. As a closing note, I do understand that this post in some respects is quite trivial but sometimes the seemingly obvious information can be pretty helpful.

If you have not done so already, I recommend looking at my previous post [Doing stuff with Spring WebFlux](https://lankydanblog.com/2018/03/15/doing-stuff-with-spring-webflux/).

Finally, if you found this post helpful and would like to keep up with my new posts as I write them, then you can follow me on Twitter at <a href="https://twitter.com/LankyDanDev" target="_blank" rel="noopener">@LankyDanDev</a>.