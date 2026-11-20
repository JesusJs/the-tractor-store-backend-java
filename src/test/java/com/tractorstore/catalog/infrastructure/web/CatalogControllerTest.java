package com.tractorstore.catalog.infrastructure.web;

import static com.tractorstore.catalog.CatalogFixtures.AUTONOMOUS;
import static com.tractorstore.catalog.CatalogFixtures.CLASSIC;
import static com.tractorstore.catalog.CatalogFixtures.au07;
import static com.tractorstore.catalog.CatalogFixtures.cl01;
import static com.tractorstore.catalog.CatalogFixtures.storeA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tractorstore.catalog.application.CategoryListing;
import com.tractorstore.catalog.application.GetCategoryListing;
import com.tractorstore.catalog.application.GetHomeTeasers;
import com.tractorstore.catalog.application.GetProductDetail;
import com.tractorstore.catalog.application.GetRecommendations;
import com.tractorstore.catalog.application.ListStores;
import com.tractorstore.catalog.domain.CategoryNotFoundException;
import com.tractorstore.catalog.domain.ProductNotFoundException;
import com.tractorstore.catalog.domain.Recommendation;
import com.tractorstore.shared.infrastructure.web.GlobalExceptionHandler;
import com.tractorstore.shared.infrastructure.web.TraceIdFilter;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(CatalogController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
class CatalogControllerTest {

  private static final String BASE = "/api/v1/catalog";

  @Autowired private MockMvc mvc;

  @MockitoBean private GetHomeTeasers getHomeTeasers;
  @MockitoBean private GetCategoryListing getCategoryListing;
  @MockitoBean private ListStores listStores;
  @MockitoBean private GetProductDetail getProductDetail;
  @MockitoBean private GetRecommendations getRecommendations;

  @Nested
  class Home {

    @Test
    void returnsOneTeaserPerCategory() throws Exception {
      when(getHomeTeasers.get()).thenReturn(List.of(CLASSIC, AUTONOMOUS));

      mvc.perform(get(BASE + "/home"))
          .andExpect(status().isOk())
          .andExpect(header().exists(TraceIdFilter.HEADER))
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].id").value("classic"))
          .andExpect(jsonPath("$[0].title").value("Classic Tractors"))
          .andExpect(jsonPath("$[0].image").value(CLASSIC.teaserImage()))
          .andExpect(jsonPath("$[0].filter").value("classic"))
          .andExpect(jsonPath("$[1].filter").value("autonomous"));
    }
  }

  @Nested
  class Categories {

    @Test
    void returnsTheListingWithTheFrontShape() throws Exception {
      when(getCategoryListing.get("classic"))
          .thenReturn(new CategoryListing("classic", List.of(cl01())));

      MvcResult result =
          mvc.perform(get(BASE + "/categories/classic"))
              .andExpect(status().isOk())
              .andExpect(header().exists(TraceIdFilter.HEADER))
              .andExpect(jsonPath("$.category").value("classic"))
              .andExpect(jsonPath("$.availableFilters[0]").value("all"))
              .andExpect(jsonPath("$.availableFilters[1]").value("classic"))
              .andExpect(jsonPath("$.availableFilters[2]").value("autonomous"))
              .andExpect(jsonPath("$.availableFilters.length()").value(3))
              .andExpect(jsonPath("$.products[0].id").value("CL-01"))
              .andExpect(jsonPath("$.products[0].brand").value("Classic"))
              .andExpect(jsonPath("$.products[0].image").value(containsString("CL-01-GR.webp")))
              .andExpect(jsonPath("$.products[0].variants[0]").value("CL-01-GR"))
              .andExpect(jsonPath("$.products[0].variants[1]").value("CL-01-GY"))
              .andExpect(content().string(containsString("\"price\":5700.00")))
              .andReturn();

      assertThat(keysAt(result, "$.products[0]"))
          .containsOnlyKeys("id", "name", "brand", "price", "image", "variants", "description");
    }

    @Test
    void unknownFilterIs404CategoryNotFound() throws Exception {
      when(getCategoryListing.get("classics")).thenThrow(new CategoryNotFoundException("classics"));

      MvcResult result =
          mvc.perform(get(BASE + "/categories/classics"))
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
              .andExpect(jsonPath("$.message").value("Category classics not found"))
              .andExpect(jsonPath("$.details").doesNotExist())
              .andReturn();

      expectTraceIdMatchesHeader(result);
    }
  }

  @Nested
  class ProductDetail {

    @Test
    void returnsTheDetailWithTheFrontShape() throws Exception {
      when(getProductDetail.get("AU-07")).thenReturn(au07());

      MvcResult result =
          mvc.perform(get(BASE + "/products/AU-07"))
              .andExpect(status().isOk())
              .andExpect(header().exists(TraceIdFilter.HEADER))
              .andExpect(jsonPath("$.id").value("AU-07"))
              .andExpect(jsonPath("$.name").value("Verde Voyager"))
              .andExpect(jsonPath("$.brand").value("Autonomous"))
              .andExpect(jsonPath("$.image").value(containsString("AU-07-MT.webp")))
              .andExpect(jsonPath("$.variants.length()").value(2))
              .andExpect(jsonPath("$.variants[0]").value("AU-07-MT"))
              .andExpect(jsonPath("$.variants[1]").value("AU-07-YE"))
              .andExpect(jsonPath("$.highlights.length()").value(3))
              .andExpect(
                  jsonPath("$.highlights[0]")
                      .value(
                          "Adaptive drive system intelligently navigates through diverse field"
                              + " conditions"))
              .andExpect(content().string(containsString("\"price\":4000.00")))
              .andReturn();

      assertThat(keysAt(result, "$"))
          .containsOnlyKeys(
              "id", "name", "brand", "price", "image", "description", "variants", "highlights");
    }

    @Test
    void unknownIdIs404ProductNotFound() throws Exception {
      when(getProductDetail.get("CL-99")).thenThrow(new ProductNotFoundException("CL-99"));

      MvcResult result =
          mvc.perform(get(BASE + "/products/CL-99"))
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
              .andExpect(jsonPath("$.message").value("Product CL-99 not found"))
              .andExpect(jsonPath("$.details").doesNotExist())
              .andReturn();

      expectTraceIdMatchesHeader(result);
    }
  }

  @Nested
  class Stores {

    @Test
    void returnsTheStoresWithAddress() throws Exception {
      when(listStores.list()).thenReturn(List.of(storeA()));

      MvcResult result =
          mvc.perform(get(BASE + "/stores"))
              .andExpect(status().isOk())
              .andExpect(header().exists(TraceIdFilter.HEADER))
              .andExpect(jsonPath("$[0].id").value("store-a"))
              .andExpect(jsonPath("$[0].address").value("Astronaut Way 1"))
              .andReturn();

      assertThat(keysAt(result, "$[0]")).containsOnlyKeys("id", "name", "address", "city", "image");
    }
  }

  @Nested
  class Recommendations {

    private String skus(int count, int length) {
      String sku = "S".repeat(length);
      return String.join(",", IntStream.range(0, count).mapToObj(i -> sku).toList());
    }

    @Test
    void commaSeparatedSkusReachTheUseCaseAsAList() throws Exception {
      when(getRecommendations.get(List.of("AU-01-SI", "CL-01-GR"))).thenReturn(List.of());

      mvc.perform(get(BASE + "/recommendations").param("skus", "AU-01-SI,CL-01-GR"))
          .andExpect(status().isOk())
          .andExpect(header().exists(TraceIdFilter.HEADER));

      verify(getRecommendations).get(List.of("AU-01-SI", "CL-01-GR"));
    }

    @Test
    void withoutSkusIsOk() throws Exception {
      when(getRecommendations.get(null)).thenReturn(List.of());

      mvc.perform(get(BASE + "/recommendations")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",,"})
    void emptySkusAreOk(String skus) throws Exception {
      when(getRecommendations.get(any())).thenReturn(List.of());

      mvc.perform(get(BASE + "/recommendations").param("skus", skus)).andExpect(status().isOk());
    }

    @Test
    void fiftySkusOfTwentyCharactersAreAccepted() throws Exception {
      when(getRecommendations.get(any())).thenReturn(List.of());

      mvc.perform(get(BASE + "/recommendations").param("skus", skus(50, 20)))
          .andExpect(status().isOk());
    }

    @Test
    void recommendedSkuGoesFirstFollowedByTheOthersInDatasetOrder() throws Exception {
      var product = cl01();
      when(getRecommendations.get(List.of("AU-07-MT")))
          .thenReturn(List.of(new Recommendation(product, product.variants().get(1))));

      MvcResult result =
          mvc.perform(get(BASE + "/recommendations").param("skus", "AU-07-MT"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$[0].id").value("CL-01"))
              .andExpect(jsonPath("$[0].variants[0]").value("CL-01-GY"))
              .andExpect(jsonPath("$[0].variants[1]").value("CL-01-GR"))
              .andExpect(jsonPath("$[0].image").value(containsString("CL-01-GR.webp")))
              .andExpect(content().string(containsString("\"price\":5700.00")))
              .andReturn();

      assertThat(keysAt(result, "$[0]"))
          .containsOnlyKeys("id", "name", "brand", "price", "image", "variants", "description");
    }

    @Test
    void moreThanFiftySkusIsValidationError() throws Exception {
      MvcResult result =
          mvc.perform(get(BASE + "/recommendations").param("skus", skus(51, 8)))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
              .andExpect(jsonPath("$.details.skus").exists())
              .andReturn();

      expectTraceIdMatchesHeader(result);
      verify(getRecommendations, never()).get(any());
    }

    @Test
    void skuLongerThanTwentyCharactersIsValidationError() throws Exception {
      MvcResult result =
          mvc.perform(get(BASE + "/recommendations").param("skus", "AU-01-SI," + "S".repeat(21)))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
              .andExpect(jsonPath("$.details.skus").exists())
              .andReturn();

      expectTraceIdMatchesHeader(result);
      verify(getRecommendations, never()).get(any());
    }
  }

  @Nested
  class Validation {

    @Test
    void tooLongFilterIsValidationError() throws Exception {
      MvcResult result =
          mvc.perform(get(BASE + "/categories/" + "x".repeat(21)))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
              .andExpect(jsonPath("$.details.filter").exists())
              .andReturn();

      expectTraceIdMatchesHeader(result);
      verify(getCategoryListing, never()).get(anyString());
    }

    @Test
    void tooLongProductIdIsValidationError() throws Exception {
      MvcResult result =
          mvc.perform(get(BASE + "/products/" + "X".repeat(30)))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
              .andExpect(jsonPath("$.details.id").exists())
              .andReturn();

      expectTraceIdMatchesHeader(result);
      verify(getProductDetail, never()).get(anyString());
    }
  }

  static Map<String, Object> keysAt(MvcResult result, String path) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), path);
  }

  static void expectTraceIdMatchesHeader(MvcResult result) throws Exception {
    String traceId = result.getResponse().getHeader(TraceIdFilter.HEADER);
    assertThat(traceId).isNotBlank();
    jsonPath("$.traceId").value(traceId).match(result);
  }
}
