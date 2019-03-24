package com.github.buildcontainers.examples.springboot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.transaction.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.MOCK, properties = {
		"spring.datasource.url=jdbc:tc:postgresql:11-alpine:///databasename",
		"spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
})
@AutoConfigureMockMvc
@AutoConfigureTestEntityManager
public class ApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
	private TestEntityManager entityManager;

	@Test
	@Transactional
	public void should_return_list_of_companies() throws Exception {
		//given
		saveCompanies(List.of("Company 1", "Company 2"));

		//when
		ResultActions resultActions = mockMvc.perform(get("/companies"));

		//then
		resultActions.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0]").value("Company 1"))
				.andExpect(jsonPath("$.[1]").value("Company 2"));
	}

	private void saveCompanies(List<String> names) {
		names.stream()
			.map(Company::new)
			.forEach(entityManager::persist);
	}

}
