package io.mirems.extension.us;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.domain.extension.ElectionExtensionPack;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UsElectionExtensionPackTest {
    @Test
    void exposesUnitedStatesElectionExtensionMetadataAndResources() {
        ElectionExtensionPack pack = new UsElectionExtensionPack();

        assertThat(pack.id()).isEqualTo("us");
        assertThat(pack.countryCode()).isEqualTo("US");
        assertThat(pack.displayName()).isEqualTo("United States election extension pack");
        assertThat(pack.flywayMigrationLocations()).containsExactly("classpath:db/migration/ext/us");
        assertThat(pack.processResourceLocations()).containsExactly("classpath:processes/ext/us");
        assertThat(pack.metadata()).containsEntry("legalBasisDocument", "docs/extensions/us/LEGAL.md");
    }

    @Test
    void exposesImmutableMetadataCollections() {
        ElectionExtensionPack pack = new UsElectionExtensionPack();

        assertThat(pack.flywayMigrationLocations()).isUnmodifiable();
        assertThat(pack.processResourceLocations()).isUnmodifiable();
        assertThat(pack.metadata()).isUnmodifiable();
        assertThat(pack.metadata()).containsAllEntriesOf(Map.of("extensionPhase", "P7"));
    }
}
