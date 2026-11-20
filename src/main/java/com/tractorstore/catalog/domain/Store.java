package com.tractorstore.catalog.domain;

/** Physical store where orders can be picked up. */
public record Store(String id, String name, String address, String city, String image) {}
