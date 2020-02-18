package org.zendesk.client.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.*;
import org.zendesk.client.v2.model.*;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * An initial attempt at a unit test that uses wiremock to test the client without requiring a running zendesk client
 * @author rbolles on 2/8/18.
 */
public class TicketsIncrementallyTest {

    private static final String MOCK_URL_FORMATTED_STRING = "http://localhost:%d";
    public static final RandomStringGenerator RANDOM_STRING_GENERATOR =
            new RandomStringGenerator.Builder().withinRange('a', 'z').build();
    private static final String MOCK_API_TOKEN = RANDOM_STRING_GENERATOR.generate(15);
    private static final String MOCK_USERNAME = RANDOM_STRING_GENERATOR.generate(10).toLowerCase() + "@cloudbees.com";

    @ClassRule
    public static WireMockClassRule zendeskApiClass = new WireMockClassRule(options()
            .dynamicPort()
            .dynamicHttpsPort()
    );

    @Rule
    public WireMockClassRule zendeskApiMock = zendeskApiClass;

    private Zendesk client;
    //use a mapper that is identical to what the client will use
    private ObjectMapper objectMapper = Zendesk.createMapper();


    @Before
    public void setUp() throws Exception {
        int ephemeralPort = zendeskApiMock.port();

        String hostname = String.format(MOCK_URL_FORMATTED_STRING, ephemeralPort);

        client = new Zendesk.Builder(hostname)
                .setUsername(MOCK_USERNAME)
                .setToken(MOCK_API_TOKEN)
                .build();
    }

    @After
    public void closeClient() {
        if (client != null) {
            client.close();
        }
        client = null;
    }


    @Test
    public void getTicketsIncrementally() throws JsonProcessingException, UnsupportedEncodingException {

        Date startTime = new Date();
        List<Zendesk.SideLoadingHandler> sideLoadingHandlers = new ArrayList<>();
        Zendesk.SideLoadingHandler<Ticket, User> userSideLoadingHandler = client
                .new SideLoadingHandler(User.class, "users", client.handleTicketsAndUsers());
        sideLoadingHandlers.add(userSideLoadingHandler);
        Zendesk.SideLoadingHandler<Ticket, Metric> metricsSideLoadingHandler = client
                .new SideLoadingHandler(Metric.class, "metric_sets", client.handleTicketsAndMetrics());
        sideLoadingHandlers.add(metricsSideLoadingHandler);
        List<String> sideLoading = sideLoadingHandlers.stream().map(sideLoadingHandler -> sideLoadingHandler.getName()).collect(Collectors.toList());

        Random r = new Random(System.currentTimeMillis());
        Ticket expectedTicket1 = new Ticket();
        Long ticketId1 = Math.abs(r.nextLong());
        expectedTicket1.setId(ticketId1);
        Long submitter1Id = Math.abs(r.nextLong());
        expectedTicket1.setSubmitterId(submitter1Id);

        Ticket expectedTicket2 = new Ticket();
        Long ticketId2 = Math.abs(r.nextLong());
        expectedTicket2.setId(ticketId2);
        Long submitter2Id = Math.abs(r.nextLong());
        expectedTicket2.setSubmitterId(submitter2Id);

        User expectedUser1 = new User();
        expectedUser1.setId(submitter1Id);
        expectedUser1.setName("test1");

        User expectedUser2 = new User();
        expectedUser2.setId(submitter2Id);
        expectedUser1.setName("test2");

        Metric expectedMetric1 = new Metric();
        expectedMetric1.setId(Math.abs(r.nextLong()));
        expectedMetric1.setTicketId(ticketId1);

        Metric expectedMetric2 = new Metric();
        expectedMetric2.setId(Math.abs(r.nextLong()));
        expectedMetric2.setTicketId(ticketId2);

        IncrementalResultsTUM incrementalResults = new IncrementalResultsTUM();
        incrementalResults.setTickets(Arrays.asList(expectedTicket1, expectedTicket2));
        incrementalResults.setUsers(Arrays.asList(expectedUser1, expectedUser2));
        incrementalResults.setMetricSets(Arrays.asList(expectedMetric1, expectedMetric2));


        zendeskApiMock.stubFor(
                get(
                        urlPathEqualTo("/api/v2/incremental/tickets.json"))
                        .withQueryParam("start_time", equalTo(String.valueOf(msToSeconds(startTime.getTime()))))
                        .withQueryParam("include", equalTo(String.join(",", sideLoading)))
                        .willReturn(ok()
                                .withBody(objectMapper.writeValueAsString(incrementalResults))
                        )

        );

        Iterable<Ticket> iterableResults = client.getTicketsIncrementallyV2(startTime, sideLoadingHandlers, new IncrementalHandler() {
            @Override
            public void handleEndTime(Long endTime) {
                //do something
            }
        });

        List<Ticket> actualResults = new ArrayList<>();
        Iterator<Ticket> ticketIterator = iterableResults.iterator();
        while(ticketIterator.hasNext()) {
            actualResults.add(ticketIterator.next());
        }

        assertThat(actualResults).as("actual results")
                .isNotNull()
                .hasSize(2)
                .extracting("id")
                .containsExactly(expectedTicket1.getId(), expectedTicket2.getId());
         assertThat(actualResults).as("actual results")
                .extracting("submitterName")
                .containsExactly(expectedUser1.getName(), expectedUser2.getName());
        assertThat(actualResults).as("actual results")
                .extracting("metric.id")
                .containsExactly(expectedMetric1.getId(), expectedMetric2.getId());
    }

    private static long msToSeconds(long millis) {
        return TimeUnit.MILLISECONDS.toSeconds(millis);
    }
}
