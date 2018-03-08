package com.lankydanblog.tutorial.client;

import java.time.LocalDateTime;

import com.lankydanblog.tutorial.event.Event;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class Client {

  private WebClient client = WebClient.create("http://localhost:8080");

  public void printEvents() {
    final Mono<ClientResponse> clientResponse = client.get()
                                                  .uri("/events/{type}?time={time}", "notification", LocalDateTime.now().minusDays(10))
                                                  .accept(MediaType.APPLICATION_JSON).exchange();
    clientResponse.flatMapMany(response -> response.bodyToFlux(Event.class)).subscribe(event -> System.out.println("Event of type: " + event.getStartTime()));

    final Mono<ClientResponse> clientResponse2 = client.get()
                                                  .uri("/events/{type}", "notification")
                                                  .accept(MediaType.APPLICATION_JSON).exchange();
    clientResponse2.flatMapMany(response -> response.bodyToFlux(Event.class)).subscribe(event -> System.out.println("Event of type and after time: " + event.getStartTime()));

    final Mono<ClientResponse> clientResponse3 = client.get()
                                                   .uri("/events")
                                                   .accept(MediaType.APPLICATION_JSON).exchange();
    clientResponse3.flatMapMany(response -> response.bodyToFlux(Event.class)).subscribe(event -> System.out.println("All events: " + event.getStartTime()));
  }
}
