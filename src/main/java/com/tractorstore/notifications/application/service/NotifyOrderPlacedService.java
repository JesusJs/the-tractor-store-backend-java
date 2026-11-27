package com.tractorstore.notifications.application.service;

import com.tractorstore.notifications.application.port.in.NotifyOrderPlacedUseCase;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** There is no real mail server: the confirmation email is a log line (research R13). */
@Service
class NotifyOrderPlacedService implements NotifyOrderPlacedUseCase {

  private static final Logger log = LoggerFactory.getLogger(NotifyOrderPlacedService.class);

  @Override
  public void notify(UUID orderId) {
    log.info("Confirmation email sent for order {}", orderId);
  }
}
