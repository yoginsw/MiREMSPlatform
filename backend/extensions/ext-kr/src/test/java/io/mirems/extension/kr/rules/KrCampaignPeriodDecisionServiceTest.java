package io.mirems.extension.kr.rules;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.extension.kr.KrElectionType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;

class KrCampaignPeriodDecisionServiceTest {
    private static final Path DMN = Path.of("src/main/resources/decisions/kr/KrCampaignPeriod.dmn");

    private final KrCampaignPeriodDecisionService service = new KrCampaignPeriodDecisionService();

    @ParameterizedTest(name = "{0}")
    @MethodSource("decisionRows")
    void evaluatesEveryKrCampaignPeriodDecisionRow(
            String scenario,
            KrCampaignPeriodRequest request,
            boolean expectedAllowed,
            String expectedReason) {
        KrCampaignPeriodResult result = service.evaluate(request);

        assertThat(result.allowed()).as(scenario).isEqualTo(expectedAllowed);
        assertThat(result.reason()).isEqualTo(expectedReason);
    }

    @Test
    void campaignPeriodDmnDeclaresInputsOutputsAndDecisionRows() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(DMN.toFile());
        String xml = Files.readString(DMN);

        assertThat(xml).contains("name=\"electionType\"");
        assertThat(xml).contains("name=\"daysUntilElection\"");
        assertThat(xml).contains("name=\"allowed\"");
        assertThat(xml).contains("name=\"reason\"");
        assertThat(document.getElementsByTagNameNS("*", "rule").getLength()).isEqualTo(7);
    }

    private static Stream<Arguments> decisionRows() {
        return Stream.of(
                Arguments.of(
                        "campaigning on or after election day is prohibited",
                        request(KrElectionType.PRESIDENTIAL_ELECTION, 0),
                        false,
                        "campaign activity is prohibited on or after election day"),
                Arguments.of(
                        "campaigning after election day is prohibited",
                        request(KrElectionType.LOCAL_ELECTION, -1),
                        false,
                        "campaign activity is prohibited on or after election day"),
                Arguments.of(
                        "presidential campaign is allowed from D-23 through D-1",
                        request(KrElectionType.PRESIDENTIAL_ELECTION, 23),
                        true,
                        "presidential campaign period is active"),
                Arguments.of(
                        "presidential campaign before D-23 is prohibited",
                        request(KrElectionType.PRESIDENTIAL_ELECTION, 24),
                        false,
                        "presidential campaign period has not started"),
                Arguments.of(
                        "national assembly campaign is allowed from D-14 through D-1",
                        request(KrElectionType.NATIONAL_ASSEMBLY_ELECTION, 14),
                        true,
                        "standard public election campaign period is active"),
                Arguments.of(
                        "local campaign is allowed from D-14 through D-1",
                        request(KrElectionType.LOCAL_ELECTION, 14),
                        true,
                        "standard public election campaign period is active"),
                Arguments.of(
                        "superintendent campaign is allowed from D-14 through D-1",
                        request(KrElectionType.SUPERINTENDENT_ELECTION, 14),
                        true,
                        "standard public election campaign period is active"),
                Arguments.of(
                        "by-election campaign is allowed from D-14 through D-1",
                        request(KrElectionType.BY_ELECTION, 14),
                        true,
                        "standard public election campaign period is active"),
                Arguments.of(
                        "standard campaign before D-14 is prohibited",
                        request(KrElectionType.LOCAL_ELECTION, 15),
                        false,
                        "standard public election campaign period has not started"));
    }

    private static KrCampaignPeriodRequest request(KrElectionType electionType, int daysUntilElection) {
        return new KrCampaignPeriodRequest(electionType, daysUntilElection);
    }
}
