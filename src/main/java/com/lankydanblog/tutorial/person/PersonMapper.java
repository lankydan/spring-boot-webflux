package com.lankydanblog.tutorial.person;

import com.lankydanblog.tutorial.person.repository.entity.PersonByCountryEntity;
import com.lankydanblog.tutorial.person.repository.entity.PersonByCountryKey;
import com.lankydanblog.tutorial.person.repository.entity.PersonEntity;

class PersonMapper {

  private PersonMapper() {}

  static Person toPerson(PersonByCountryEntity personByCountryEntity) {
    return new Person(
        personByCountryEntity.getKey().getId(),
        personByCountryEntity.getKey().getFirstName(),
        personByCountryEntity.getKey().getLastName(),
        personByCountryEntity.getKey().getCountry(),
        personByCountryEntity.getAge());
  }

  static Person toPerson(PersonEntity personEntity) {
    return new Person(
        personEntity.getId(),
        personEntity.getFirstName(),
        personEntity.getLastName(),
        personEntity.getCountry(),
        personEntity.getAge());
  }

  static PersonEntity toPersonEntity(Person person) {
    return new PersonEntity(
        person.getId(),
        person.getFirstName(),
        person.getLastName(),
        person.getCountry(),
        person.getAge());
  }

  static PersonByCountryEntity toPersonByCountryEntity(Person person) {
    return new PersonByCountryEntity(
        new PersonByCountryKey(
            person.getCountry(), person.getFirstName(), person.getLastName(), person.getId()),
        person.getAge());
  }
}
