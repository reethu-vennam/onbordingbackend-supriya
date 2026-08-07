package com.sabbpe.event;

import org.springframework.context.ApplicationEvent;

public class MerchantApprovedEvent extends ApplicationEvent {

    private final String merchantId;

    public MerchantApprovedEvent(Object source, String merchantId) {
        super(source);
        this.merchantId = merchantId;
    }

    public String getMerchantId() {
        return merchantId;
    }
}
