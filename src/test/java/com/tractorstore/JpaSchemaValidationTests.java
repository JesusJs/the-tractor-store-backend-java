package com.tractorstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

/**
 * Hibernate never creates or alters the schema: Flyway owns it (FR-021, FR-028, review 001 #10).
 */
class JpaSchemaValidationTests {

  private static final Path RESOURCES = Path.of("src/main/resources");
  private static final String DDL_AUTO = "spring.jpa.hibernate.ddl-auto";
  private static final String GENERATE_DDL = "spring.jpa.generate-ddl";

  static Stream<Path> profileFiles() throws IOException {
    try (Stream<Path> files = Files.list(RESOURCES)) {
      return files
          .filter(file -> file.getFileName().toString().matches("application-.+\\.ya?ml"))
          .toList()
          .stream();
    }
  }

  @Test
  void baseConfigurationValidatesTheSchema() throws IOException {
    List<PropertySource<?>> base = load(RESOURCES.resolve("application.yml"));

    assertThat(property(base, DDL_AUTO)).isEqualTo("validate");
    assertThat(property(base, GENERATE_DDL)).isNull();
  }

  @Test
  void thereAreProfilesToCheck() throws IOException {
    assertThat(profileFiles()).isNotEmpty();
  }

  @ParameterizedTest
  @MethodSource("profileFiles")
  void noProfileOverridesSchemaValidation(Path file) throws IOException {
    List<PropertySource<?>> profile = load(file);

    assertThat(property(profile, DDL_AUTO)).isIn(null, "validate");
    assertThat(property(profile, GENERATE_DDL)).isIn(null, "false", false);
  }

  private static List<PropertySource<?>> load(Path file) throws IOException {
    return new YamlPropertySourceLoader()
        .load(file.getFileName().toString(), new FileSystemResource(file));
  }

  private static Object property(List<PropertySource<?>> sources, String name) {
    return sources.stream()
        .map(source -> source.getProperty(name))
        .filter(value -> value != null)
        .map(value -> value instanceof CharSequence text ? text.toString() : value)
        .findFirst()
        .orElse(null);
  }
}
