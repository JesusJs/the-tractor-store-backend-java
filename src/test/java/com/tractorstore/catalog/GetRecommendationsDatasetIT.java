package com.tractorstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.tractorstore.TestcontainersConfiguration;
import com.tractorstore.catalog.application.port.in.GetRecommendationsUseCase;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Reference cases of contracts/catalog-api.md against the real Flyway dataset (SC-003, SC-004). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class GetRecommendationsDatasetIT {

  @Autowired private GetRecommendationsUseCase getRecommendations;

  static Stream<Arguments> referenceCases() {
    return Stream.of(
        Arguments.of("AU-01-SI", List.of("AU-07-MT", "CL-15-SD", "CL-08-GR", "CL-11-SK")),
        Arguments.of("CL-01-GR", List.of("CL-09-GR", "CL-14-GR", "CL-07-GR", "CL-04-RD")),
        Arguments.of(
            "AU-02-OG,AU-02-BL,AU-02-GG", List.of("AU-04-BK", "CL-09-GR", "CL-01-GR", "AU-06-MT")),
        Arguments.of("CL-02-BL,AU-05-ZZ", List.of("CL-13-BL", "AU-03-TQ", "CL-09-BL", "CL-04-TQ")),
        Arguments.of("", List.of("CL-01-GR", "CL-02-BL", "CL-03-GR", "CL-04-BL")),
        Arguments.of("NOPE-00", List.of("CL-01-GR", "CL-02-BL", "CL-03-GR", "CL-04-BL")));
  }

  @ParameterizedTest(name = "[{index}] skus={0}")
  @MethodSource("referenceCases")
  void matchesTheContractReferenceCases(String skus, List<String> expected) {
    List<String> requested = skus.isEmpty() ? List.of() : Arrays.asList(skus.split(","));

    assertThat(getRecommendations.get(requested))
        .extracting(recommendation -> recommendation.variant().sku())
        .containsExactlyElementsOf(expected);
  }
}
