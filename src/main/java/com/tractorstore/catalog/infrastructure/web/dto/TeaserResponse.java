package com.tractorstore.catalog.infrastructure.web.dto;

/** Home teaser of a category ({@code CategoryTeaser} in the front). */
public record TeaserResponse(String id, String title, String image, String filter) {}
