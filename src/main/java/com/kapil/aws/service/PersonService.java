package com.kapil.aws.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.kapil.aws.entities.Person;
import com.kapil.aws.repository.PersonRepository;

@Service
public class PersonService {

    private static final Logger logger = LoggerFactory.getLogger(PersonService.class);

    @Autowired
    private PersonRepository personRepository;

    // Cache the list of persons. This can be done depending on the frequency of changes.
  //  @Cacheable(value = "persons", key = "'allPersons'")
    public List<Person> getPersons() {
        logger.info("Get key persons information list");
        return personRepository.findAll();
    }

    // Cache the result of fetching person by ID. Use a unique key based on the ID.
    //@Cacheable(value = "persons", key = "#id")
    public Optional<Person> getPersonById(int id) {
    	logger.info("Get person information for person id: " + id);
        return personRepository.findById(id);
    }

    // Update the cache after saving a new person or updating an existing person.
   // @CachePut(cacheNames = "persons", key = "#person.id")
    public Person savePerson(Person person) {
        logger.info("Successfully save person details " + person.toString());
        return personRepository.save(person);
    }

    // Remove the cache entry for the specific person when deleted.
   // @CacheEvict(cacheNames = "persons", key = "#id")
    public void removePerson(int id) {
        logger.info("Deleting person information with id: " + id);
        personRepository.deleteById(id);
    }

    // Optionally, you can use @CacheEvict with `allEntries = true` for cache invalidation after certain operations.
    //@CacheEvict(cacheNames = "persons", allEntries = true)
    public void removeAllPersons() {
    	logger.info("Delete all persons and clear the cache");
        personRepository.deleteAll();
    }
}
