package archfixtures.appspring.shop.application;

import org.springframework.context.ApplicationEventPublisher;

/** Violation: application uses Spring beyond {@code @Service} and {@code @Transactional}. */
public class PublishingService {

  ApplicationEventPublisher publisher;
}
