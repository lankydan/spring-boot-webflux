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
    return RouterFunctions.route(GET("/people/{id}").and(accept(APPLICATION_JSON)), personHandler::get)
        .andRoute(GET("/people").and(accept(APPLICATION_JSON)), personHandler::all)
        .andRoute(POST("/people").and(accept(APPLICATION_JSON)), personHandler::post)
        .andRoute(PUT("/people/{id}").and(accept(APPLICATION_JSON)), personHandler::put)
        .andRoute(DELETE("/people/{id}").and(accept(APPLICATION_JSON)), personHandler::delete)
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

The `RequestPredicate` is what we use to specify behavior of the route, such as the path to our handler function, what type of request it is and what type of input it can accept. Due to my use of static imports to make everything read a bit clearer, some important information has been hidden from you. To create a `RequestPredicate` we should use the `RequestPredicates` (plural), a static helper class providing us with all the methods we need. Personally I do recommend statically importing `RequestPredicates` otherwise you code will be a mess due to the amount of times you might need to make use of `RequestPredicates` static methods.

The next parameter is a `HandlerFunction` which is a Functional Interface. There are two pieces of important information here, it's generic type of `<T extends ServerResponse>` and its `handle` method that returns a `Mono<T>`. Using these we can determine that we need to pass in a function that returns a `Mono<ServerResponse>` (or one of it's subtypes). This obviously places a heavy constraint onto what is returned from our handler functions as they must meet this requirement or they will not be suitable for use in this format.