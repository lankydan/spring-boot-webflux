So Spring Boot 2.0 when GA recently, so I decided to write my first post about Spring for quite a while. Since the release I have been seeing more mentions of Spring WebFlux along with tutorials on how to use it, but after reading through them and trying to get it working myself I found it a bit hard to make the jump from the posts and tutorials that I read to actually write some code that actually does something a tiny bit more interesting than returning a string from the back-end. Now, I'm hoping I'm not shooting myself in the foot by saying that as you could probably make the same criticism of the code I use in this post, but here is my attempt to give a tutorial of Spring WebFlux that actually resembles something that you might use in the wild.

Before I continue and after all this mentioning of WebFlux, what actually is it? Spring WebFlux is a fully non-blocking reactive alternative to Spring MVC. This allows better vertical scaling without increasing your hardware resources. Being reactive it now makes use of Reactive Streams to allow asynchronous processing of data returned from calls to the server. This means we are going to see a lot less `List`s, `Collection`s or even single objects and instead their reactive equivalents such as `Flux` and `Mono` (if you are using Reactor). I'm not going to go to in depth on what Reactive Streams are, as honestly I need to look into it even more myself before I try to explain it to anyone. Instead lets get back to focusing on WebFlux.

I use Spring Boot to write the code in this tutorial as usual.

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
Although I didn't include it in the dependency snippet above, the `spring-boot-start-parent` is used, which can finally be upped to version `2.0.0.RELEASE`. Being this tutorial is about WebFlux, including the `spring-boot-starter-webflux` is obviously a good idea. `spring-boot-starter-data-cassandra-reactive` has also been included as we will be using this as the database for the example application as it is one of the few databases that have reactive support (at the time of writing). By using these dependencies together our application can be fully reactive from front to back.

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

The annotation method is not what I want to focus on in this post though. It's not cool and hip enough for us. There isn't enough use of lambdas to satisfy our thirst for writing Java in a more functional way. But Spring WebFlux has our backs. It provides an alternative method to route and handle requests to our servers that lightly uses lambdas to write our router functions. Let's take a look at an example.
```java
@Configuration
public class PersonRouter {

  @Bean
  public RouterFunction<ServerResponse> route(PersonHandler personHandler) {

    // DOES THE GET METHOD NEED AN `accept` DEFINED IT JUST READS THE PATH VARIABLE

    // spring example uses content type for post, does put need the same?

    // I think I do want accept for all except for DELETE
    // and content type for POST and DELETE


    return RouterFunctions.route(GET("/people/{id}").and(accept(APPLICATION_JSON)), personHandler::get)
        .andRoute(GET("/people").and(accept(APPLICATION_JSON)), personHandler::all)
        .andRoute(POST("/people").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::post)
        .andRoute(PUT("/people/{id}").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::put)
        .andRoute(DELETE("/people/{id}"), personHandler::delete)
        .andRoute(GET("/people/country/{country}").and(accept(APPLICATION_JSON)), personHandler::getByCountry);
  }
}
```
These are all the routes to methods on the `PersonHandler` which we will look at later on. We have created a bean that will handle our routing. To setup the routing functions we use the well named `RoutingFunctions` providing us with a load of static methods but we are only currently interested with it's `route` method. Below is the signature of the `route` method.
```java
public static <T extends ServerResponse> RouterFunction<T> route(
      RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
  // stuff
}
```
The method shows that it takes in a `RequestPredicate` along with a `HandlerFunction` and outputs a `RouterFunction`. 

The `RequestPredicate` is what we use to specify behavior of the route, such as the path to our handler function, what type of request it is and what type of input it can accept. Due to my use of static imports to make everything read a bit clearer, some important information has been hidden from you. To create a `RequestPredicate` we should use the `RequestPredicates` (plural), a static helper class providing us with all the methods we need. Personally I do recommend statically importing `RequestPredicates` otherwise you code will be a mess due to the amount of times you might need to make use of `RequestPredicates` static methods. In the above example, `GET`, `POST`, `PUT`, `DELETE` and `accept` are all static `RequestPredicates` methods.

The next parameter is a `HandlerFunction` which is a Functional Interface. There are three pieces of important information here, it's generic type of `<T extends ServerResponse>` and its `handle` method that returns a `Mono<T>` while taking in a `ServerRequest`. Using these we can determine that we need to pass in a function that returns a `Mono<ServerResponse>` (or one of it's subtypes). This obviously places a heavy constraint onto what is returned from our handler functions as they must meet this requirement or they will not be suitable for use in this format.

Finally the output is a `RouterFunction`. This can then be returned and will be used to route to whatever function we specified. But normally we would want to route lots of different requests to various handers at once, which WebFlux caters for. Due to `route` returning a `RouteFunction` and the fact that `RouterFunction` also has its own routing method available, `andRoute`, we can chain the calls together and keep adding all the extra routes that we require.

If we take another look back at the `PersonRouter` example above, we can see that there methods named after the REST verbs such as `GET` and `POST` that define the path and type of requests that a handler will take. If we take the first `GET` request for example, it is routing to `/people` with a path variable name `id` (path variable denoted by `{id}`) and the type of the returned content, specifically `APPLICATION_JSON` (static field from `MediaType`) is defined using the `accept` method. If a different path is used, it will not be handled. If the path is correct but the content is of the wrong type, then an then the request might fail depending on the difference between the requested content and what is actually returned.

Before we continue I want to go over the `accept` and `contentType` methods. Both of these set request headers, `accept` matches to the Accept header and `contentType` to Content-Type. The Accept header defines what Media Types are acceptable for the response, as we were returning JSON representations of the `Person` object setting it to `APPLICATION_JSON` makes sense. The Content-Type has the same idea but instead describes what Media Type is inside the body of the sent request. That is why only the `POST` and `PUT` verbs have `contentType` included as the others do not having anything contained in their bodies. `DELETE` does not included `accept` and `contentType` so we can conclude that it is neither expecting anything to be returned nor including anything in its request body.

Now that we know how to setup the routes, lets look at writing the handler methods that deal with the incoming requests. Below is the handles all the requests from the routes that were defined in the earlier example.
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
                            Person.class))
                    .switchIfEmpty(notFound().build()));
  }

  public Mono<ServerResponse> post(ServerRequest request) {
    final Mono<Person> person = request.bodyToMono(Person.class);
    final UUID id = UUID.randomUUID();
    return created(UriComponentsBuilder.fromPath("people/" + id).build().toUri())
        .contentType(APPLICATION_JSON)
        .body(
            fromPublisher(
                person.map(p -> new Person(p, id)).flatMap(personManager::save),
                Person.class));
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
One thing that is quite noticable, is the lack of annotations. Bar the `@Component` annotation to auto create a `PersonHandler` bean there are no other Spring annotations.

I have tried to keep most of the repsitory logic out of this class and have hidden any references to the entity objects by going via the `PersonManager` that delegates to `PersonRepository` is contains within in it. If you are interested in the code within `PersonManager` then it can be seen here on my [GitHub](URL), further explanations about it will be excluded for this post so we can focus on WebFlux itself.

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
This method is for retrival of a single record from the database that backs this example application. Due to Cassandra being the database of choice I have decided to use an `UUID` for the primary key of each record, this has the unfortunate effect of making testing the example more annoying but nothing that some copy and pasting can't solve. 

Remember that a path variable was included in the path for this `GET` request. Using the `pathVariable` method on the `ServerRequest` passed into the method we are able to extract it's value by providing the name of the variable, in this case `id`. The ID is then converted into a `UUID`, which will throw an exception if the string is not in the correct format, I decided to ignore this problem so the example code doesn't get messier.

Once we have the ID, we can query the database for the existance of a matching record. A `Mono<Person>` is returned (the actual repository returns the same type) which either contains the existing record mapped to a `Person` or it left as an empty `Mono`. 

Using the returned `Mono` we can return different responses depends on it's existance. This means we can return useful status codes to the client to go along with the methods output. Using `flatMap` a `ServerResponse` with the `OK` status is created if the record exists. Along with this status we want to output the record that was found, to do this we specify the content type of the body, in this case `APPLICATION_JSON`, and add the record into it. `fromPublisher` takes our `Mono<Person>` (which is a `Publisher`) along with the `Person` class so it know what it is mapping into the body. `fromPublisher` is a static method from the `BodyInserters` class.

If the record does not exist, then the flow will move into the `switchIfEmpty` block and return a `NOT FOUND` status. As nothing is found, the is nothing that needs to be added into the body so we just create the `ServerResponse` there are then.

Now onto the `post` handler.
```java
public Mono<ServerResponse> post(ServerRequest request) {
  final Mono<Person> person = request.bodyToMono(Person.class);
  final UUID id = UUID.randomUUID();
  return created(UriComponentsBuilder.fromPath("people/" + id).build().toUri())
      .contentType(APPLICATION_JSON)
      .body(
          fromPublisher(
              person.map(p -> new Person(p, id)).flatMap(personManager::save),
              Person.class));
}
```
Even just from the first line we can see that it is already different to how the `get` method was working. As this is a `POST` request it needs to accept the object that we want to persist from the body of the request. As we are trying to insert a single record we will use the request's `bodyToMono` method to retrieve the `Person` from the body. If you were dealing with multiple records you would probably want to use `bodyToFlux` instead.

We will return a `CREATED` status using the `created` method that takes in a `URI` to determine the path to the inserted record. It then follows a similar setup as the `get` method by using the `fromPublisher` method to add the new record to the body of the response. The code that forms the `Publisher` is slightly different but the output is still a `Mono<Person>` which is what matters. Just for further explanation about how the inserting is done, the `Person` passed in from the request is is mapped to a new `Person` using the `UUID` we generated which itself is flat mapped to call `save`. By creating a new `Person` we are able to only inserts values into Cassandra that we allow, in this case we do not want the `UUID` passed in from the request body.

So, that's about it when it comes to the handlers. Obviously the other methods that I didn't go through all work differently but they all follow the same concept of returning a `ServerResponse` that contains a suitable status code and record(s) in the body if required.

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