package com.dating.owoke.events.sync.dto;

import java.util.List;

public record KudaGoPage(List<ExternalEventData> events, boolean hasNext, int received, int skipped) {
}
