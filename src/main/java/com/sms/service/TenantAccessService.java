package com.sms.service;

import org.springframework.stereotype.Service;

import com.sms.config.TenantContext;

@Service
public class TenantAccessService {

    public Long requireCurrentTenantId() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not available for this request");
        }
        return tenantId;
    }

    public Long currentTenantOrDefault() {
        Long tenantId = TenantContext.get();
        return tenantId == null ? 1L : tenantId;
    }
}
