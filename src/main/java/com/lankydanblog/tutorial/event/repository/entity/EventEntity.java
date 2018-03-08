package com.lankydanblog.tutorial.event.repository.entity;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("events")
public class EventEntity {

  @PrimaryKey private EventEntityKey key;
  private double value;

  public EventEntity(final EventEntityKey key, final double value) {
    this.key = key;
    this.value = value;
  }

  public EventEntityKey getKey() {
    return key;
  }

  public void setKey(EventEntityKey key) {
    this.key = key;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }
}
