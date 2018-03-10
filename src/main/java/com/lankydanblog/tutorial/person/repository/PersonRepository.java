package com.lankydanblog.tutorial.person.repository;

import java.util.UUID;

import com.lankydanblog.tutorial.person.repository.entity.PersonEntity;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends ReactiveCassandraRepository<PersonEntity, UUID> {
}
