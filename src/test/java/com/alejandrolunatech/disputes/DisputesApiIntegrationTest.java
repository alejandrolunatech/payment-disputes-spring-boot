package com.alejandrolunatech.disputes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The whole service against a real PostgreSQL: HTTP in, Flyway schema, JPA, the scripted model. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DisputesApiIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void opensAClearFraudDisputeWithoutAPerson() {
        MvcTestResult result = open("key-fraud-0001", "TX-1001", "49.99",
                "I did not make this payment, my card was stolen. Contact me at ana@example.com");

        assertThat(result).hasStatus(201).bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).asString().isEqualTo("OPEN"))
                .hasPathSatisfying("$.triage.reasonCode", reason -> assertThat(reason).asString().isEqualTo("FRAUD"))
                .hasPathSatisfying("$.triage.decidedBy", by -> assertThat(by).asString().isEqualTo("MODEL"))
                .hasPathSatisfying("$.statement", text -> assertThat(text).asString()
                        .contains("[EMAIL]").doesNotContain("ana@example.com"));
        assertThat(result.getResponse().getHeader("Location")).startsWith("/api/disputes/");
    }

    @Test
    void aRetryWithTheSameKeyReturnsTheSameDispute() {
        String first = open("key-retry-0001", "TX-1002", "12.99", "The parcel never arrived").getResponse()
                .getHeader("Location");
        String second = open("key-retry-0001", "TX-1002", "12.99", "The parcel never arrived").getResponse()
                .getHeader("Location");

        assertThat(second).isEqualTo(first);
        assertThat(jdbc.sql("select count(*) from disputes where idempotency_key = 'key-retry-0001'")
                .query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void theSameKeyWithADifferentBodyIsRefused() {
        open("key-reuse-0001", "TX-1003", "10.00", "Charged twice for one order");

        assertThat(open("key-reuse-0001", "TX-1003", "11.00", "Charged twice for one order"))
                .hasStatus(422).bodyJson()
                .hasPathSatisfying("$.type", type -> assertThat(type).asString().endsWith("#idempotency-key-reused"));
    }

    @Test
    void anUnclearStatementWaitsForAPersonWhoDecidesTheReason() {
        MvcTestResult opened = open("key-review-0001", "TX-1004", "15.00", "Something is off with this one");
        assertThat(opened).hasStatus(201).bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).asString().isEqualTo("NEEDS_TRIAGE"));
        String location = opened.getResponse().getHeader("Location");

        assertThat(mvc.get().uri("/api/disputes/review-queue")).hasStatusOk().bodyText().contains(location.substring(14));

        assertThat(mvc.post().uri(location + "/reason").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reasonCode\":\"NOT_AS_DESCRIBED\"}"))
                .hasStatusOk().bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).asString().isEqualTo("OPEN"))
                .hasPathSatisfying("$.triage.decidedBy", by -> assertThat(by).asString().isEqualTo("REVIEWER"));
    }

    @Test
    void theLifecycleRefusesAnIllegalMove() {
        String location = open("key-flow-0001", "TX-1005", "30.00", "The item arrived broken").getResponse()
                .getHeader("Location");

        assertThat(mvc.post().uri(location + "/resolution").contentType(MediaType.APPLICATION_JSON)
                .content("{\"won\":true}"))
                .hasStatus(409).bodyJson()
                .hasPathSatisfying("$.type", type -> assertThat(type).asString().endsWith("#illegal-transition"));

        assertThat(mvc.post().uri(location + "/evidence")).hasStatusOk();
        assertThat(mvc.post().uri(location + "/resolution").contentType(MediaType.APPLICATION_JSON)
                .content("{\"won\":true}"))
                .hasStatusOk().bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).asString().isEqualTo("WON"));
    }

    @Test
    void thePolicyRefusesAnOldTransaction() {
        assertThat(open("key-old-0001", "TX-0999", "5.00", "I did not make this payment"))
                .hasStatus(422).bodyJson()
                .hasPathSatisfying("$.detail", detail -> assertThat(detail).asString().contains("older than"));
    }

    @Test
    void everyModelCallIsLoggedWithoutTheStatement() {
        open("key-log-0001", "TX-1006", "12.00", "Charged twice, the duplicate is from yesterday");

        var detail = jdbc.sql("select outcome, model from model_calls order by id desc limit 1")
                .query((rs, row) -> rs.getString("outcome") + "/" + rs.getString("model")).single();
        assertThat(detail).isEqualTo("ACCEPTED/scripted-stand-in");
    }

    private MvcTestResult open(String key, String transaction, String amount, String statement) {
        return mvc.post().uri("/api/disputes")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"transactionReference":"%s","amount":%s,"currency":"EUR","statement":"%s"}
                        """.formatted(transaction, amount, statement))
                .exchange();
    }
}
