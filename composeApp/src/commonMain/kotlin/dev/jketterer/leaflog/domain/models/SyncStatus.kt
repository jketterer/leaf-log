package dev.jketterer.leaflog.domain.models

enum class SyncStatus {
    SYNCED,
    PENDING,
    SYNCING,
    CONFLICT,
    ERROR,
    LOCAL_ONLY,
}