package com.lankydanblog.tutorial.event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Event {

  private UUID id;
  private String type;
  private LocalDateTime startTime;
  private double value;

  public Event() {

  }

  public Event(UUID id, String type, LocalDateTime startTime, double value) {
    this.id = id;
    this.type = type;
    this.startTime = startTime;
    this.value = value;
  }

  public UUID getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public double getValue() {
    return value;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public void setValue(double value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if(this == o) return true;
    if(o == null || getClass() != o.getClass()) return false;
    Event event = (Event)o;
    return Double.compare(event.value, value) == 0 &&
             Objects.equals(id, event.id) &&
             Objects.equals(type, event.type) &&
             Objects.equals(startTime, event.startTime);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id, type, startTime, value);
  }

  @Override
  public String toString() {
    return "Event{" +
             "id=" + id +
             ", type='" + type + '\'' +
             ", startTime=" + startTime +
             ", value=" + value +
             '}';
  }
}
