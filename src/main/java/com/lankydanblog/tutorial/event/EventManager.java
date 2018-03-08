package com.lankydanblog.tutorial.event;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.lankydanblog.tutorial.event.repository.EventRepository;
import com.lankydanblog.tutorial.event.repository.entity.EventEntity;
import com.lankydanblog.tutorial.event.repository.entity.EventEntityKey;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class EventManager {

  private final EventRepository eventRepository;

  public EventManager(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  public Flux<Event> findAll() {
    System.out.println("FIND ALL EXECUTED");
    return eventRepository.findAll().map(this::create);
  }

  public Flux<Event> findAllOfType(String type) {
    System.out.println("FIND ALL OF TYPE EXECUTED");
    return eventRepository.findAllByKeyType(type).map(this::create);
  }

  public Flux<Event> findAllOfTypeAfterStartTime(String type, LocalDateTime time) {
    System.out.println("FIND ALL OF TYPE AFTER START TIME EXECUTED");
    return eventRepository.findAllByTypeAfterStartTime(type, time).map(this::create);
  }

  private Event create(EventEntity eventEntity) {
    return new Event(eventEntity.getKey().getId(), eventEntity.getKey().getType(), eventEntity.getKey().getStartTime(), eventEntity.getValue());
  }

  public void save(Event event) {
    eventRepository.save(create(event)).subscribe();
  }

  public Mono<Event> save(Event event) {
    eventRepository.save(create(event)).map(this::create);
  }

  private EventEntity create(Event event) {
    return new EventEntity(new EventEntityKey(event.getType(), event.getStartTime(), event.getId()), event.getValue());
  }

//  @Scheduled(fixedRateString = "1000")
  public void insertData() {
    eventRepository.save(new EventEntity(new EventEntityKey("Transaction", LocalDateTime.now(), UUID.randomUUID()), ThreadLocalRandom.current().nextInt() * 100)).subscribe();
  }
}
