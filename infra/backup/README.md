# PostgreSQL backup and restore runbook

Owoke keeps one PostgreSQL database per business service. Back up all six
databases independently; a backup set is complete only when all six dumps from
the same maintenance window are present.

## Backup

Run the following commands on the VPS from the repository directory. Store the
resulting directory on encrypted storage outside that VPS.

```bash
backup_dir="/opt/owoke/backups/$(date -u +%Y%m%dT%H%M%SZ)"
install -d -m 700 "$backup_dir"

docker compose --env-file /opt/owoke/secrets/production.env -f infra/compose.prod.yaml \
  exec -T identity-postgres pg_dump -U owoke_identity -Fc owoke_identity >"$backup_dir/identity.dump"
docker compose --env-file /opt/owoke/secrets/production.env -f infra/compose.prod.yaml \
  exec -T dating-postgres pg_dump -U owoke_dating -Fc owoke_dating >"$backup_dir/dating.dump"
docker compose --env-file /opt/owoke/secrets/production.env -f infra/compose.prod.yaml \
  exec -T notification-postgres pg_dump -U owoke_notification -Fc owoke_notification >"$backup_dir/notification.dump"
docker compose --env-file /opt/owoke/secrets/production.env -f infra/compose.prod.yaml \
  exec -T places-postgres pg_dump -U owoke_places -Fc owoke_places >"$backup_dir/places.dump"
docker compose --env-file /opt/owoke/secrets/production.env -f infra/compose.prod.yaml \
  exec -T media-postgres pg_dump -U owoke_media -Fc owoke_media >"$backup_dir/media.dump"
docker compose --env-file /opt/owoke/secrets/production.env -f infra/compose.prod.yaml \
  exec -T events-postgres pg_dump -U owoke_events -Fc owoke_events >"$backup_dir/events.dump"

sha256sum "$backup_dir"/*.dump >"$backup_dir/SHA256SUMS"
```

Use a scheduler outside the application stack, apply retention rules, alert on
non-zero exit codes and periodically copy the dumps off-host. Redis is not the
system of record; PostgreSQL dumps are the required durable backups.

## Restore drill

Never test a restore against production. Provision an isolated PostgreSQL
instance with no application traffic, verify `SHA256SUMS`, create empty target
databases and restore each dump with `pg_restore --clean --if-exists`. Then start
the matching service versions against the restored databases and verify:

1. Liquibase validation and application readiness are healthy.
2. Identity users, couples, places, events, media metadata and notifications have plausible row counts.
3. A read-only end-to-end walkthrough can load an existing couple and date.
4. The restored environment has no production SMTP, Telegram or Kafka access.

Record the date, backup identifiers, restore duration and result. A backup is not
considered operational until a restore drill has succeeded.
