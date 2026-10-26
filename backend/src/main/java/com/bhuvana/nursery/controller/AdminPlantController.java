package com.bhuvana.nursery.controller;

import com.bhuvana.nursery.dto.PlantAdminRequest;
import com.bhuvana.nursery.dto.PlantAdminResponse;
import com.bhuvana.nursery.service.PlantAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/plants")
public class AdminPlantController {

    private final PlantAdminService plantAdminService;

    public AdminPlantController(PlantAdminService plantAdminService) {
        this.plantAdminService = plantAdminService;
    }

    @GetMapping
    public List<PlantAdminResponse> getAllPlants() {
        return plantAdminService.getAllPlants();
    }

    @GetMapping("/{id}")
    public PlantAdminResponse getPlant(@PathVariable UUID id) {
        return plantAdminService.getPlant(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlantAdminResponse create(
            @Valid @RequestBody PlantAdminRequest request
    ) {
        return plantAdminService.create(request);
    }

    @PutMapping("/{id}")
    public PlantAdminResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody PlantAdminRequest request
    ) {
        return plantAdminService.update(id, request);
    }

    @PatchMapping("/{id}/publish")
    public PlantAdminResponse publish(
            @PathVariable UUID id,
            @RequestParam boolean value
    ) {
        return plantAdminService.setPublished(id, value);
    }

    @PatchMapping("/{id}/restore")
    public PlantAdminResponse restore(@PathVariable UUID id) {
        return plantAdminService.restore(id);
    }

    @DeleteMapping("/{id}")
    public PlantAdminResponse archive(@PathVariable UUID id) {
        return plantAdminService.archive(id);
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanent(@PathVariable UUID id) {
        plantAdminService.deletePermanent(id);
    }
}
