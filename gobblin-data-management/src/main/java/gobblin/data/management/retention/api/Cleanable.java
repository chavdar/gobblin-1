package gobblin.data.management.retention.api;

import gobblin.data.management.dataset.Dataset;
import gobblin.data.management.partition.Partition;
import gobblin.data.management.retention.version.DatasetVersion;

/**
 * Describes an object that can be cleaned using a retention policy represented through a
 * {@link SelectionPolicy}. The policy is applied to items owned by this (container) object.
 * Typically, the owner is a {@link Dataset} and the items subject to the policy are
 * {@link Partition}s or {@link DatasetVersion}s. Each item is identified by a key to be used for
 * the partition.
 *
 * @param K     key type of items subject to the cleaning policy
 */
public interface Cleanable<K> {
    /**
     * Get the candidate items to be
     * @return
     */
    Iterable<K> getCandidateKeys();
    /**
     * Drops the key with the selected key.
     */
    void dropItem(K itemKey);
}
