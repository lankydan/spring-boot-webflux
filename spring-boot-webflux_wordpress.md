So Spring Boot 2.0 when GA recently, so I decided to write my first post about Spring for quite a while. Since the release I have been seeing more and more mentions of Spring WebFlux along with tutorials on how to use it. But after reading through them and trying to get it working myself, I found it a bit hard to make the jump from the code included in the posts and tutorials I read to writing code that actually does something a tiny bit more interesting than returning a string from the back-end. Now, I'm hoping I'm not shooting myself in the foot by saying that as you could probably make the same criticism of the code I use in this post, but here is my attempt to give a tutorial of Spring WebFlux that actually resembles something that you might use in the wild.

Before I continue, and after all this mentioning of WebFlux, what actually is it? Spring WebFlux is a fully non-blocking reactive alternative to Spring MVC. It allows better vertical scaling without increasing your hardware resources. Being reactive it now makes use of Reactive Streams to allow asynchronous processing of data returned from calls to the server. This means we are going to see a lot less <code>List</code>s, <code>Collection</code>s or even single objects and instead their reactive equivalents such as <code>Flux</code> and <code>Mono</code> (from Reactor). I'm not going to go to in depth on what Reactive Streams are, as honestly I need to look into it even more myself before I try to explain it to anyone. Instead lets get back to focusing on WebFlux.

I used Spring Boot to write the code in this tutorial as usual.

Below are the dependencies that I used in this post.

[gist https://gist.github.com/lankydan/756297e0ce725b4652ccb012503c96af /]

Although I didn't include it in the dependency snippet above, the <code>spring-boot-starter-parent</code> is used, which can finally be upped to version <code>2.0.0.RELEASE</code>. Being this tutorial is about WebFlux, including the <code>spring-boot-starter-webflux</code> is obviously a good idea. <code>spring-boot-starter-data-cassandra-reactive</code> has also been included as we will be using this as the database for the example application as it is one of the few databases that have reactive support (at the time of writing). By using these dependencies together our application can be fully reactive from front to back.

WebFlux introduces a different way to handle requests instead of using the <code>@Controller</code> or <code>@RestController</code> programming model that is used in Spring MVC. But, it does not replace it. Instead it has been updated to allow reactive types to be used. This allows you to keep the same format that you are used to writing with Spring but with a few changes to the return types so <code>Flux</code>s or <code>Mono</code>s are returned instead. Below is a very contrived example.

[gist https://gist.github.com/lankydan/622339d4438afe4ba1015635aed149c9 /]

To me this looks very familiar and from a quick glance it doesn't really look any different from your standard Spring MVC controller, but after reading through the methods we can see the different return types from what we would normally expect. In this example <code>PersonRepository</code> must be a reactive repository as we have been able to directly return the results of their search queries, for reference, reactive repositories will return a <code>Flux</code> for collections and a <code>Mono</code> for singular entities.

The annotation method is not what I want to focus on in this post though. It's not cool and hip enough for us. There isn't enough use of lambdas to satisfy our thirst for writing Java in a more functional way. But Spring WebFlux has our backs. It provides an alternative method to route and handle requests to our servers that lightly uses lambdas to write router functions. Let's take a look at an example.

[gist https://gist.github.com/lankydan/fb84804c2d7dda73c611bbbcdc6f7d08 /]

These are all the routes to methods in the <code>PersonHandler</code> which we will look at later on. We have created a bean that will handle our routing. To setup the routing functions we use the well named <code>RouterFunctions</code> class providing us with a load of static methods, but for now we are only interested with it's <code>route</code> method. Below is the signature of the <code>route</code> method.

[gist https://gist.github.com/lankydan/faa6c8dfdbc8900cb372cb366cba03d0 /]

The method shows that it takes in a <code>RequestPredicate</code> along with a <code>HandlerFunction</code> and outputs a <code>RouterFunction</code>. 

The <code>RequestPredicate</code> is what we use to specify behavior of the route, such as the path to our handler function, what type of request it is and the type of input it can accept. Due to my use of static imports to make everything read a bit clearer, some important information has been hidden from you. To create a <code>RequestPredicate</code> we should use the <code>RequestPredicates</code> (plural), a static helper class providing us with all the methods we need. Personally I do recommend statically importing <code>RequestPredicates</code> otherwise you code will be a mess due to the amount of times you might need to make use of <code>RequestPredicates</code> static methods. In the above example, <code>GET</code>, <code>POST</code>, <code>PUT</code>, <code>DELETE</code>, <code>accept</code> and <code>contentType</code> are all static <code>RequestPredicates</code> methods.

The next parameter is a <code>HandlerFunction</code>, which is a Functional Interface. There are three pieces of important information here, it has a generic type of <code>&lt;T extends ServerResponse&gt;</code>, it's <code>handle</code> method returns a <code>Mono&lt;T&gt;</code> and it takes in a <code>ServerRequest</code>. Using these we can determine that we need to pass in a function that returns a <code>Mono&lt;ServerResponse&gt;</code> (or one of it's subtypes). This obviously places a heavy constraint onto what is returned from our handler functions as they must meet this requirement or they will not be suitable for use in this format.

Finally the output is a <code>RouterFunction</code>. This can then be returned and will be used to route to whatever function we specified. But normally we would want to route lots of different requests to various handlers at once, which WebFlux caters for. Due to <code>route</code> returning a <code>RouterFunction</code> and the fact that <code>RouterFunction</code> also has its own routing method available, <code>andRoute</code>, we can chain the calls together and keep adding all the extra routes that we require.

If we take another look back at the <code>PersonRouter</code> example above, we can see that the methods are named after the REST verbs such as <code>GET</code> and <code>POST</code> that define the path and type of requests that a handler will take. If we take the first <code>GET</code> request for example, it is routing to <code>/people</code> with a path variable name <code>id</code> (path variable denoted by <code>{id}</code>) and the type of the returned content, specifically <code>APPLICATION_JSON</code> (static field from <code>MediaType</code>) is defined using the <code>accept</code> method. If a different path is used, it will not be handled. If the path is correct but the Accept header is not one of the accepted types, then the request will fail.

Before we continue I want to go over the <code>accept</code> and <code>contentType</code> methods. Both of these set request headers, <code>accept</code> matches to the Accept header and <code>contentType</code> to Content-Type. The Accept header defines what Media Types are acceptable for the response, as we were returning JSON representations of the <code>Person</code> object setting it to <code>APPLICATION_JSON</code> (<code>application/json</code> in the actual header) makes sense. The Content-Type has the same idea but instead describes what Media Type is inside the body of the sent request. That is why only the <code>POST</code> and <code>PUT</code> verbs have <code>contentType</code> included as the others do not have anything contained in their bodies. <code>DELETE</code> does not include <code>accept</code> and <code>contentType</code> so we can conclude that it is neither expecting anything to be returned nor including anything in its request body.

Now that we know how to setup the routes, lets look at writing the handler methods that deal with the incoming requests. Below is the code that handles all the requests from the routes that were defined in the earlier example.

[gist https://gist.github.com/lankydan/2558d7a040f647d89d619f554be85a73 /]

One thing that is quite noticeable, is the lack of annotations. Bar the <code>@Component</code> annotation to auto create a <code>PersonHandler</code> bean there are no other Spring annotations.

I have tried to keep most of the repository logic out of this class and have hidden any references to the entity objects by going via the <code>PersonManager</code> that delegates to the <code>PersonRepository</code> it contains. If you are interested in the code within <code>PersonManager</code> then it can be seen here on my <a href="https://github.com/lankydan/spring-boot-webflux/blob/master/src/main/java/com/lankydanblog/tutorial/person/PersonManager.java" target="_blank" rel="noopener">GitHub</a>, further explanations about it will be excluded for this post so we can focus on WebFlux itself.

Ok, back to the code at hand. Let's take a closer look at the <code>get</code> and <code>post</code> methods to figure out what is going on.

[gist https://gist.github.com/lankydan/f5811875a4d03a6c95c108b0fb44b9b0 /]

This method is for retrieving a single record from the database that backs this example application. Due to Cassandra being the database of choice I have decided to use an <code>UUID</code> for the primary key of each record, this has the unfortunate effect of making testing the example more annoying but nothing that some copy and pasting can't solve. 

Remember that a path variable was included in the path for this <code>GET</code> request. Using the <code>pathVariable</code> method on the <code>ServerRequest</code> passed into the method we are able to extract it's value by providing the name of the variable, in this case <code>id</code>. The ID is then converted into a <code>UUID</code>, which will throw an exception if the string is not in the correct format, I decided to ignore this problem so the example code doesn't get messier.

Once we have the ID, we can query the database for the existence of a matching record. A <code>Mono&lt;Person&gt;</code> is returned which either contains the existing record mapped to a <code>Person</code> or it left as an empty <code>Mono</code>. 

Using the returned <code>Mono</code> we can output different responses depending on it's existence. This means we can return useful status codes to the client to go along with the contents of the body. If the record exists then <code>flatMap</code> returns a <code>ServerResponse</code> with the <code>OK</code> status. Along with this status we want to output the record, to do this we specify the content type of the body, in this case <code>APPLICATION_JSON</code>, and add the record into it. <code>fromPublisher</code> takes our <code>Mono&lt;Person&gt;</code> (which is a <code>Publisher</code>) along with the <code>Person</code> class so it knows what it is mapping into the body. <code>fromPublisher</code> is a static method from the <code>BodyInserters</code> class.

If the record does not exist, then the flow will move into the <code>switchIfEmpty</code> block and return a <code>NOT FOUND</code> status. As nothing is found, the body can be left empty so we just create the <code>ServerResponse</code> there are then.

Now onto the <code>post</code> handler.

[gist https://gist.github.com/lankydan/5406fd6316a4050e72287d1ffd3e21a9 /]

Even just from the first line we can see that it is already different to how the <code>get</code> method was working. As this is a <code>POST</code> request it needs to accept the object that we want to persist from the body of the request. As we are trying to insert a single record we will use the request's <code>bodyToMono</code> method to retrieve the <code>Person</code> from the body. If you were dealing with multiple records you would probably want to use <code>bodyToFlux</code> instead.

We will return a <code>CREATED</code> status using the <code>created</code> method that takes in a <code>URI</code> to determine the path to the inserted record. It then follows a similar setup as the <code>get</code> method by using the <code>fromPublisher</code> method to add the new record to the body of the response. The code that forms the <code>Publisher</code> is slightly different but the output is still a <code>Mono&lt;Person&gt;</code> which is what matters. Just for further explanation about how the inserting is done, the <code>Person</code> passed in from the request is mapped to a new <code>Person</code> using the <code>UUID</code> we generated and is then passed to <code>save</code> by calling <code>flatMap</code>. By creating a new <code>Person</code> we only insert values into Cassandra that we allow, in this case we do not want the <code>UUID</code> passed in from the request body.

So, that's about it when it comes to the handlers. Obviously there other methods that we didn't go through. They all work differently but all follow the same concept of returning a <code>ServerResponse</code> that contains a suitable status code and record(s) in the body if required.

We have now written all the code we need to get a basic Spring WebFlux back-end up a running. All that is left is to tie all the configuration together, which is easy with Spring Boot.

[gist https://gist.github.com/lankydan/8e69212fd861e628cf016d7067350057 /]

Rather than ending the post here we should probably look into how to actually make use of the code.

Spring provides the <code>WebClient</code> class to handle requests without blocking. We can make use of this now as a way to test the application, although there is also a <code>WebTestClient</code> which we could use here instead. The <code>WebClient</code> is what you would use instead of the blocking <code>RestTemplate</code> when creating a reactive application.

Below is some code that calls the handlers that were defined in the <code>PersonHandler</code>.

[gist https://gist.github.com/lankydan/56808561fd389885c4ba9077b80e0215 /]

Don't forget to instantiate the <code>Client</code> somewhere, below is a nice lazy way to do it!

[gist https://gist.github.com/lankydan/d28bf38b7c54e8687cee7329f56ef284 /]

First we create the <code>WebClient</code>.

[gist https://gist.github.com/lankydan/7b44b1ef4e1768f6095f6dfc2d11a27b /]

Once created we can start doing stuff with it, hence the <code>doStuff</code> method.

Let's break down the <code>POST</code> request that is being send to the back-end.

[gist https://gist.github.com/lankydan/73c157dbf260ffeeee28e787f530958d /]

I wrote this one down slightly differently so you can see that a <code>Mono&lt;ClientResponse&gt;</code> is returned from sending a request. The <code>exchange</code> method fires the HTTP request over to the server. The response will then be dealt with whenever the response arrives, if it ever does.

Using the <code>WebClient</code> we specify that we want to send a <code>POST</code> request using the <code>post</code> method of course. The <code>URI</code> is then added with the <code>uri</code> method (overloaded method, this one takes in a <code>String</code> but another accepts a <code>URI</code>). Im tired of saying this method does what the method is called so, the contents of the body are then added along with the Accept header. Finally we send the request by calling <code>exchange</code>. 

Note that the Media Type of <code>APPLICATION_JSON</code> matches up with the type defined in the <code>POST</code> router function. If we were to send a different type, say <code>TEXT_PLAIN</code> we would get a <code>404</code> error as no handler exists that matches to what the request is expecting to be returned.

Using the <code>Mono&lt;ClientResponse&gt;</code> returned by calling <code>exchange</code> we can map it's contents to our desired output. In the case of the example above, the status code is printed to the console. If we think back to the <code>post</code> method in <code>PersonHandler</code>, remember that it can only return the "Created" status, but if the sent request does not match up correctly then "Not Found" will be printed out.

Let's look at one of the other requests.

[gist https://gist.github.com/lankydan/f5e6f5bfeccad877498e6cd98639c33d /]

This is our typical <code>GET</code> request. It looks pretty similar to the <code>POST</code> request we just went through. The main differences are that <code>uri</code> takes in both the path of the request and the <code>UUID</code> (as a <code>String</code> in this case) as a parameter to that will replace the path variable <code>{id}</code> and that the body is left empty. How the response is handled is also different. In this example it extracts the body of the response and maps it to a <code>Mono&lt;Person&gt;</code> and prints it out. This could have been done with the previous <code>POST</code> example but the status code of the response was more useful for it's scenario.

For a slightly different perspective, we could use cURL to make requests and see what the response looks like.
<pre>
CURL -H "Accept:application/json" -i localhost:8080/people
</pre>

<pre>
HTTP/1.1 200 OK
transfer-encoding: chunked
Content-Type: application/json

[
  {
      "id": "13c403a2-6770-4174-8b76-7ba7b75ef73d",
      "firstName": "John",
      "lastName": "Doe",
      "country": "UK",
      "age": 50
  },
  {
      "id": "fbd53e55-7313-4759-ad74-6fc1c5df0986",
      "firstName": "Peter",
      "lastName": "Parker",
      "country": "US",
      "age": 50
  }
]
</pre>
The response will look something like this, obviously it will differ depending on the data you have stored.

Note the response headers.
<pre>
transfer-encoding: chunked
Content-Type: application/json
</pre>
The <code>transfer-encoding</code> here represents data that is transferred in chunks that can be used to stream data. This is what we need so the client can act reactively to the data that is returned to it.

I think that this should be a good place to stop. We have covered quite a lot of material here which has hopefully helped you understand Spring WebFlux better. There are a few other topics I want to cover about WebFlux but I will do those in separate posts as I think this one is long enough as it is.

In conclusion, in this post we very briefly discussed why you would want to use Spring WebFlux over a typical Spring MVC back-end. We then looked at how to setup routes and handlers to process the incoming requests. The handlers implemented methods that could deal with most of the REST verbs and returned the correct data and status codes in their responses. Finally we looked at two ways to make requests to the back-end, one using a <code>WebClient</code> to process the output directly on the client side and another via cURL to see what the returned JSON looks like.

If you are interested in looking at the rest of the code I used to create the example application for this post, it can be found on my <a href="https://github.com/lankydan/spring-boot-webflux" target="_blank" rel="noopener">GitHub</a>.

As always if you found this post helpful, please share it and if you want to keep up with my latest posts then you can follow me on Twitter at <a href="https://twitter.com/LankyDanDev" target="_blank" rel="noopener">@LankyDanDev</a>.