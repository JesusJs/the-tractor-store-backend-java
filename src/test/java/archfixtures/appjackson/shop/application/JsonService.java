package archfixtures.appjackson.shop.application;

import tools.jackson.databind.ObjectMapper;

/** Violation: a use case that serializes JSON itself (an adapter concern). */
public class JsonService {

  ObjectMapper mapper;
}
