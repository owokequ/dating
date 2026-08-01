# Owoke event contracts

Kafka messages use the envelope defined in `event-envelope-v1.schema.json`.
Payload schemas are versioned independently and Java classes are intentionally
not shared between services. Producers and consumers own their local DTOs.

`dating.commands.v1` carries decisions initiated from external channels such as
Telegram. `dating.events.v1` carries the resulting domain status event and a
`DateProposalDecisionResultV1` acknowledgement for the actor.

`media.events.v1` carries media readiness, collection ordering and soft-delete
metadata. Binary image content is intentionally never placed into Kafka.

Compatibility rules:

- existing required fields are never removed or renamed;
- optional fields may be added;
- an incompatible payload is published under a new event type/version;
- UUID values are strings and timestamps are UTC ISO-8601 values.
