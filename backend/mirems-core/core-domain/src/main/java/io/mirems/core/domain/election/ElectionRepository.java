package io.mirems.core.domain.election;

import java.util.Optional;
import java.util.UUID;

/** Persistence port for election aggregate lifecycle changes. */
public interface ElectionRepository {
    Election save(Election election);

    Optional<Election> findById(UUID id);
}
