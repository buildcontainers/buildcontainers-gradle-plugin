package com.github.buildcontainers.examples.springboot;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CompanyRepository extends CrudRepository<Company, Long> {
    List<Company> findAll();
}
