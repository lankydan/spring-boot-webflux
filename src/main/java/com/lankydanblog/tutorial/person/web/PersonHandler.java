package com.lankydanblog.tutorial.person.web;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.lankydanblog.tutorial.person.Person;
import com.lankydanblog.tutorial.person.PersonManager;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;

@Component
public class PersonHandler {

  private final PersonManager personManager;

  public PersonHandler(PersonManager personManager) {
    this.personManager = personManager;
  }

  // why use the server response object?

  public Mono<ServerResponse> get(ServerRequest request) {
    final UUID id = UUID.fromString(request.pathVariable("id"));
    final Mono<Person> events = personManager.findById(id);
    return ServerResponse.ok()
             .contentType(APPLICATION_JSON)
             .body(fromPublisher(events, Person.class));
  }

  public Mono<ServerResponse> all(ServerRequest request) {
    return ServerResponse.ok()
             .contentType(APPLICATION_JSON)
             .body(fromPublisher(personManager.findAll(), Person.class));
  }
  //  Error:(50, 47) java: incompatible types: inference variable R has incompatible bounds
  //  equality constraints: org.springframework.web.reactive.function.server.ServerResponse
  //  lower bounds: reactor.core.publisher.Mono<R>

  // put is problematic for cassandra because the primary key fields cannot be updated. This will cause issues in the people_by_country table due to the name fields included in the primary key
  // so either I don't use them in the primary key, dont let them be updated or drop and recreate the existing record. Go for drop and recreate for now, this can be done within the manager to hide
  // the logic from this class.

  // so the web method will update the existing record if it exits and return a 200 status code, otherwise it will return a 404 error.
  public Mono<ServerResponse> put(ServerRequest request) {
    final UUID id = UUID.fromString(request.pathVariable("id"));
    final Mono<Person> person = request.bodyToMono(Person.class);
    // docs use otherwiseIfEmpty but I cannot find this method and defaultIfEmpty cannot take in a Mono.
    //    Mono<ServerResponse> notFound = ServerResponse.notFound().build();
    //        return personManager.findById(id).then(saved -> ServerResponse
    //                 .ok()
    //                 .contentType(TEXT_PLAIN)
    //                 .body(fromPublisher(person.flatMap(p -> personManager.save(p)), Person.class)).defaultIfEmpty(ServerResponse.notFound().build()));

    // seems like flatMap is normally the magic keyword to fix everything
    return personManager.findById(id)
             .flatMap(saved -> ServerResponse
                                 .ok()
                                 .contentType(TEXT_PLAIN)
                                 .body(fromPublisher(person.flatMap(p -> personManager.save(p)), Person.class))
                                 .switchIfEmpty(ServerResponse.notFound().build()));

    //    return personManager.findById(id).then(saved -> ServerResponse
    //                                                      .ok()
    //                                                      .contentType(TEXT_PLAIN)
    //                                                      .body(fromPublisher(person.flatMap(p -> personManager.save(p)), Person.class))
    //                                                      .defaultIfEmpty(ServerResponse
    //                                                                              .ok()
    //                                                                              .contentType(TEXT_PLAIN)
    //                                                                              .body(fromPublisher(person.flatMap(ppp -> personManager.save(ppp)), Person.class))));
  }

  public Mono<ServerResponse> post(ServerRequest request) {
    final Mono<Person> person = request.bodyToMono(Person.class);
    final UUID id = UUID.randomUUID();
    Mono<ServerResponse> response = ServerResponse
                                      .created(UriComponentsBuilder.fromPath("person/get/" + id).build().toUri())
                                      .contentType(TEXT_PLAIN).body(fromPublisher(person.flatMap(p -> personManager.save(p)), Person.class));
    return response;
  }

  ;
  // publish compiles but not 100% sure what it is doing
  //    Mono<ServerResponse> response = person.publish(p -> ServerResponse
  //                                                 .created(UriComponentsBuilder.fromPath("events/get/").build().toUri())
  //                                                 .contentType(TEXT_PLAIN)
  //                                                 .body(BodyInserters.empty()));
  //     Mono<ServerResponse> response = person
  //             .map(p -> personManager.save(p)
  //                         .map((saved) -> ServerResponse
  //                                           .created(UriComponentsBuilder.fromPath("events/get/" + saved.getId()).build().toUri())
  //                                           .contentType(TEXT_PLAIN)
  //                                           .body(fromPublisher(person, Person.class))));
  //    return person.map(p -> personManager.save(p)).map((Mono<Person> saved) -> ServerResponse.created(UriComponentsBuilder.fromPath("events/get/" + saved.getId()).build().toUri()).contentType(TEXT_PLAIN).body(fromPublisher(person, Person.class)));
}
