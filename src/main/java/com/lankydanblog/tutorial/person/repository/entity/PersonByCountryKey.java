package com.lankydanblog.tutorial.person.repository.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.*;

@PrimaryKeyClass
public class PersonByCountryKey implements Serializable {

  @PrimaryKeyColumn(type = PARTITIONED)
  private String country;

  @PrimaryKeyColumn(name = "first_name", type = CLUSTERED, ordinal = 0)
  private String firstName;

  @PrimaryKeyColumn(name = "last_name", type = CLUSTERED, ordinal = 1)
  private String lastName;

  @PrimaryKeyColumn(name = "person_id", type = CLUSTERED, ordinal = 2)
  private UUID id;

  public PersonByCountryKey(String country, String firstName, String lastName, UUID id) {
    this.country = country;
    this.firstName = firstName;
    this.lastName = lastName;
    this.id = id;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
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

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PersonByCountryKey that = (PersonByCountryKey) o;
    return Objects.equals(country, that.country)
        && Objects.equals(firstName, that.firstName)
        && Objects.equals(lastName, that.lastName)
        && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {

    return Objects.hash(country, firstName, lastName, id);
  }

  @Override
  public String toString() {
    return "PersonByCountryKey{"
        + "country='"
        + country
        + '\''
        + ", firstName='"
        + firstName
        + '\''
        + ", lastName='"
        + lastName
        + '\''
        + ", id="
        + id
        + '}';
  }
}
