package io.mirems.core.infra.persistence.election;

import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataElectionRepository extends JpaRepository<Election, UUID> {
    List<Election> findByElectionStatus(ElectionStatus electionStatus);

    List<Election> findByCountryCode(String countryCode);

    List<Election> findByScheduledDateBetween(LocalDate fromInclusive, LocalDate toInclusive);
}
