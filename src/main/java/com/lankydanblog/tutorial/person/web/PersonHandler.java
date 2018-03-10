package com.lankydanblog.tutorial.person.web;

import com.lankydanblog.tutorial.person.Person;
import com.lankydanblog.tutorial.person.PersonManager;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class PersonHandler {

  private final PersonManager personManager;

  public PersonHandler(PersonManager personManager) {
    this.personManager = personManager;
  }

  // why use the server response object?

  public Mono<ServerResponse> get(ServerRequest request) {
    final UUID id = UUID.fromString(request.pathVariable("id"));
    final Mono<Person> person = personManager.findById(id);
    //    return ok().contentType(APPLICATION_JSON).body(fromPublisher(person, Person.class));
    return person
        .flatMap(p -> ok().contentType(APPLICATION_JSON).body(fromPublisher(person, Person.class)))
        .switchIfEmpty(notFound().build());
  }

  public Mono<ServerResponse> all(ServerRequest request) {
    return ok().contentType(APPLICATION_JSON)
        .body(fromPublisher(personManager.findAll(), Person.class));
  }
  //  Error:(50, 47) java: incompatible types: inference variable R has incompatible bounds
  //  equality constraints: org.springframework.web.reactive.function.server.ServerResponse
  //  lower bounds: reactor.core.publisher.Mono<R>

  // put is problematic for cassandra because the primary key fields cannot be updated. This will
  // cause issues in the people_by_country table due to the name fields included in the primary key
  // so either I don't use them in the primary key, dont let them be updated or drop and recreate
  // the existing record. Go for drop and recreate for now, this can be done within the manager to
  // hide
  // the logic from this class.

  // so the web method will update the existing record if it exits and return a 200 status code,
  // otherwise it will return a 404 error.
  public Mono<ServerResponse> put(ServerRequest request) {
    final UUID id = UUID.fromString(request.pathVariable("id"));
    final Mono<Person> person = request.bodyToMono(Person.class);
    // docs use otherwiseIfEmpty but I cannot find this method and defaultIfEmpty cannot take in a
    // Mono.
    //    Mono<ServerResponse> notFound = ServerResponse.notFound().build();
    //        return personManager.findById(id).then(saved -> ServerResponse
    //                 .ok()
    //                 .contentType(TEXT_PLAIN)
    //                 .body(fromPublisher(person.flatMap(p -> personManager.save(p)),
    // Person.class)).defaultIfEmpty(ServerResponse.notFound().build()));

    // seems like flatMap is normally the magic keyword to fix everything
//    return personManager
//        .findById(id)
//        .flatMap(
//            saved ->
//                ok().contentType(APPLICATION_JSON)
//                    .body(
//                        fromPublisher(
//                            person.map(p -> new Person(p, id)).flatMap(p -> personManager.update(p)),
//                            Person.class))
//                    .switchIfEmpty(notFound().build()));
    return personManager
            .findById(id)
            .flatMap(
                    saved ->
                            ok().contentType(APPLICATION_JSON)
                                    .body(
                                            fromPublisher(
                                                    person.map(p -> new Person(p, id)).flatMap(p -> personManager.update(saved, p)),
                                                    Person.class))
                                    .switchIfEmpty(notFound().build()));

    //    return personManager.findById(id).then(saved -> ServerResponse
    //                                                      .ok()
    //                                                      .contentType(TEXT_PLAIN)
    //                                                      .body(fromPublisher(person.flatMap(p ->
    // personManager.save(p)), Person.class))
    //                                                      .defaultIfEmpty(ServerResponse
    //                                                                              .ok()
    //
    // .contentType(TEXT_PLAIN)
    //
    // .body(fromPublisher(person.flatMap(ppp -> personManager.save(ppp)), Person.class))));
  }

  public Mono<ServerResponse> post(ServerRequest request) {
    final Mono<Person> person = request.bodyToMono(Person.class);
    final UUID id = UUID.randomUUID();
    return created(UriComponentsBuilder.fromPath("person/get/" + id).build().toUri())
        .contentType(APPLICATION_JSON)
        .body(
            fromPublisher(
                person.map(p -> new Person(p, id)).flatMap(p -> personManager.save(p)),
                Person.class));
  }

  // publish compiles but not 100% sure what it is doing
  //    Mono<ServerResponse> response = person.publish(p -> ServerResponse
  //
  // .created(UriComponentsBuilder.fromPath("events/get/").build().toUri())
  //                                                 .contentType(TEXT_PLAIN)
  //                                                 .body(BodyInserters.empty()));
  //     Mono<ServerResponse> response = person
  //             .map(p -> personManager.save(p)
  //                         .map((saved) -> ServerResponse
  //                                           .created(UriComponentsBuilder.fromPath("events/get/"
  // + saved.getId()).build().toUri())
  //                                           .contentType(TEXT_PLAIN)
  //                                           .body(fromPublisher(person, Person.class))));
  //    return person.map(p -> personManager.save(p)).map((Mono<Person> saved) ->
  // ServerResponse.created(UriComponentsBuilder.fromPath("events/get/" +
  // saved.getId()).build().toUri()).contentType(TEXT_PLAIN).body(fromPublisher(person,
  // Person.class)));

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
