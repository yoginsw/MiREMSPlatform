package io.mirems.core.infra.persistence.ballot;

import io.mirems.core.domain.ballot.BallotStyle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataBallotStyleRepository extends JpaRepository<BallotStyle, UUID> {
    List<BallotStyle> findByBallotId(UUID ballotId);

    Optional<BallotStyle> findByStyleCode(String styleCode);

    List<BallotStyle> findByDistrictAndLanguage(String district, String language);
}
