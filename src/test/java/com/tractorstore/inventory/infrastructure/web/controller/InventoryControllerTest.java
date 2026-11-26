package com.tractorstore.inventory.infrastructure.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tractorstore.inventory.application.port.in.GetStockUseCase;
import com.tractorstore.inventory.domain.exception.StockNotFoundException;
import com.tractorstore.inventory.domain.model.StockItem;
import com.tractorstore.shared.infrastructure.web.GlobalExceptionHandler;
import com.tractorstore.shared.infrastructure.web.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
class InventoryControllerTest {

  private static final String BASE = "/api/v1/inventory";

  @Autowired private MockMvc mvc;

  @MockitoBean private GetStockUseCase getStock;

  @Test
  void returnsSkuAndStockWithoutSessionCookie() throws Exception {
    when(getStock.get("CL-01-GR")).thenReturn(new StockItem("CL-01-GR", 10));

    mvc.perform(get(BASE + "/CL-01-GR"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.sku").value("CL-01-GR"))
        .andExpect(jsonPath("$.stock").value(10))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
  }

  @Test
  void outOfStockIsStillOk() throws Exception {
    when(getStock.get("CL-01-GY")).thenReturn(new StockItem("CL-01-GY", 0));

    mvc.perform(get(BASE + "/CL-01-GY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value(0));
  }

  @Test
  void unknownSkuIsVariantNotFound() throws Exception {
    when(getStock.get("XX-99-ZZ")).thenThrow(new StockNotFoundException("XX-99-ZZ"));

    mvc.perform(get(BASE + "/XX-99-ZZ"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("VARIANT_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Variant XX-99-ZZ not found"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
  }

  @Test
  void skuLongerThan20IsValidationErrorWithoutCallingTheUseCase() throws Exception {
    mvc.perform(get(BASE + "/ABCDEFGHIJKLMNOPQRSTU"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details.sku").exists())
        .andExpect(jsonPath("$.traceId").isNotEmpty());

    verify(getStock, never()).get(anyString());
  }

  @Test
  void skuWithMarkupIsValidationErrorThatDoesNotEchoIt() throws Exception {
    mvc.perform(get(BASE + "/%3Cb%3Ex"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details.sku").value("must only contain letters, digits and hyphens"))
        .andExpect(content().string(not(containsString("<b>"))));

    verify(getStock, never()).get(anyString());
  }
}
