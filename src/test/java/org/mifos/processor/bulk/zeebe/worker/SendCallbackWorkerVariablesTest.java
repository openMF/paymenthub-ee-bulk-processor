package org.mifos.processor.bulk.zeebe.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * SendCallbackWorker is the last worker in the batch flow. Before PHEE-428 it parsed four job variables with
 * Integer.parseInt(variables.get(..).toString()), so a missing one failed the job with a NullPointerException that
 * named nothing, and a decimal completionRate ("50.0") with a NumberFormatException.
 */
class SendCallbackWorkerVariablesTest {

    @Test
    void readsIntegersAndDecimals() {
        assertThat(SendCallbackWorker.intVariable(Map.of("retry", 3), "retry", null)).isEqualTo(3);
        assertThat(SendCallbackWorker.intVariable(Map.of("completionRate", "50.0"), "completionRate", null)).isEqualTo(50);
        assertThat(SendCallbackWorker.intVariable(Map.of("completionRate", 66.67f), "completionRate", null)).isEqualTo(66);
    }

    @Test
    void usesDefaultWhenMissing() {
        assertThat(SendCallbackWorker.intVariable(Map.of(), "retry", 0)).isZero();
    }

    @Test
    void namesTheVariableWhenMissingOrInvalid() {
        assertThatThrownBy(() -> SendCallbackWorker.intVariable(Map.of(), "maxStatusRetry", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Missing job variable maxStatusRetry");
        assertThatThrownBy(() -> SendCallbackWorker.intVariable(Map.of("completionThreshold", "high"), "completionThreshold", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Job variable completionThreshold is not a number: high");
    }
}
