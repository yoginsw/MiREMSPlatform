package io.mirems.extension.kr;

import io.mirems.core.domain.extension.ElectionExtensionPack;
import io.mirems.core.domain.extension.ExtensionPackRegistry;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(name = "mirems.extension.kr.enabled", havingValue = "true")
public class KrExtensionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(KrElectionExtensionPack.class)
    KrElectionExtensionPack krElectionExtensionPack() {
        return new KrElectionExtensionPack();
    }

    @Bean
    @ConditionalOnMissingBean(ExtensionPackRegistry.class)
    ExtensionPackRegistry extensionPackRegistry(List<ElectionExtensionPack> extensionPacks) {
        return new ExtensionPackRegistry(extensionPacks);
    }
}
