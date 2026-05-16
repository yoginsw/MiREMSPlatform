package io.mirems.extension.template;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(name = "mirems.extension.template.enabled", havingValue = "true")
public class TemplateExtensionAutoConfiguration {
    @Bean
    TemplateElectionExtensionPack templateElectionExtensionPack() {
        return new TemplateElectionExtensionPack();
    }
}
