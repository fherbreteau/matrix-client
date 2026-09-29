package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class RetryPolicyTest {

  @Test
  void defaultsAndDisabledPoliciesAreBounded() {
    assertThat(RetryPolicy.defaults().maxRetries()).isEqualTo(2);
    assertThat(RetryPolicy.defaults().retries("GET")).isTrue();
    assertThat(RetryPolicy.defaults().retries("POST")).isFalse();
    assertThat(RetryPolicy.disabled().maxRetries()).isZero();
  }

  @Test
  void delaysUseExponentialBackoffAndHonorCappedRetryAfter() {
    var policy = new RetryPolicy(4, 10, 40, 100);
    assertThat(policy.delayMs(1, null)).isEqualTo(10);
    assertThat(policy.delayMs(2, null)).isEqualTo(20);
    assertThat(policy.delayMs(3, null)).isEqualTo(40);
    assertThat(policy.delayMs(1, 75L)).isEqualTo(75);
    assertThat(policy.delayMs(1, 500L)).isEqualTo(100);
    assertThat(policy.delayMs(2, -1L)).isEqualTo(20);
  }

  @Test
  void rejectsInvalidLimits() {
    assertThatIllegalArgumentException().isThrownBy(() -> new RetryPolicy(-1, 0, 0, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> new RetryPolicy(1, -1, 0, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> new RetryPolicy(1, 10, 5, 0));
  }
}
