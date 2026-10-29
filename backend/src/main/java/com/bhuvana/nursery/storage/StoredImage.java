package com.bhuvana.nursery.storage;

/**
 * Result of storing an image. {@code url} is what the API returns and what the
 * admin UI writes into {@code plants.image_url} / {@code hero_slides.image_url}.
 */
public record StoredImage(String url, String filename) {
}
