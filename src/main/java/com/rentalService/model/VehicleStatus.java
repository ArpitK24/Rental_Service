package com.rentalService.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Status values for vehicles in the rental system.
 *
 * <p>Provides utility methods to check allowed state transitions and to determine
 * whether a status is terminal.</p>
 */
public enum VehicleStatus {
    UNDER_REVIEW,
    ACTIVE,
    INACTIVE,
    REJECTED;

    /**
     * Returns whether this status is considered a terminal state in the current business rules.
     * For example, REJECTED may be considered terminal unless manually changed by an admin.
     *
     * @return true if terminal
     */
    public boolean isTerminal() {
        return this == REJECTED;
    }

    /**
     * Returns the set of allowed target statuses this status can transition to under
     * normal admin workflow rules. Keep business rules centralized here to avoid
     * scattering transition logic across services/controllers.
     *
     * Current rules implemented:
     * - PENDING -> APPROVED, REJECTED
     * - APPROVED -> SUSPENDED
     * - SUSPENDED -> APPROVED, REJECTED
     * - REJECTED -> (no direct transitions; admin may perform manual override if necessary)
     *
     * @return set of allowed target statuses
     */
    public Set<VehicleStatus> allowedTransitions() {
        switch (this) {
            case UNDER_REVIEW:
                return EnumSet.of(ACTIVE, INACTIVE, REJECTED);
            case ACTIVE:
                return EnumSet.of(INACTIVE, REJECTED);
            case INACTIVE:
                return EnumSet.of(ACTIVE, REJECTED);
            case REJECTED:
            default:
                return EnumSet.noneOf(VehicleStatus.class);
        }
    }


    /**
     * Convenience check to test whether transitioning from this status to {@code target}
     * is permitted according to {@link #allowedTransitions()}.
     *
     * @param target the target status
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(VehicleStatus target) {
        if (target == null) return false;
        return allowedTransitions().contains(target);
    }
}
