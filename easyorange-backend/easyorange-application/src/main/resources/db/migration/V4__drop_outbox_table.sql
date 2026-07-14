-- Cleanup: Drop the Outbox table (no longer used)
-- Outbox pattern was replaced by RabbitMQ direct publish (V1 era),
-- then replaced by Spring Modulith Event Publication Registry (V3).
-- The eo_domain_event table has been unused since the RabbitMQ-only migration.
-- Spring Modulith's EVENT_PUBLICATION table (V3) is the current event registry.
--
-- WARNING: If rolling back to the old publisher, keep this table.
-- With Modulith (V3+), this table is dead code.

DROP TABLE IF EXISTS `eo_domain_event`;
