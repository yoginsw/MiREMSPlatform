package io.mirems.core.domain.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExtensionPackRegistryTest {
    @Test
    void registersExtensionPacksByStableIdentifier() {
        ElectionExtensionPack krPack = new StubExtensionPack("kr", "KR", "대한민국 선거 확장팩");
        ElectionExtensionPack usPack = new StubExtensionPack("us", "US", "United States Election Extension Pack");

        ExtensionPackRegistry registry = new ExtensionPackRegistry(List.of(krPack, usPack));

        assertTrue(registry.findById("kr").isPresent());
        assertSame(krPack, registry.findById("kr").orElseThrow());
        assertSame(usPack, registry.requireById("us"));
        assertEquals(List.of("kr", "us"), registry.enabledPackIds());
        assertFalse(registry.findById("jp").isPresent());
    }

    @Test
    void rejectsDuplicateExtensionPackIdentifiers() {
        ElectionExtensionPack first = new StubExtensionPack("kr", "KR", "KR pack");
        ElectionExtensionPack duplicate = new StubExtensionPack("kr", "KR", "duplicate KR pack");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ExtensionPackRegistry(List.of(first, duplicate)));
        assertTrue(exception.getMessage().contains("Duplicate extension pack id: kr"));
    }

    @Test
    void rejectsBlankExtensionPackIdentifier() {
        ElectionExtensionPack blank = new StubExtensionPack(" ", "KR", "blank id pack");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ExtensionPackRegistry(List.of(blank)));
        assertTrue(exception.getMessage().contains("Extension pack id must not be blank"));
    }

    private record StubExtensionPack(String id, String countryCode, String displayName) implements ElectionExtensionPack {
        @Override
        public List<String> flywayMigrationLocations() {
            return List.of("classpath:db/migration/ext/" + id);
        }
    }
}
