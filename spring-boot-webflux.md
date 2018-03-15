So Spring Boot 2.0 when GA recently, so I decided to write my first post about Spring for quite a while. Since the release I have been seeing more and more mentions of Spring WebFlux along with tutorials on how to use it. But after reading through them and trying to get it working myself, I found it a bit hard to make the jump from the code included in the posts and tutorials I read to writing code that actually does something a tiny bit more interesting than returning a string from the back-end. Now, I'm hoping I'm not shooting myself in the foot by saying that as you could probably make the same criticism of the code I use in this post, but here is my attempt to give a tutorial of Spring WebFlux that actually resembles something that you might use in the wild.

Before I continue, and after all this mentioning of WebFlux, what actually is it? Spring WebFlux is a fully non-blocking reactive alternative to Spring MVC. It allows better vertical scaling without increasing your hardware resources. Being reactive it now makes use of Reactive Streams to allow asynchronous processing of data returned from calls to the server. This means we are going to see a lot less `List`s, `Collection`s or even single objects and instead their reactive equivalents such as `Flux` and `Mono` (from Reactor). I'm not going to go to in depth on what Reactive Streams are, as honestly I need to look into it even more myself before I try to explain it to anyone. Instead lets get back to focusing on WebFlux.

I used Spring Boot to write the code in this tutorial as usual.

Below are the dependencies that I used in this post.
```xml
<dependencies>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-cassandra-reactive</artifactId>
    <version>2.0.0.RELEASE</version>
  </dependency>

</dependencies>
```
Although I didn't include it in the dependency snippet above, the `spring-boot-starter-parent` is used, which can finally be upped to version `2.0.0.RELEASE`. Being this tutorial is about WebFlux, including the `spring-boot-starter-webflux` is obviously a good idea. `spring-boot-starter-data-cassandra-reactive` has also been included as we will be using this as the database for the example application as it is one of the few databases that have reactive support (at the time of writing). By using these dependencies together our application can be fully reactive from front to back.

WebFlux introduces a different way to handle requests instead of using the `@Controller` or `@RestController` programming model that is used in Spring MVC. But, it does not replace it. Instead it has been updated to allow reactive types to be used. This allows you to keep the same format that you are used to writing with Spring but with a few changes to the return types so `Flux`s or `Mono`s are returned instead. Below is a very contrived example.
```java
@RestController
public class PersonController {

  private final PersonRepository personRepository;

  public PersonController(PersonRepository personRepository) {
    this.personRepository = personRepository;
  }

  @GetMapping("/people")
  public Flux<Person> all() {
    return personRepository.findAll();
  }

  @GetMapping("/people/{id}")
	Mono<Person> findById(@PathVariable String id) {
		return personRepository.findOne(id);
	}
}
```
To me this looks very familiar and from a quick glance it doesn't really look any different from your standard Spring MVC controller, but after reading through the methods we can see the different return types from what we would normally expect. In this example `PersonRepository` must be a reactive repository as we have been able to directly return the results of their search queries, for reference, reactive repositories will return a `Flux` for collections and a `Mono` for singular entities.

The annotation method is not what I want to focus on in this post though. It's not cool and hip enough for us. There isn't enough use of lambdas to satisfy our thirst for writing Java in a more functional way. But Spring WebFlux has our backs. It provides an alternative method to route and handle requests to our servers that lightly uses lambdas to write router functions. Let's take a look at an example.
```java
@Configuration
public class PersonRouter {

  @Bean
  public RouterFunction<ServerResponse> route(PersonHandler personHandler) {
    return RouterFunctions.route(GET("/people/{id}").and(accept(APPLICATION_JSON)), personHandler::get)
        .andRoute(GET("/people").and(accept(APPLICATION_JSON)), personHandler::all)
        .andRoute(POST("/people").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::post)
        .andRoute(PUT("/people/{id}").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::put)
        .andRoute(DELETE("/people/{id}"), personHandler::delete)
        .andRoute(GET("/people/country/{country}").and(accept(APPLICATION_JSON)), personHandler::getByCountry);
  }
}
```
These are all the routes to methods in the `PersonHandler` which we will look at later on. We have created a bean that will handle our routing. To setup the routing functions we use the well named `RouterFunctions` class providing us with a load of static methods, but for now we are only interested with it's `route` method. Below is the signature of the `route` method.
```java
public static <T extends ServerResponse> RouterFunction<T> route(
      RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
  // stuff
}
```
The method shows that it takes in a `RequestPredicate` along with a `HandlerFunction` and outputs a `RouterFunction`. 

The `RequestPredicate` is what we use to specify behavior of the route, such as the path to our handler function, what type of request it is and the type of input it can accept. Due to my use of static imports to make everything read a bit clearer, some important information has been hidden from you. To create a `RequestPredicate` we should use the `RequestPredicates` (plural), a static helper class providing us with all the methods we need. Personally I do recommend statically importing `RequestPredicates` otherwise you code will be a mess due to the amount of times you might need to make use of `RequestPredicates` static methods. In the above example, `GET`, `POST`, `PUT`, `DELETE`, `accept` and `contentType` are all static `RequestPredicates` methods.

The next parameter is a `HandlerFunction`, which is a Functional Interface. There are three pieces of important information here, it has a generic type of `<T extends ServerResponse>`, it's `handle` method returns a `Mono<T>` and it takes in a `ServerRequest`. Using these we can determine that we need to pass in a function that returns a `Mono<ServerResponse>` (or one of it's subtypes). This obviously places a heavy constraint onto what is returned from our handler functions as they must meet this requirement or they will not be suitable for use in this format.

Finally the output is a `RouterFunction`. This can then be returned and will be used to route to whatever function we specified. But normally we would want to route lots of different requests to various handlers at once, which WebFlux caters for. Due to `route` returning a `RouterFunction` and the fact that `RouterFunction` also has its own routing method available, `andRoute`, we can chain the calls together and keep adding all the extra routes that we require.

If we take another look back at the `PersonRouter` example above, we can see that the methods are named after the REST verbs such as `GET` and `POST` that define the path and type of requests that a handler will take. If we take the first `GET` request for example, it is routing to `/people` with a path variable name `id` (path variable denoted by `{id}`) and the type of the returned content, specifically `APPLICATION_JSON` (static field from `MediaType`) is defined using the `accept` method. If a different path is used, it will not be handled. If the path is correct but the Accept header is not one of the accepted types, then the request will fail.

Before we continue I want to go over the `accept` and `contentType` methods. Both of these set request headers, `accept` matches to the Accept header and `contentType` to Content-Type. The Accept header defines what Media Types are acceptable for the response, as we were returning JSON representations of the `Person` object setting it to `APPLICATION_JSON` (`application/json` in the actual header) makes sense. The Content-Type has the same idea but instead describes what Media Type is inside the body of the sent request. That is why only the `POST` and `PUT` verbs have `contentType` included as the others do not have anything contained in their bodies. `DELETE` does not include `accept` and `contentType` so we can conclude that it is neither expecting anything to be returned nor including anything in its request body.

Now that we know how to setup the routes, lets look at writing the handler methods that deal with the incoming requests. Below is the code that handles all the requests from the routes that were defined in the earlier example.
```java
@Component
public class PersonHandler {

  private final PersonManager personManager;

  public PersonHandler(PersonManager personManager) {
    this.personManager = personManager;
  }

  public Mono<ServerResponse> get(ServerRequest request) {
    final UUID id = UUID.fromString(request.pathVariable("id"));
    final Mono<Person> person = personManager.findById(id);
    return person
        .flatMap(p -> ok().contentType(APPLICATION_JSON).body(fromPublisher(person, Person.class)))
        .switchIfEmpty(notFound().build());
  }

  public Mono<ServerResponse> all(ServerRequest request) {
    return ok().contentType(APPLICATION_JSON)
        .body(fromPublisher(personManager.findAll(), Person.class));
  }

  public Mono<ServerResponse> put(ServerRequest request) {
    final UUID id = UUID.fromString(request.pathVariable("id"));
    final Mono<Person> person = request.bodyToMono(Person.class);
    return personManager
        .findById(id)
        .flatMap(
            old ->
                ok().contentType(APPLICATION_JSON)
                    .body(
                        fromPublisher(
                            person
                                .map(p -> new Person(p, id))
                                .flatMap(p -> personManager.update(old, p)),
                            Person.class)))
        .switchIfEmpty(notFound().build());
  }

  public Mono<ServerResponse> post(ServerRequest request) {
    final Mono<Person> person = request.bodyToMono(Person.class);
    final UUID id = UUID.randomUUID();
    return created(UriComponentsBuilder.fromPath("people/" + id).build().toUri())
        .contentType(APPLICATION_JSON)
        .body(
            fromPublisher(
                person.map(p -> new Person(p, id)).flatMap(personManager::save), Person.class));
  }

  public Mono<ServerResponse> delete(ServerRequest request) {
    final UUID id = UUID.fromString(request.pathVariable("id"));
    return personManager
        .findById(id)
        .flatMap(p -> noContent().build(personManager.delete(p)))
        .switchIfEmpty(notFound().build());
  }

  public Mono<ServerResponse> getByCountry(ServerRequest serverRequest) {
    final String country = serverRequest.pathVariable("country");
    return ok().contentType(APPLICATION_JSON)
        .body(fromPublisher(personManager.findAllByCountry(country), Person.class));
  }
}
```
One thing that is quite noticeable, is the lack of annotations. Bar the `@Component` annotation to auto create a `PersonHandler` bean there are no other Spring annotations.

I have tried to keep most of the repository logic out of this class and have hidden any references to the entity objects by going via the `PersonManager` that delegates to the `PersonRepository` it contains. If you are interested in the code within `PersonManager` then it can be seen here on my [GitHub](https://github.com/lankydan/spring-boot-webflux/blob/master/src/main/java/com/lankydanblog/tutorial/person/PersonManager.java), further explanations about it will be excluded for this post so we can focus on WebFlux itself.

Ok, back to the code at hand. Let's take a closer look at the `get` and `post` methods to figure out what is going on.
```java
public Mono<ServerResponse> get(ServerRequest request) {
  final UUID id = UUID.fromString(request.pathVariable("id"));
  final Mono<Person> person = personManager.findById(id);
  return person
      .flatMap(p -> ok().contentType(APPLICATION_JSON).body(fromPublisher(person, Person.class)))
      .switchIfEmpty(notFound().build());
}
```
This method is for retrieving a single record from the database that backs this example application. Due to Cassandra being the database of choice I have decided to use an `UUID` for the primary key of each record, this has the unfortunate effect of making testing the example more annoying but nothing that some copy and pasting can't solve. 

Remember that a path variable was included in the path for this `GET` request. Using the `pathVariable` method on the `ServerRequest` passed into the method we are able to extract it's value by providing the name of the variable, in this case `id`. The ID is then converted into a `UUID`, which will throw an exception if the string is not in the correct format, I decided to ignore this problem so the example code doesn't get messier.

Once we have the ID, we can query the database for the existence of a matching record. A `Mono<Person>` is returned which either contains the existing record mapped to a `Person` or it left as an empty `Mono`. 

Using the returned `Mono` we can output different responses depending on it's existence. This means we can return useful status codes to the client to go along with the contents of the body. If the record exists then `flatMap` returns a `ServerResponse` with the `OK` status. Along with this status we want to output the record, to do this we specify the content type of the body, in this case `APPLICATION_JSON`, and add the record into it. `fromPublisher` takes our `Mono<Person>` (which is a `Publisher`) along with the `Person` class so it knows what it is mapping into the body. `fromPublisher` is a static method from the `BodyInserters` class.

If the record does not exist, then the flow will move into the `switchIfEmpty` block and return a `NOT FOUND` status. As nothing is found, the body can be left empty so we just create the `ServerResponse` there are then.

Now onto the `post` handler.
```java
public Mono<ServerResponse> post(ServerRequest request) {
  final Mono<Person> person = request.bodyToMono(Person.class);
  final UUID id = UUID.randomUUID();
  return created(UriComponentsBuilder.fromPath("people/" + id).build().toUri())
      .contentType(APPLICATION_JSON)
      .body(
          fromPublisher(
              person.map(p -> new Person(p, id)).flatMap(personManager::save), Person.class));
}
```
Even just from the first line we can see that it is already different to how the `get` method was working. As this is a `POST` request it needs to accept the object that we want to persist from the body of the request. As we are trying to insert a single record we will use the request's `bodyToMono` method to retrieve the `Person` from the body. If you were dealing with multiple records you would probably want to use `bodyToFlux` instead.

We will return a `CREATED` status using the `created` method that takes in a `URI` to determine the path to the inserted record. It then follows a similar setup as the `get` method by using the `fromPublisher` method to add the new record to the body of the response. The code that forms the `Publisher` is slightly different but the output is still a `Mono<Person>` which is what matters. Just for further explanation about how the inserting is done, the `Person` passed in from the request is mapped to a new `Person` using the `UUID` we generated and is then passed to `save` by calling `flatMap`. By creating a new `Person` we only insert values into Cassandra that we allow, in this case we do not want the `UUID` passed in from the request body.

So, that's about it when it comes to the handlers. Obviously there other methods that we didn't go through. They all work differently but all follow the same concept of returning a `ServerResponse` that contains a suitable status code and record(s) in the body if required.

We have now written all the code we need to get a basic Spring WebFlux back-end up a running. All that is left is to tie all the configuration together, which is easy with Spring Boot.
```java
@SpringBootApplication
public class Application {
  public static void main(String args[]) {
    SpringApplication.run(Application.class);
  }
}
```
Rather than ending the post here we should probably look into how to actually make use of the code.

Spring provides the `WebClient` class to handle requests without blocking. We can make use of this now as a way to test the application, although there is also a `WebTestClient` which we could use here instead. The `WebClient` is what you would use instead of the blocking `RestTemplate` when creating a reactive application.

Below is some code that calls the handlers that were defined in the `PersonHandler`.
```java
public class Client {

  private WebClient client = WebClient.create("http://localhost:8080");

  public void doStuff() {

    // POST
    final Person record = new Person(UUID.randomUUID(), "John", "Doe", "UK", 50);
    final Mono<ClientResponse> postResponse =
        client
            .post()
            .uri("/people")
            .body(Mono.just(record), Person.class)
            .accept(APPLICATION_JSON)
            .exchange();
    postResponse
        .map(ClientResponse::statusCode)
        .subscribe(status -> System.out.println("POST: " + status.getReasonPhrase()));

    // GET
    client
        .get()
        .uri("/people/{id}", "a4f66fe5-7c1b-4bcf-89b4-93d8fcbc52a4")
        .accept(APPLICATION_JSON)
        .exchange()
        .flatMap(response -> response.bodyToMono(Person.class))
        .subscribe(person -> System.out.println("GET: " + person));

    // ALL
    client
        .get()
        .uri("/people")
        .accept(APPLICATION_JSON)
        .exchange()
        .flatMapMany(response -> response.bodyToFlux(Person.class))
        .subscribe(person -> System.out.println("ALL: " + person));

    // PUT
    final Person updated = new Person(UUID.randomUUID(), "Peter", "Parker", "US", 18);
    client
        .put()
        .uri("/people/{id}", "ec2212fc-669e-42ff-9c51-69782679c9fc")
        .body(Mono.just(updated), Person.class)
        .accept(APPLICATION_JSON)
        .exchange()
        .map(ClientResponse::statusCode)
        .subscribe(response -> System.out.println("PUT: " + response.getReasonPhrase()));

    // DELETE
    client
        .delete()
        .uri("/people/{id}", "ec2212fc-669e-42ff-9c51-69782679c9fc")
        .exchange()
        .map(ClientResponse::statusCode)
        .subscribe(status -> System.out.println("DELETE: " + status));
  }
}

```
Don't forget to instantiate the `Client` somewhere, below is a nice lazy way to do it!
```java
@SpringBootApplication
public class Application {

  public static void main(String args[]) {
    SpringApplication.run(Application.class);
    Client client = new Client();
    client.doStuff();
  }
}
```
First we create the `WebClient`.
```java
private final WebClient client = WebClient.create("http://localhost:8080");
```
Once created we can start doing stuff with it, hence the `doStuff` method.

Let's break down the `POST` request that is being send to the back-end.
```java
final Mono<ClientResponse> postResponse =
    client
        .post()
        .uri("/people")
        .body(Mono.just(record), Person.class)
        .accept(APPLICATION_JSON)
        .exchange();
postResponse
    .map(ClientResponse::statusCode)
    .subscribe(status -> System.out.println("POST: " + status.getReasonPhrase()));
```
I wrote this one down slightly differently so you can see that a `Mono<ClientResponse>` is returned from sending a request. The `exchange` method fires the HTTP request over to the server. The response will then be dealt with whenever the response arrives, if it ever does.

Using the `WebClient` we specify that we want to send a `POST` request using the `post` method of course. The `URI` is then added with the `uri` method (overloaded method, this one takes in a `String` but another accepts a `URI`). Im tired of saying this method does what the method is called so, the contents of the body are then added along with the Accept header. Finally we send the request by calling `exchange`. 

Note that the Media Type of `APPLICATION_JSON` matches up with the type defined in the `POST` router function. If we were to send a different type, say `TEXT_PLAIN` we would get a `404` error as no handler exists that matches to what the request is expecting to be returned.

Using the `Mono<ClientResponse>` returned by calling `exchange` we can map it's contents to our desired output. In the case of the example above, the status code is printed to the console. If we think back to the `post` method in `PersonHandler`, remember that it can only return the "Created" status, but if the sent request does not match up correctly then "Not Found" will be printed out.

Let's look at one of the other requests.
```java
client
    .get()
    .uri("/people/{id}", "a4f66fe5-7c1b-4bcf-89b4-93d8fcbc52a4")
    .accept(APPLICATION_JSON)
    .exchange()
    .flatMap(response -> response.bodyToMono(Person.class))
    .subscribe(person -> System.out.println("GET: " + person));
```
This is our typical `GET` request. It looks pretty similar to the `POST` request we just went through. The main differences are that `uri` takes in both the path of the request and the `UUID` (as a `String` in this case) as a parameter to that will replace the path variable `{id}` and that the body is left empty. How the response is handled is also different. In this example it extracts the body of the response and maps it to a `Mono<Person>` and prints it out. This could have been done with the previous `POST` example but the status code of the response was more useful for it's scenario.

For a slightly different perspective, we could use cURL to make requests and see what the response looks like.
```
CURL -H "Accept:application/json" -i localhost:8080/people
```json
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
```
The response will look something like this, obviously it will differ depending on the data you have stored.

Note the response headers.
```
transfer-encoding: chunked
Content-Type: application/json
```
The `transfer-encoding` here represents data that is transferred in chunks that can be used to stream data. This is what we need so the client can act reactively to the data that is returned to it.

I think that this should be a good place to stop. We have covered quite a lot of material here which has hopefully helped you understand Spring WebFlux better. There are a few other topics I want to cover about WebFlux but I will do those in separate posts as I think this one is long enough as it is.

In conclusion, in this post we very briefly discussed why you would want to use Spring WebFlux over a typical Spring MVC back-end. We then looked at how to setup routes and handlers to process the incoming requests. The handlers implemented methods that could deal with most of the REST verbs and returned the correct data and status codes in their responses. Finally we looked at two ways to make requests to the back-end, one using a `WebClient` to process the output directly on the client side and another via cURL to see what the returned JSON looks like.

If you are interested in looking at the rest of the code I used to create the example application for this post, it can be found on my [GitHub](https://github.com/lankydan/spring-boot-webflux).

As always if you found this post helpful, please share it and if you want ot keep up with my latest posts then you can follow me on Twitter at [@LankyDanDev](https://twitter.com/LankyDanDev).