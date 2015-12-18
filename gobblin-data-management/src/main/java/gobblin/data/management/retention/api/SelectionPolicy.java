package gobblin.data.management.retention.api;

import javax.annotation.Nonnull;

/**
 * Selects a collection of items for post processing. Examples include retention management,
 * copying, etc.
 * @param T the type of items to be cleaned
 */
public interface SelectionPolicy<T> {

    /** Selects the items for processing. */
    Iterable<T> select(@Nonnull Iterable<T> items);

    /** A human-readable name of the policy */
    String getName();
}
