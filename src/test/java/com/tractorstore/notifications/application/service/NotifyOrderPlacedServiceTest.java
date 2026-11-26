package com.tractorstore.notifications.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class NotifyOrderPlacedServiceTest {

  private static final UUID ORDER = UUID.fromString("0b6c1e2f-3a4d-4b5c-8d7e-9f0a1b2c3d4e");

  @Test
  void logsTheConfirmationEmailAtInfo(CapturedOutput output) {
    new NotifyOrderPlacedService().notify(ORDER);

    assertThat(output.getOut())
        .containsPattern("INFO .*Confirmation email sent for order " + ORDER);
  }
}
