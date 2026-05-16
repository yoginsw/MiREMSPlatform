package io.mirems.extension.us;

import io.mirems.core.domain.extension.ElectionExtensionPack;
import io.mirems.core.domain.extension.ExtensionPackRegistry;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(name = "mirems.extension.us.enabled", havingValue = "true")
public class UsExtensionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(UsElectionExtensionPack.class)
    UsElectionExtensionPack usElectionExtensionPack() {
        return new UsElectionExtensionPack();
    }

    @Bean
    @ConditionalOnMissingBean(ExtensionPackRegistry.class)
    ExtensionPackRegistry extensionPackRegistry(List<ElectionExtensionPack> extensionPacks) {
        return new ExtensionPackRegistry(extensionPacks);
    }
}
