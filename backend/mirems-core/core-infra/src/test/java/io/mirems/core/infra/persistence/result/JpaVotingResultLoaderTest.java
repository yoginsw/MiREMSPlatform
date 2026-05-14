package io.mirems.core.infra.persistence.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mirems.core.domain.result.VotingResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaVotingResultLoaderTest {
    private static final UUID ELECTION_ID = UUID.fromString("018f4ba0-0000-7000-8000-000000000001");

    @Test
    void loadsCommittedResultsByElectionThroughSpringDataRepository() {
        SpringDataVotingResultJpaRepository repository = mock(SpringDataVotingResultJpaRepository.class);
        VotingResult result = mock(VotingResult.class);
        when(repository.findBySessionElectionId(ELECTION_ID)).thenReturn(List.of(result));
        JpaVotingResultLoader loader = new JpaVotingResultLoader(repository);

        List<VotingResult> loaded = loader.loadCommittedResults(ELECTION_ID);

        assertEquals(List.of(result), loaded);
        verify(repository).findBySessionElectionId(ELECTION_ID);
    }
}
