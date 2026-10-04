package com.mandiflow.inventory.service;

import com.mandiflow.inventory.api.CreateProductRequest;
import com.mandiflow.inventory.api.ProductView;
import com.mandiflow.inventory.api.UpdateProductRequest;
import com.mandiflow.inventory.domain.Product;
import com.mandiflow.inventory.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final ProductRepository productRepository;

    /** Cached in Redis under products::{id} for 10 minutes. The second call skips SQL. */
    @Cacheable(cacheNames = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        log.debug("Cache miss for product {}, reading MySQL", id);
        return ProductDto.from(productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id)));
    }

    /** The full catalogue with live stock. Not cached, because stock changes with every order. */
    @Transactional(readOnly = true)
    public List<ProductView> list() {
        return productRepository.findAllByOrderByIdAsc().stream().map(ProductView::from).toList();
    }

    @Transactional
    public ProductView create(CreateProductRequest req) {
        Product p = productRepository.save(Product.of(req.name(), req.unit(), req.pricePerUnit(), req.availableQty()));
        return ProductView.from(p);
    }

    /** Admin changes the day's price and/or stock. The cached entry is evicted. */
    @CacheEvict(cacheNames = "products", key = "#id")
    @Transactional
    public ProductView update(Long id, UpdateProductRequest req) {
        Product p = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (req.pricePerUnit() != null) p.setPricePerUnit(req.pricePerUnit());
        if (req.availableQty() != null) p.setAvailableQty(req.availableQty());
        log.info("Product {} updated: ₹{} per {}, {} available", id, p.getPricePerUnit(), p.getUnit(), p.getAvailableQty());
        return ProductView.from(p);
    }
}
