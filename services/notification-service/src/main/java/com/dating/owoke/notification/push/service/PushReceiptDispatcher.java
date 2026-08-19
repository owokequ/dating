package com.dating.owoke.notification.push.service;

import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.dating.owoke.notification.delivery.domain.DeliveryAttempt;
import com.dating.owoke.notification.delivery.service.DeliveryService;

@Component
@ConditionalOnProperty(prefix = "owoke.expo-push", name = "enabled", havingValue = "true")
public class PushReceiptDispatcher {
    private final DeliveryService deliveryService; private final ExpoPushClient client;
    private final MobileDeviceService devices; private final PushMetrics metrics;
    public PushReceiptDispatcher(DeliveryService deliveryService, ExpoPushClient client, MobileDeviceService devices, PushMetrics metrics) {
        this.deliveryService = deliveryService; this.client = client; this.devices = devices; this.metrics = metrics;
    }
    @Scheduled(fixedDelayString = "${owoke.expo-push.receipt-fixed-delay:60000}")
    public void checkReceipts() {
        List<DeliveryAttempt> attempts = deliveryService.claimPushReceipts();
        if (attempts.isEmpty()) return;
        Map<String, ExpoPushClient.ExpoReceipt> receipts = client.receipts(attempts.stream().map(DeliveryAttempt::getProviderMessageId).toList());
        for (DeliveryAttempt attempt : attempts) {
            ExpoPushClient.ExpoReceipt receipt = receipts.get(attempt.getProviderMessageId());
            if (receipt == null) { retry(attempt, "Expo receipt is missing"); continue; }
            if ("ok".equals(receipt.status())) { deliveryService.markPushDelivered(attempt.getId()); metrics.delivered(); continue; }
            if ("DeviceNotRegistered".equals(receipt.error())) {
                devices.deactivate(attempt.getDestination()); deliveryService.markPushDeviceDisabled(attempt.getId(), receipt.error()); metrics.deviceDisabled(); continue;
            }
            retry(attempt, receipt.error() == null ? "Expo receipt failed" : receipt.error());
        }
    }
    private void retry(DeliveryAttempt attempt, String error) { deliveryService.markPushReceiptFailed(attempt.getId(), new IllegalStateException(error)); metrics.retry(); }
}
