package io.mirems.extension.us;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.domain.extension.ElectionExtensionPack;
import io.mirems.core.domain.extension.ExtensionPackRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class UsExtensionAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UsExtensionAutoConfiguration.class));

    @Test
    void doesNotLoadUnitedStatesExtensionPackWhenPropertyIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ElectionExtensionPack.class);
            assertThat(context).doesNotHaveBean(ExtensionPackRegistry.class);
        });
    }

    @Test
    void doesNotLoadUnitedStatesExtensionPackWhenPropertyIsDisabled() {
        contextRunner
                .withPropertyValues("mirems.extension.us.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ElectionExtensionPack.class));
    }

    @Test
    void loadsUnitedStatesExtensionPackAndRegistryWhenPropertyIsEnabled() {
        contextRunner
                .withPropertyValues("mirems.extension.us.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(UsElectionExtensionPack.class);
                    assertThat(context).hasSingleBean(ElectionExtensionPack.class);
                    assertThat(context).hasSingleBean(ExtensionPackRegistry.class);
                    assertThat(context.getBean(ExtensionPackRegistry.class).findById("us"))
                            .containsInstanceOf(UsElectionExtensionPack.class);
                });
    }
}
