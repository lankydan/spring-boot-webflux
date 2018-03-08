package com.lankydanblog.tutorial.event.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class EventRouter {

  @Bean
  public RouterFunction<ServerResponse> route(EventHandler eventHandler) {
    return RouterFunctions.route(GET("/events/{type}").and(accept(APPLICATION_JSON)), eventHandler::get)
             .andRoute(GET("/events").and(accept(APPLICATION_JSON)), eventHandler::all);
  }
}
