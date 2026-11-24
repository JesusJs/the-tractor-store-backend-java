package com.tractorstore.catalog.infrastructure.web.dto;

/** Physical store ({@code StoreLocation} in the front). */
public record StoreResponse(String id, String name, String address, String city, String image) {}
