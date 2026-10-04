package com.mandiflow.inventory.api;

import com.mandiflow.inventory.service.CatalogService;
import com.mandiflow.inventory.service.ProductDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final CatalogService catalogService;

    @GetMapping
    public List<ProductView> list() {
        return catalogService.list();
    }

    /** Name, unit and price only, served from the Redis cache after the first call. */
    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return catalogService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(@Valid @RequestBody CreateProductRequest request) {
        return catalogService.create(request);
    }

    @PutMapping("/{id}")
    public ProductView update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return catalogService.update(id, request);
    }
}
