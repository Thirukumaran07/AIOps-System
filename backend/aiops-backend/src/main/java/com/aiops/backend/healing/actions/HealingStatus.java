package com.aiops.backend.healing.actions;

/**
 * Represents the outcome status of a self-healing action.
 */
public enum HealingStatus {

    SUCCESS,

    /** Healing was executed and verified; metric returned below threshold. */
    RECOVERED,

    /** Healing was attempted but metric remains above threshold. */
    UNRESOLVED,

    /** Process/resource is critical; administrator must act manually. */
    ADMIN_NOTIFICATION_REQUIRED,

    /** Files/storage identified; administrator must approve before deletion. */
    ADMIN_REVIEW_REQUIRED,

    /** An exception occurred during the healing attempt. */
    FAILED,

    /** dry-run=true; action would be executed but was NOT actually performed. */
    DRY_RUN
}
