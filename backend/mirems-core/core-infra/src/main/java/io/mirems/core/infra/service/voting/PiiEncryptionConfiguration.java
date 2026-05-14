package io.mirems.core.infra.service.voting;

import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import io.mirems.core.infra.persistence.voting.SpringDataVoterRecordRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides the PII encryption service used by voter-roll application services. */
@Configuration
@ConditionalOnBean(SpringDataVoterRecordRepository.class)
class PiiEncryptionConfiguration {
    private static final String RAW_KEY_ENV = "MIREMS_ENCRYPTION_KEY";
    private static final String BASE64_KEY_ENV = "MIREMS_PII_ENCRYPTION_KEY_BASE64";

    @Bean
    @ConditionalOnMissingBean(PiiEncryptionService.class)
    PiiEncryptionService piiEncryptionService() {
        String base64Key = System.getenv(BASE64_KEY_ENV);
        if (base64Key != null && !base64Key.isBlank()) {
            return new PiiEncryptionService(Base64.getDecoder().decode(base64Key));
        }
        String rawKey = System.getenv(RAW_KEY_ENV);
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException(RAW_KEY_ENV + " or " + BASE64_KEY_ENV + " is required for voter PII encryption");
        }
        return new PiiEncryptionService(rawKey.getBytes(StandardCharsets.UTF_8));
    }
}
