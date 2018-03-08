package com.lankydanblog.tutorial.event;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.lankydanblog.tutorial.event.repository.EventRepository;
import com.lankydanblog.tutorial.event.repository.entity.EventEntity;
import com.lankydanblog.tutorial.event.repository.entity.EventEntityKey;
import org.springframework.stereotype.Component;

@Component
public class EventCreator {

  private final EventRepository eventRepository;

  public EventCreator(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  //  @Scheduled(fixedRateString = "1000")
  public void create() {
    eventRepository.save(new EventEntity(new EventEntityKey("Transaction", LocalDateTime.now(), UUID.randomUUID()), ThreadLocalRandom.current().nextInt() * 100)).subscribe();
  }
}
