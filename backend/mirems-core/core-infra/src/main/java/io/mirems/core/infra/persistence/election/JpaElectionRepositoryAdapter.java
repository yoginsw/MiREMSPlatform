package io.mirems.core.infra.persistence.election;

import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(SpringDataElectionRepository.class)
public class JpaElectionRepositoryAdapter implements ElectionRepository {
    private final SpringDataElectionRepository repository;

    public JpaElectionRepositoryAdapter(SpringDataElectionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    @Override
    public Election save(Election election) {
        return repository.save(election);
    }

    @Override
    public Optional<Election> findById(UUID id) {
        return repository.findById(id);
    }
}
