package com.kapil.aws.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kapil.aws.entities.Person;

public interface PersonRepository extends JpaRepository<Person, Integer> {
}

