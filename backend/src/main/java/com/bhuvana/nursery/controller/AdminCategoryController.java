package com.bhuvana.nursery.controller;

import com.bhuvana.nursery.dto.CategoryAdminRequest;
import com.bhuvana.nursery.dto.CategoryAdminResponse;
import com.bhuvana.nursery.service.CategoryAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final CategoryAdminService service;

    public AdminCategoryController(CategoryAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryAdminResponse> getAll() {
        return service.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryAdminResponse create(
            @Valid @RequestBody CategoryAdminRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CategoryAdminResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryAdminRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/publish")
    public CategoryAdminResponse publish(
            @PathVariable UUID id,
            @RequestParam boolean value
    ) {
        return service.setPublished(id, value);
    }
}
