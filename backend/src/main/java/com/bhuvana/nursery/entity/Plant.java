package com.bhuvana.nursery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plants")
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private PlantCategory category;

    @Column(name = "name_kn", nullable = false, length = 200)
    private String nameKn;

    @Column(name = "name_en", nullable = false, length = 200)
    private String nameEn;

    @Column(name = "scientific_name", length = 250)
    private String scientificName;

    @Column(name = "slug", nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "description_kn", columnDefinition = "text")
    private String descriptionKn;

    @Column(name = "description_en", columnDefinition = "text")
    private String descriptionEn;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "enquiry_only", nullable = false)
    private boolean enquiryOnly = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability", nullable = false, length = 20)
    private AvailabilityStatus availability = AvailabilityStatus.UNKNOWN;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "featured", nullable = false)
    private boolean featured;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Plant() {
    }

    public UUID getId() {
        return id;
    }

    public PlantCategory getCategory() {
        return category;
    }

    public void setCategory(PlantCategory category) {
        this.category = category;
    }

    public String getNameKn() {
        return nameKn;
    }

    public void setNameKn(String nameKn) {
        this.nameKn = nameKn;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescriptionKn() {
        return descriptionKn;
    }

    public void setDescriptionKn(String descriptionKn) {
        this.descriptionKn = descriptionKn;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public void setDescriptionEn(String descriptionEn) {
        this.descriptionEn = descriptionEn;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isEnquiryOnly() {
        return enquiryOnly;
    }

    public void setEnquiryOnly(boolean enquiryOnly) {
        this.enquiryOnly = enquiryOnly;
    }

    public AvailabilityStatus getAvailability() {
        return availability;
    }

    public void setAvailability(AvailabilityStatus availability) {
        this.availability = availability;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
