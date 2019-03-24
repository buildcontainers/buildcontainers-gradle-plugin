package com.github.buildcontainers.examples.springboot;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyRepository repository;

    @GetMapping
    public List<String> companies() {
        return repository.findAll()
                .stream()
                .map(Company::getName)
                .sorted()
                .collect(Collectors.toList());

    }
}
