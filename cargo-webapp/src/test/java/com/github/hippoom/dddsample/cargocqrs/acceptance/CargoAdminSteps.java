package com.github.hippoom.dddsample.cargocqrs.acceptance;

import static com.github.dreamhead.moco.Moco.and;
import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.eq;
import static com.github.dreamhead.moco.Moco.httpserver;
import static com.github.dreamhead.moco.Moco.query;
import static com.github.dreamhead.moco.Moco.uri;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dreamhead.moco.HttpServer;
import com.github.dreamhead.moco.internal.ActualHttpServer;
import com.github.dreamhead.moco.internal.MocoHttpServer;
import com.github.hippoom.dddsample.cargocqrs.rest.CargoDto;
import com.github.hippoom.dddsample.cargocqrs.rest.RouteCandidateDto;

import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@WebAppConfiguration
@ContextConfiguration("classpath:acceptance.xml")
public class CargoAdminSteps implements ApplicationContextAware {

	@Autowired
	private WebApplicationContext wac;

	private MocoHttpServer moco;

	private ApplicationContext applicationContext;

	private String trackingId;

	private CargoDto cargo;

	private List<RouteCandidateDto> routeCandidates;

	@When("^I fill the form with origin, destination and arrival deadline$")
	public void I_fill_the_form_with_origin_destination_and_arrival_deadline()
			throws Throwable {
		this.trackingId = aNewCargoIsRegistered();

	}

	private String aNewCargoIsRegistered() throws Exception {
		final MvcResult result = mockMvc()
				.perform(
						put("/cargo")
								.content(
										json("classpath:acceptance_route_specification.json"))
								.contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isOk()).andReturn();

		return new ObjectMapper().readValue(result.getResponse()
				.getContentAsByteArray(), String.class);

	}

	private String json(String file) throws JsonParseException,
			JsonMappingException, IOException {
		return new String(IOUtils.toByteArray(applicationContext.getResource(
				file).getInputStream()), "UTF-8");
	}

	private MockMvc mockMvc() {
		return webAppContextSetup(this.wac).build();
	}

	@Then("^a new cargo is registered$")
	public void a_new_cargo_is_registered() throws Throwable {

	}

	@Then("^the tracking id is shown for following steps$")
	public void the_tracking_id_is_shown_for_following_steps() throws Throwable {
		assertThat(trackingId, not(nullValue()));
	}

	@Given("^a cargo has been registered$")
	public void a_cargo_has_been_registered() throws Throwable {
		this.trackingId = aNewCargoIsRegistered();
	}

	@Given("^I request possible routes for the cargo$")
	public void I_request_possible_routes_for_the_cargo() throws Throwable {

		HttpServer server = httpserver(10001);
		server.get(
				and(by(uri("/pathfinder/shortestPath")),
						eq(query("spec"),
								json("classpath:acceptance_route_specification.json"))))
				.response(json("classpath:acceptance_pathfinder_stub.json"));

		moco = new MocoHttpServer((ActualHttpServer) server);
		moco.start();

		final MvcResult result = mockMvc()
				.perform(get("/routes/" + this.trackingId)).andDo(print())
				.andExpect(status().isOk()).andReturn();

		routeCandidates = new ObjectMapper().readValue(result.getResponse()
				.getContentAsByteArray(),
				new TypeReference<List<RouteCandidateDto>>() {
				});
		moco.stop();
	}

	@Given("^some routes are shown$")
	public void some_routes_are_shown() throws Throwable {
		assertThat(routeCandidates, hasSize(greaterThan(1)));
	}

	@When("^I pick up a candidate$")
	public void I_pick_up_a_candidate() throws Throwable {
		// Express the Regexp above with the code you wish you had
		throw new PendingException();
	}

	@Then("^the cargo is assigned to the route$")
	public void the_cargo_is_assigned_to_the_route() throws Throwable {
		final MvcResult result = mockMvc()
				.perform(get("/cargo/" + this.trackingId)).andDo(print())
				.andExpect(status().isOk()).andReturn();

		this.cargo = new ObjectMapper().readValue(result.getResponse()
				.getContentAsByteArray(), CargoDto.class);
		throw new PendingException();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}
}
