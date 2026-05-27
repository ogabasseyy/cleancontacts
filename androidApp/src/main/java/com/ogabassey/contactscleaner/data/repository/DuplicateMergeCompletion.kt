package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.model.CleanupStatus

internal object DuplicateMergeCompletion {
    fun status(
        totalGroups: Int,
        mergedGroups: Int,
        skippedSyncedGroups: Int,
        failedMergeableGroups: Int
    ): CleanupStatus {
        return when {
            failedMergeableGroups > 0 && mergedGroups > 0 ->
                CleanupStatus.Partial("Merged $mergedGroups of $totalGroups groups")
            failedMergeableGroups > 0 ->
                CleanupStatus.Error("Failed to merge duplicate groups")
            mergedGroups > 0 && skippedSyncedGroups > 0 ->
                CleanupStatus.Success("Merged $mergedGroups groups; $skippedSyncedGroups synced groups need review")
            mergedGroups > 0 ->
                CleanupStatus.Success("Merged $mergedGroups groups successfully")
            skippedSyncedGroups > 0 ->
                CleanupStatus.Error("No mergeable duplicate groups found")
            else ->
                CleanupStatus.Error("Failed to merge duplicate groups")
        }
    }
}
