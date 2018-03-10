package com.lankydanblog.tutorial.person.repository;

import com.lankydanblog.tutorial.person.repository.entity.PersonByCountryEntity;
import com.lankydanblog.tutorial.person.repository.entity.PersonByCountryKey;
import com.lankydanblog.tutorial.person.repository.entity.PersonEntity;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface PersonByCountryRepository extends ReactiveCassandraRepository<PersonByCountryEntity, PersonByCountryKey> {

  Flux<PersonByCountryEntity> findAllByKeyCountry(final String country);
}
