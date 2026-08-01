package com.dating.owoke.events.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dating.owoke.events.sync.dto.ExternalEventData;
import com.dating.owoke.events.sync.dto.ExternalImageData;
import com.dating.owoke.events.sync.dto.ExternalOccurrenceData;

class CatalogEventTest {
    private static final Instant NOW = Instant.parse("2026-08-02T10:00:00Z");

    @Test
    void importsCompleteFutureEventAsActiveWithStableOccurrenceId() {
        CatalogEvent first = CatalogEvent.imported(data("42", true), NOW);
        CatalogEvent second = CatalogEvent.imported(data("42", true), NOW);

        assertThat(first.getStatus()).isEqualTo(EventStatus.ACTIVE);
        assertThat(first.getOccurrences()).singleElement()
                .extracting(EventOccurrence::getId)
                .isEqualTo(second.getOccurrences().getFirst().getId());
        assertThat(first.getSourcePageUrl()).startsWith("https://kudago.com/");
    }

    @Test
    void keepsManualVenueAndHiddenStatusDuringProviderRefresh() {
        CatalogEvent event = CatalogEvent.imported(data("42", false), NOW);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        event.updateVenue("Своя площадка", "Свой адрес", 55.79, 49.12, NOW.plusSeconds(1));
        event.publish(NOW.plusSeconds(2));
        event.hide(NOW.plusSeconds(3));

        event.refresh(data("42", true), NOW.plusSeconds(4));

        assertThat(event.getStatus()).isEqualTo(EventStatus.HIDDEN);
        assertThat(event.getVenueName()).isEqualTo("Своя площадка");
        assertThat(event.isVenueOverride()).isTrue();
    }

    @Test
    void refusesPublishingWithoutVenue() {
        CatalogEvent event = CatalogEvent.imported(data("42", false), NOW);
        assertThatThrownBy(() -> event.publish(NOW.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("venue");
    }

    private ExternalEventData data(String id, boolean venue) {
        return new ExternalEventData(id, "Концерт", "Описание", List.of("concert"), "1000 ₽", false,
                "12+", "http://kudago.com/kzn/event/test/", "7",
                venue ? "Зал" : null, venue ? "ул. Баумана, 1" : null,
                venue ? 55.79 : null, venue ? 49.12 : null,
                List.of(new ExternalOccurrenceData("one", NOW.plusSeconds(3600), NOW.plusSeconds(7200), false)),
                List.of(new ExternalImageData("image", "https://kudago.com/media/images/event/test.jpg",
                        null, "Автор", "https://example.com")));
    }
}
