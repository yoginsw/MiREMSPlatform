package io.mirems.extension.kr;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.domain.extension.ElectionExtensionPack;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KrElectionExtensionPackTest {
    @Test
    void exposesKoreanElectionExtensionMetadataAndResources() {
        ElectionExtensionPack pack = new KrElectionExtensionPack();

        assertThat(pack.id()).isEqualTo("kr");
        assertThat(pack.countryCode()).isEqualTo("KR");
        assertThat(pack.displayName()).isEqualTo("대한민국 선거 확장팩");
        assertThat(pack.flywayMigrationLocations()).containsExactly("classpath:db/migration/ext/kr");
        assertThat(pack.processResourceLocations()).containsExactly("classpath:processes/ext/kr");
        assertThat(pack.metadata()).containsEntry("legalBasisDocument", "docs/extensions/kr/LEGAL.md");
    }

    @Test
    void exposesImmutableMetadataCollections() {
        ElectionExtensionPack pack = new KrElectionExtensionPack();

        assertThat(pack.flywayMigrationLocations()).isUnmodifiable();
        assertThat(pack.processResourceLocations()).isUnmodifiable();
        assertThat(pack.metadata()).isUnmodifiable();
        assertThat(pack.metadata()).containsAllEntriesOf(Map.of("extensionPhase", "P6"));
    }
}
