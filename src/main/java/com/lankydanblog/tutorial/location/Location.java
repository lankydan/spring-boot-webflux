package com.lankydanblog.tutorial.location;

import java.util.Objects;
import java.util.UUID;

public class Location {

  private UUID id;
  private String street;
  private String country;

  public Location(UUID id, String street, String country) {
    this.id = id;
    this.street = street;
    this.country = country;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  @Override
  public String toString() {
    return "Location{" +
            "id=" + id +
            ", street='" + street + '\'' +
            ", country='" + country + '\'' +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Location location = (Location) o;
    return Objects.equals(id, location.id) &&
            Objects.equals(street, location.street) &&
            Objects.equals(country, location.country);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, street, country);
  }
}
