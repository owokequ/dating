package com.dating.owoke.notification.push.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PushMetrics {
    private final MeterRegistry registry;
    public PushMetrics(MeterRegistry registry) { this.registry = registry; }
    public void sent() { registry.counter("owoke.push.sent").increment(); }
    public void delivered() { registry.counter("owoke.push.delivered").increment(); }
    public void retry() { registry.counter("owoke.push.retry").increment(); }
    public void permanentFailure() { registry.counter("owoke.push.permanent_failure").increment(); }
    public void deviceDisabled() { registry.counter("owoke.push.device_disabled").increment(); }
}
