package com.lankydanblog.tutorial.person.repository.entity;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

@Table("people")
public class PersonEntity {

  @PrimaryKey("person_id")
  private UUID id;
  private String firstName;
  private String lastName;
  private String country;
  private int age;

  public PersonEntity(UUID id, String firstName, String lastName, String country, int age) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.country = country;
    this.age = age;
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
    PersonEntity that = (PersonEntity)o;
    return age == that.age &&
             Objects.equals(id, that.id) &&
             Objects.equals(firstName, that.firstName) &&
             Objects.equals(lastName, that.lastName) &&
             Objects.equals(country, that.country);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id, firstName, lastName, country, age);
  }

  @Override
  public String toString() {
    return "PersonEntity{" +
             "id=" + id +
             ", firstName='" + firstName + '\'' +
             ", lastName='" + lastName + '\'' +
             ", country='" + country + '\'' +
             ", age=" + age +
             '}';
  }
}
