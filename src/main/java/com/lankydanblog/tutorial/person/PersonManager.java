package com.lankydanblog.tutorial.person;

import java.util.UUID;

import com.lankydanblog.tutorial.person.repository.PersonByCountryRepository;
import com.lankydanblog.tutorial.person.repository.PersonRepository;
import com.lankydanblog.tutorial.person.repository.entity.PersonByCountryEntity;
import com.lankydanblog.tutorial.person.repository.entity.PersonByCountryKey;
import com.lankydanblog.tutorial.person.repository.entity.PersonEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PersonManager {

  private final PersonRepository personRepository;
  private final PersonByCountryRepository personByCountryRepository;

  public PersonManager(PersonRepository personRepository, PersonByCountryRepository personByCountryRepository) {
    this.personRepository = personRepository;
    this.personByCountryRepository = personByCountryRepository;
  }

  public Flux<Person> findAll() {
//    return personRepository.findAll().map(this::convertPersonEntityToPerson);
    return personByCountryRepository.findAll().map(this::convertPersonByCountryEntityToPerson);
  }

  public Flux<Person> findAllFromCountry(String country) {
    return personByCountryRepository.findAllByKeyCountry(country).map(this::convertPersonByCountryEntityToPerson);
  }

  private Person convertPersonByCountryEntityToPerson(PersonByCountryEntity personByCountryEntity) {
    return new Person(personByCountryEntity.getKey().getId(), personByCountryEntity.getKey().getFirstName(), personByCountryEntity.getKey().getLastName(), personByCountryEntity.getKey().getCountry(), personByCountryEntity.getAge());
  }

  private Person convertPersonEntityToPerson(PersonEntity personEntity) {
    return new Person(personEntity.getId(), personEntity.getFirstName(), personEntity.getLastName(), personEntity.getCountry(), personEntity.getAge());
  }

  public Mono<Person> findById(final UUID id) {
    return personRepository.findById(id).map(this::convertPersonEntityToPerson);
  }

  public Mono<Person> save(Person person) {
    return Mono.fromSupplier(() -> {
      personRepository.save(convertPersonToPersonEntity(person)).subscribe();
      personByCountryRepository.save(convertPersonToPersonByCountryEntity(person)).subscribe();
      return person;
    });
//    personRepository.save(convertPersonToPersonEntity(person)).map(this::convertPersonEntityToPerson);
//    personByCountryRepository.save(convertPersonToPersonByCountryEntity(person)).ma
  }

  private PersonEntity convertPersonToPersonEntity(Person person) {
    return new PersonEntity(person.getId(), person.getFirstName(), person.getLastName(), person.getCountry(), person.getAge());
  }

  private PersonByCountryEntity convertPersonToPersonByCountryEntity(Person person) {
    return new PersonByCountryEntity(new PersonByCountryKey(person.getCountry(), person.getFirstName(), person.getLastName(), person.getId()), person.getAge());
  }

}
