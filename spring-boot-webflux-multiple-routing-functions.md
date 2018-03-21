In this post we will be looking at defining multiple router functions to different logical domains in Spring WebFlux. This might not be a problem if you are creating "Microservices" as you will most likely only be working within a single domain for each service, but if you are not then you will likely have the need to include multiple domains within your application that users or your own services can interact with. The code to do this is as simple as I hoped it would be and could be explained in a few sentences. To make this post a little more interesting we will look at some of the Spring code that makes this all possible.

If you are new to WebFlux I recommend having a look at my previous post, [Doing stuff with Spring WebFlux](https://lankydanblog.com/2018/03/15/doing-stuff-with-spring-webflux/), where I wrote some thorough examples and explanations on the subject.

So lets set the scene first. You have two different domains within your application, say people and locations. You might want to keep them separated from each other not only logically but also within your code. To do so you need a way to define your routes in isolation from each others domain. That is what we will look at in this post.

If you think you already know the answer to this problem, then you are probably right. It really is that simple. Let's work our way up to it though. To create routes for just the people domain create a `RouterFunction` bean that maps to the relevant handler functions, like below.
```java
@Configuration
public class MyRouter {
	// works for a single bean
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
	// not ideal!
  @Bean
  public RouterFunction<ServerResponse> routes(PersonHandler personHandler, LocationHandler locationHandler) {
    return RouterFunctions.route(GET("/people/{id}").and(accept(APPLICATION_JSON)), personHandler::get)
        .andRoute(GET("/people").and(accept(APPLICATION_JSON)), personHandler::all)
        .andRoute(POST("/people").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::post)
        .andRoute(PUT("/people/{id}").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), personHandler::put)
        .andRoute(DELETE("/people/{id}"), personHandler::delete)
        .andRoute(GET("/people/country/{country}").and(accept(APPLICATION_JSON)), personHandler::getByCountry)
        .andRoute(GET("/locations/{id}").and(accept(APPLICATION_JSON)), locationHandler::get);
  }
}
```
The bean is now including a reference to the `LocationHandler` so the location route can be setup. The problem with this solution is that it requires the code to be coupled together. Furthermore, if you need to add even more handlers you are soon going to be overwhelmed with the amount of dependencies being injected into this bean.

The way around this, is to create multiple `RouterFunction` beans. That's it. So, if we create one in the people domain, say `PersonRouter` and one in the location domain named `LocationRouter`, each can define the routes that they need and Spring will do the rest. This works because Spring goes through the application context and finds or creates any `RouterFunction` beans and consolidates them into a single function for later use.

Using this information we can write the code below.
```java
@Configuration
public class PersonRouter {
	// solution
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
	// solution
	I
  @Bean
  public RouterFunction<ServerResponse> locationRoutes(LocationHandler locationHandler) {
    return RouterFunctions.route(GET("/locations/{id}").and(accept(APPLICATION_JSON)), locationHandler::get);
  }
}
```
`PersonRouter` can be kept with other people / person related code and `LocationRouter` can do the same.

To make this more interesting, why does this work?

`RouterFunctionMapping` is the class that retrieves all the `RouterFunction` beans created within the application context. The `RouterFunctionMapping` bean is created within `WebFluxConfigurationSupport` which is the epicenter for Spring WebFlux configuration. By including the `@EnableWebFlux` annotation on a configuration class or by relying on auto-configuration, a chain of events starts and collecting all of our `RouterFunction`s is one of them.

Below is the `RouterFunctionMapping` class. I have removed it's constructors and a few methods to make the snippet here a bit easier to digest.
```java
public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {

	@Nullable
	private RouterFunction<?> routerFunction;

	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

	// constructors

	// getRouterFunction

	// setMessageReaders

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(this.messageReaders)) {
			ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
			this.messageReaders = codecConfigurer.getReaders();
		}

		if (this.routerFunction == null) {
			initRouterFunctions();
		}
	}

	/**
	 * Initialized the router functions by detecting them in the application context.
	 */
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
		SortedRouterFunctionsContainer container = new SortedRouterFunctionsContainer();
		obtainApplicationContext().getAutowireCapableBeanFactory().autowireBean(container);

		return CollectionUtils.isEmpty(container.routerFunctions) ? Collections.emptyList() :
				container.routerFunctions;
	}

	// getHandlerInternal

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
The path to retrieving all the routes starts in `afterPropertiesSet` that is invoked after the `RouterFunctionMapping` bean is created. As it's internal `RouterFunction` is `null` it calls `initRouterFunctions` triggering a series of methods leading to the execution of `routerFunctions`. A new `SortedRouterFunctionsContainer` is constructed (private static class) setting it's `routerFunctions` field by injecting all `RouterFunction`s from the application context. This works since Spring will inject all beans that of type `T` when a `List<T>` is being injected. The now retrieved `RouterFunction`s are combined together to make a single `RouterFunction` that is used from now on to route all incoming requests to the appropriate handler.

That's all there is to it. In conclusion defining multiple `RouterFunction`s for different business domains is very simple as you just create them in whatever area they most make sense and Spring will go off and fetch them all. To demistify some of the magic we looked into `RouterFunctionMapping` to see how the `RouterFunction`s we create are collected and combined so that they can be used to route requests to handlers. As a closing note, I do understand that this post in some respects is quite trivial but sometimes the seemingly obvious information can be pretty helpful.

If you have not done so already, I recommend looking at my previous post [Doing stuff with Spring WebFlux](https://lankydanblog.com/2018/03/15/doing-stuff-with-spring-webflux/).

Finally, if you found this post helpful and would like to keep up with my new posts as I write them, then you can follow me on Twitter at [@LankyDanDev](https://twitter.com/LankyDanDev).