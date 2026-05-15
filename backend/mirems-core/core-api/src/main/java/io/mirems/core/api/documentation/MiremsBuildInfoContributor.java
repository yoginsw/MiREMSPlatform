package io.mirems.core.api.documentation;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

@Component
public class MiremsBuildInfoContributor implements InfoContributor {
    private final ObjectProvider<BuildProperties> buildProperties;
    private final ObjectProvider<GitProperties> gitProperties;

    public MiremsBuildInfoContributor(
            ObjectProvider<BuildProperties> buildProperties,
            ObjectProvider<GitProperties> gitProperties) {
        this.buildProperties = buildProperties;
        this.gitProperties = gitProperties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        BuildProperties build = buildProperties.getIfAvailable();
        GitProperties git = gitProperties.getIfAvailable();

        builder.withDetail("build", Map.of(
                "name", build == null ? "MiREMS Platform Core API" : build.getName(),
                "version", build == null ? "0.1.0-SNAPSHOT" : build.getVersion(),
                "time", build == null ? Instant.EPOCH.toString() : build.getTime().toString()));
        builder.withDetail("git", Map.of(
                "commit", git == null ? "unknown" : git.getShortCommitId()));
    }
}
