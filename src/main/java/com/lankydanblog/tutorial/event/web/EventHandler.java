package com.lankydanblog.tutorial.event.web;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

import com.lankydanblog.tutorial.event.Event;
import com.lankydanblog.tutorial.event.EventManager;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;

@Component
public class EventHandler {

  private final EventManager eventManager;

  public EventHandler(EventManager eventManager) {
    this.eventManager = eventManager;
  }

  // why use the server response object?

  public Mono<ServerResponse> get(ServerRequest request) {
    final String type = request.pathVariable("type");
    final Optional<String> timeParameter = request.queryParam("time");
    final Flux<Event> events = timeParameter
                                 .map(time -> eventManager.findAllOfTypeAfterStartTime(type, LocalDateTime.parse(time)))
                                 .orElseGet(() -> eventManager.findAllOfType(type));
    return ServerResponse.ok()
             .contentType(APPLICATION_JSON)
             .body(fromPublisher(events, Event.class));
  }

  public Mono<ServerResponse> all(ServerRequest request) {
    return ServerResponse.ok()
             .contentType(APPLICATION_JSON)
             .body(fromPublisher(eventManager.findAll(), Event.class));
  }

//  public Mono<ServerResponse> post(ServerRequest request) {
//    final Mono<Event> event = request.bodyToMono(Event.class);
//    return event.subscribe(e -> {
//      eventManager.save(e);
//      final URI uri =
//        MvcUriComponentsBuilder.fromController(getClass())
//          .path("/{id}")
//          .buildAndExpand(person.getId())
//          .toUri();
//      return ServerResponse.created(new UriBuilder().path("events/get")).contentType(TEXT_PLAIN).
//    });
//  }
}
