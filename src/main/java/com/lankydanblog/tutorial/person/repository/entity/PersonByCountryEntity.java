package com.lankydanblog.tutorial.person.repository.entity;

import java.util.Objects;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("people_by_country")
public class PersonByCountryEntity {

  @PrimaryKey private PersonByCountryKey key;
  private int age;

  public PersonByCountryEntity(PersonByCountryKey key, int age) {
    this.key = key;
    this.age = age;
  }

  public PersonByCountryKey getKey() {
    return key;
  }

  public void setKey(PersonByCountryKey key) {
    this.key = key;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PersonByCountryEntity that = (PersonByCountryEntity) o;
    return age == that.age && Objects.equals(key, that.key);
  }

  @Override
  public int hashCode() {

    return Objects.hash(key, age);
  }

  @Override
  public String toString() {
    return "PersonByCountryEntity{" + "key=" + key + ", age=" + age + '}';
  }
}
