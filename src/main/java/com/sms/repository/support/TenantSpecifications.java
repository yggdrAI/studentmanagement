package com.sms.repository.support;

import org.springframework.data.jpa.domain.Specification;

public final class TenantSpecifications {

    private TenantSpecifications() {
    }

    public static <T> Specification<T> hasTenantId(Long tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }
}
