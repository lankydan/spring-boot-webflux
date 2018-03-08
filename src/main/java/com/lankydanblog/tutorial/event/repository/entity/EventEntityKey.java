package com.lankydanblog.tutorial.event.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import static org.springframework.data.cassandra.core.cql.Ordering.DESCENDING;
import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.*;

@PrimaryKeyClass
public class EventEntityKey implements Serializable {

  @PrimaryKeyColumn(type = PARTITIONED)
  private String type;
  @PrimaryKeyColumn(name = "start_time", type = CLUSTERED, ordinal = 0, ordering = DESCENDING)
  private LocalDateTime startTime;
  @PrimaryKeyColumn(type = CLUSTERED, ordinal = 1)
  private UUID id;

  public EventEntityKey(final String type, final LocalDateTime startTime, final UUID id) {
    this.type = type;
    this.startTime = startTime;
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
