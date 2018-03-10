package com.lankydanblog.tutorial.person;

import java.util.Objects;
import java.util.UUID;

public class Person {

  private UUID id;
  private String firstName;
  private String lastName;
  private String country;
  private int age;

  public Person() {

  }

  public Person(UUID id, String firstName, String lastName, String country, int age) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.country = country;
    this.age = age;
  }

  public Person(Person person, UUID id) {
    this.id = id;
    this.firstName = person.firstName;
    this.lastName = person.lastName;
    this.country = person.country;
    this.age = person.age;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  @Override
  public boolean equals(Object o) {
    if(this == o) return true;
    if(o == null || getClass() != o.getClass()) return false;
    Person person = (Person)o;
    return age == person.age &&
             Objects.equals(id, person.id) &&
             Objects.equals(firstName, person.firstName) &&
             Objects.equals(lastName, person.lastName) &&
             Objects.equals(country, person.country);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id, firstName, lastName, country, age);
  }

  @Override
  public String toString() {
    return "Person{" +
             "id=" + id +
             ", firstName='" + firstName + '\'' +
             ", lastName='" + lastName + '\'' +
             ", country='" + country + '\'' +
             ", age=" + age +
             '}';
  }
}
