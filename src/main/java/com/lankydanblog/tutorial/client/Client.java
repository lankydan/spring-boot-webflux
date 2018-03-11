package com.lankydanblog.tutorial.client;

import com.lankydanblog.tutorial.person.Person;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public class Client {

  private WebClient client = WebClient.create("http://localhost:8080");

  public void doStuff() {

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

    final Mono<ClientResponse> getResponse =
        client
            .get()
            .uri("/people/{id}", "63782fe7-1394-437a-89f7-47f89d6046a0")
            .accept(APPLICATION_JSON)
            .exchange();
    getResponse
        .flatMapMany(response -> response.bodyToFlux(Person.class))
        .subscribe(person -> System.out.println("GET: " + person));

    final Mono<ClientResponse> allResponse =
        client.get().uri("/people").accept(APPLICATION_JSON).exchange();
    allResponse
        .flatMapMany(response -> response.bodyToFlux(Person.class))
        .subscribe(person -> System.out.println("ALL: " + person));

    final Person updated = new Person(UUID.randomUUID(), "Laura", "So", "US", 18);
    final Mono<ClientResponse> putResponse =
        client
            .put()
            .uri("/people/{id}", "ec2212fc-669e-42ff-9c51-69782679c9fc")
            .body(Mono.just(updated), Person.class)
            .accept(APPLICATION_JSON)
            .exchange();
    putResponse
        .flatMapMany(response -> response.bodyToFlux(Person.class))
        .subscribe(person -> System.out.println("PUT: " + person));

    final Mono<ClientResponse> deleteResponse =
        client
            .delete()
            .uri("/people/{id}", "ec2212fc-669e-42ff-9c51-69782679c9fc")
            .accept(APPLICATION_JSON)
            .exchange();
    deleteResponse
        .map(ClientResponse::statusCode)
        .subscribe(status -> System.out.println("DELETE: " + status));
  }
}
