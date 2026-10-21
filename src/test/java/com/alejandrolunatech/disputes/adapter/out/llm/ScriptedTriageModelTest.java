package com.alejandrolunatech.disputes.adapter.out.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alejandrolunatech.disputes.application.port.TriageModel;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ScriptedTriageModelTest {

    final ScriptedTriageModel model = new ScriptedTriageModel();

    static TriageModel.Request request(String statement) {
        return new TriageModel.Request(statement, "Shop", BigDecimal.TEN, "EUR");
    }

    @Test
    void moreCuesMeanMoreConfidence() {
        assertThat(model.propose(request("the parcel never arrived")).confidence()).isEqualTo(0.75);
        assertThat(model.propose(request("not me, my card was stolen")).confidence()).isEqualTo(0.95);
    }

    @Test
    void noCueMeansUnknownWithLowConfidence() {
        var proposal = model.propose(request("please help"));

        assertThat(proposal.reasonCode()).isEqualTo("UNKNOWN");
        assertThat(proposal.confidence()).isEqualTo(0.30);
        assertThat(proposal.usage().model()).isEqualTo(ScriptedTriageModel.NAME);
    }

    @Test
    void readsTheAmountTheStatementMentions() {
        assertThat(model.propose(request("they took € 120,50 from me")).mentionedAmount()).isEqualByComparingTo("120.50");
        assertThat(model.propose(request("no amount here")).mentionedAmount()).isNull();
    }

    @Test
    void canPlayAnOutageForDemos() {
        assertThatThrownBy(() -> model.propose(request("charged twice #outage")))
                .isInstanceOf(TriageModel.UnavailableException.class);
    }
}
