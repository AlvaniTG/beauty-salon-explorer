package me.jorlowski.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import me.jorlowski.dto.SalonDetails;
import me.jorlowski.dto.SalonEditRequest;
import me.jorlowski.dto.SalonFilter;
import me.jorlowski.dto.SalonItemList;
import me.jorlowski.service.SalonService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/salons")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class SalonController {

    private final SalonService salonService;

    @GetMapping
    public Page<SalonItemList> getSalons(
            SalonFilter filter,
            @PageableDefault(sort = "rating", direction = Sort.Direction.DESC) Pageable pageable) {

        return salonService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public SalonDetails getSalonById(@PathVariable UUID id) {
        return salonService.findById(id);
    }

    @PatchMapping("/{id}")
    public SalonDetails updateSalon(
            @PathVariable UUID id,
            @RequestBody SalonEditRequest request) {

        return salonService.updateSalon(id, request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }
}
