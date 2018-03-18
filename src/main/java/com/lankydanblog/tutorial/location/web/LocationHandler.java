package com.lankydanblog.tutorial.location.web;

import com.lankydanblog.tutorial.location.Location;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class LocationHandler {

  public Mono<ServerResponse> get(ServerRequest request) {
    return ServerResponse.ok()
        .body(
            BodyInserters.fromPublisher(
                Mono.just(new Location(UUID.randomUUID(), "Westminster", "UK")), Location.class));
  }
}
