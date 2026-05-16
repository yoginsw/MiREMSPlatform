package io.mirems.extension.kr;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.domain.extension.ElectionExtensionPack;
import io.mirems.core.domain.extension.ExtensionPackRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KrExtensionAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KrExtensionAutoConfiguration.class));

    @Test
    void doesNotLoadKoreanExtensionPackWhenPropertyIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ElectionExtensionPack.class);
            assertThat(context).doesNotHaveBean(ExtensionPackRegistry.class);
        });
    }

    @Test
    void doesNotLoadKoreanExtensionPackWhenPropertyIsDisabled() {
        contextRunner
                .withPropertyValues("mirems.extension.kr.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ElectionExtensionPack.class));
    }

    @Test
    void loadsKoreanExtensionPackAndRegistryWhenPropertyIsEnabled() {
        contextRunner
                .withPropertyValues("mirems.extension.kr.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(KrElectionExtensionPack.class);
                    assertThat(context).hasSingleBean(ElectionExtensionPack.class);
                    assertThat(context).hasSingleBean(ExtensionPackRegistry.class);
                    assertThat(context.getBean(ExtensionPackRegistry.class).findById("kr"))
                            .containsInstanceOf(KrElectionExtensionPack.class);
                });
    }
}
