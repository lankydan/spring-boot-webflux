package com.lankydanblog.tutorial.event.repository;

import java.time.LocalDateTime;

import com.lankydanblog.tutorial.event.repository.entity.EventEntity;
import com.lankydanblog.tutorial.event.repository.entity.EventEntityKey;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface EventRepository extends ReactiveCassandraRepository<EventEntity, EventEntityKey> {

  Flux<EventEntity> findAllByKeyType(final String type);

  @Query("select * from events where type = ?0 and start_time > ?1")
  Flux<EventEntity> findAllByTypeAfterStartTime(final String type, final LocalDateTime startTime);
}
