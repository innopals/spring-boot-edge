package com.innopals.edge.demo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bestmike007
 */

@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@SpringBootTest(
	classes = EdgeGatewayServiceApplication.class,
	webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
public class ParameterValidationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	public void validatePostTest() throws Exception {
		mvc.perform(post("/public/api/v1/users")
						.content("{\"name\" : \"a\"}")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.code").value("400"))
						.andExpect(jsonPath("$.message").value("the length of the name should between 5-30!")
						);
	}

	@Test
	public void validateQueryStringTest() throws Exception {
		mvc.perform(get("/public/api/v1/users/199")
						.accept(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.code").value("400")
						);
	}
}
