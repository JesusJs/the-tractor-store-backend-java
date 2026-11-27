package archfixtures.apphibernate.shop.application;

import org.hibernate.Session;

/** Violation: a use case that depends on Hibernate. */
public class HibernateSessionService {

  Session session;
}
