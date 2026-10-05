package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import org.junit.jupiter.api.Test;

class ContentReportTest {

  @Test
  void serializesOptionalReason() {
    assertThat(ContentReport.empty().toJson().toJson()).isEqualTo("{}");
    assertThat(ContentReport.withReason("spam").toJson().toJson())
        .isEqualTo("{\"reason\":\"spam\"}");
    assertThat(ContentReport.from(JsonParser.parse("{\"reason\":\"abuse\"}")))
        .extracting(ContentReport::reason)
        .isEqualTo("abuse");
  }

  @Test
  void rejectsInvalidReportJson() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ContentReport.from(null))
        .withMessage("content report must be a JSON object");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ContentReport.from(JsonParser.parse("{\"reason\":1}")))
        .withMessage("report reason must be a string");
  }
}
