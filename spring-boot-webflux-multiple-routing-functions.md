In this post will will be looking at defining multiple routing functions to different logical domains in Spring WebFlux. This might not be a problem if you are writing "Microservices" as you will probably only be working with a single domain for each service, but if you are not then you will likely have the need to include multiple domains within your application that users or you own services can interact with. The code to do this was as simple as I hoped it would be and could be explained in a few sentences. To make this a little more interesting we will look at some of the Spring code that makes this all possible.

If you are new to WebFlux I recommend having a look at my previous post, [Doing stuff with Spring WebFlux](https://lankydanblog.com/2018/03/15/doing-stuff-with-spring-webflux/), where I wrote some thorough examples and explanations on the subject.

So lets set the scene first. You have two different domains within your application, say keeping information about people and locations. You might want to keep them separated from each other not only logically but also within your code. To do so you need to a way to define your routes in isolation from other domain. That is what we will look at in this post.

Now if you think you know the answer to this problem, then you are probably right. It really is that simple. To create routes for just the people domain you create a `RouterFunction` bean that maps to the relevant handler functions, like below.
```java
@Configuration
public class MyRouter {

  @Bean
  public RouterFunction<ServerResponse> routes(PersonHandler personHandler) {
    return RouterFunctions.route(GET("/people/{id}").and(accept(APPLICATION_JSON)), personHandler::get)
        .andRoute(GET("/people").and(accept(APPLICATION_JSON)), personHandler::all)
        .andRoute(POST("/people").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::post)
        .andRoute(PUT("/people/{id}").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::put)
        .andRoute(DELETE("/people/{id}"), personHandler::delete)
        .andRoute(GET("/people/country/{country}").and(accept(APPLICATION_JSON)), personHandler::getByCountry);
  }
}
```
This creates the routes to the various handler functions in the `PersonHandler`.

So, now we want to add the routes for the location logic. We could simply add the routes to this bean, like below.
```java
@Configuration
public class MyRouter {

  @Bean
  public RouterFunction<ServerResponse> routes(PersonHandler personHandler, LocationHandler locationHandler) {
    return RouterFunctions.route(GET("/people/{id}").and(accept(APPLICATION_JSON)), personHandler::get)
        .andRoute(GET("/people").and(accept(APPLICATION_JSON)), personHandler::all)
        .andRoute(POST("/people").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::post)
        .andRoute(PUT("/people/{id}").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::put)
        .andRoute(DELETE("/people/{id}"), personHandler::delete)
        .andRoute(GET("/people/country/{country}").and(accept(APPLICATION_JSON)), personHandler::getByCountry)
        .andRoute(GET("/location/{id}").and(accept(APPLICATION_JSON)), locationHandler::get);
  }
}
```
The bean is now including a reference to the `LocationHandler` so the location route can be setup. The problem with this solution is that it requires the code to be coupled together.

The way around this, as I keep mentioning is very simple, is to create multiple `RouterFunction` beans. That's it. So, if we create one in the people domain, say `PersonRouter` and one in the location domain named `LocationRouter`, each can define the routes that they need and Spring will do the rest. This works because when Spring go to create all the routes it goes through the application context and finds existing or creates any `RouterFunction` beans.

If we take this information, a tidy solution is to write.
```java
@Configuration
public class PersonRouter {

  @Bean
  public RouterFunction<ServerResponse> peopleRoutes(PersonHandler personHandler) {
    return RouterFunctions.route(GET("/people/{id}").and(accept(APPLICATION_JSON)), personHandler::get)
        .andRoute(GET("/people").and(accept(APPLICATION_JSON)), personHandler::all)
        .andRoute(POST("/people").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::post)
        .andRoute(PUT("/people/{id}").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::put)
        .andRoute(DELETE("/people/{id}"), personHandler::delete)
        .andRoute(GET("/people/country/{country}").and(accept(APPLICATION_JSON)), personHandler::getByCountry);
  }
}
```
and
```java
@Configuration
public class LocationRouter {

  @Bean
  public RouterFunction<ServerResponse> locationRoutes(LocationHandler locationHandler) {
    return RouterFunctions.route(GET("/location/{id}").and(accept(APPLICATION_JSON)), locationHandler::get);
  }
}
```


RouterFunctionMapping
```java  

/**
	 * Create an empty {@code RouterFunctionMapping}.
	 * <p>If this constructor is used, this mapping will detect all {@link RouterFunction} instances
	 * available in the application context.
	 */
	public RouterFunctionMapping() {
	}

	/**
	 * Create a {@code RouterFunctionMapping} with the given {@link RouterFunction}.
	 * <p>If this constructor is used, no application context detection will occur.
	 * @param routerFunction the router function to use for mapping
	 */
	public RouterFunctionMapping(RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}


  protected void initRouterFunctions() {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for router functions in application context: " +
					getApplicationContext());
		}

		List<RouterFunction<?>> routerFunctions = routerFunctions();
		if (!CollectionUtils.isEmpty(routerFunctions) && logger.isInfoEnabled()) {
			routerFunctions.forEach(routerFunction -> logger.info("Mapped " + routerFunction));
		}
		this.routerFunction = routerFunctions.stream()
				.reduce(RouterFunction::andOther)
				.orElse(null);
	}

	private List<RouterFunction<?>> routerFunctions() {

    // creates the private container below

		SortedRouterFunctionsContainer container = new SortedRouterFunctionsContainer();

    // creates a bean from the container and autowired in its dependencies of RouterFunction'setRouterFunctions
    // router function beans are then created due to being required now by Spring (can be seen from debugging, hits my code breakpoints after the autowiring of the container happens)

    // the router functions retrieved are then combined to make a new RouterFunction that contains all the routes of the retrieved functions
    // this new router function is named `routerFunction` in RouterFunctionMapper
    // therefore to have multiple routes you just need to define multiple RouterFunction beans and Spring will do everything else

		obtainApplicationContext().getAutowireCapableBeanFactory().autowireBean(container);

		return CollectionUtils.isEmpty(container.routerFunctions) ? Collections.emptyList() :
				container.routerFunctions;
	}

	@Override
	protected Mono<?> getHandlerInternal(ServerWebExchange exchange) {
		if (this.routerFunction != null) {
			ServerRequest request = ServerRequest.create(exchange, this.messageReaders);
			exchange.getAttributes().put(RouterFunctions.REQUEST_ATTRIBUTE, request);
			return this.routerFunction.route(request);
		}
		else {
			return Mono.empty();
		}
	}


	private static class SortedRouterFunctionsContainer {

		@Nullable
		private List<RouterFunction<?>> routerFunctions;

		@Autowired(required = false)
		public void setRouterFunctions(List<RouterFunction<?>> routerFunctions) {
			this.routerFunctions = routerFunctions;
		}
	}

}
```