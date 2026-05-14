package io.mirems.core.infra.persistence.election;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mirems.core.domain.election.Election;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaElectionRepositoryAdapterTest {
    @Test
    void delegatesSaveAndFindByIdToSpringDataRepository() {
        SpringDataElectionRepository repository = mock(SpringDataElectionRepository.class);
        JpaElectionRepositoryAdapter adapter = new JpaElectionRepositoryAdapter(repository);
        Election election = mock(Election.class);
        UUID electionId = UUID.fromString("018f4bd0-aaaa-7bbb-8ccc-233344445555");
        when(repository.save(election)).thenReturn(election);
        when(repository.findById(electionId)).thenReturn(Optional.of(election));

        assertSame(election, adapter.save(election));
        assertTrue(adapter.findById(electionId).isPresent());
        assertSame(election, adapter.findById(electionId).orElseThrow());

        verify(repository).save(election);
        verify(repository, times(2)).findById(electionId);
    }
}
