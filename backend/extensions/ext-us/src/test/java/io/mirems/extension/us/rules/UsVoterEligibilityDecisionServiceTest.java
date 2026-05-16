package io.mirems.extension.us.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;

class UsVoterEligibilityDecisionServiceTest {
    private static final Path DMN = Path.of("src/main/resources/decisions/us/UsVoterEligibility.dmn");
    private static final LocalDate ELECTION_DAY = LocalDate.of(2028, 11, 7);
    private static final LocalDate PRIMARY_DAY = LocalDate.of(2028, 3, 5);

    private final UsVoterEligibilityDecisionService service = new UsVoterEligibilityDecisionService();

    @ParameterizedTest(name = "{0}")
    @MethodSource("decisionRows")
    void evaluatesEveryUsVoterEligibilityDecisionRow(
            String scenario,
            UsVoterEligibilityRequest request,
            boolean expectedEligible,
            boolean expectedProvisionalRequired,
            String expectedReason) {
        UsVoterEligibilityResult result = service.evaluate(request);

        assertThat(result.eligible()).as(scenario).isEqualTo(expectedEligible);
        assertThat(result.provisionalBallotRequired()).as(scenario).isEqualTo(expectedProvisionalRequired);
        assertThat(result.reason()).isEqualTo(expectedReason);
    }

    @Test
    void voterEligibilityDmnDeclaresInputsOutputsAndRows() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(DMN.toFile());
        String xml = Files.readString(DMN);

        assertThat(xml).contains("name=\"citizenshipStatus\"");
        assertThat(xml).contains("name=\"birthDate\"");
        assertThat(xml).contains("name=\"electionDay\"");
        assertThat(xml).contains("name=\"generalElectionDay\"");
        assertThat(xml).contains("name=\"electionType\"");
        assertThat(xml).contains("upper case(string(stateCode))");
        assertThat(xml).contains("name=\"stateCode\"");
        assertThat(xml).contains("name=\"idVerificationStatus\"");
        assertThat(xml).contains("name=\"registered\"");
        assertThat(xml).contains("name=\"eligible\"");
        assertThat(xml).contains("name=\"provisionalBallotRequired\"");
        assertThat(xml).contains("name=\"reason\"");
        assertThat(xml).contains("Rule_PrimaryAgeByGeneralElection");
        assertThat(xml).contains("Rule_PrimaryAgeByGeneralHavaProvisional");
        assertThat(document.getElementsByTagNameNS("*", "rule").getLength()).isEqualTo(8);
    }

    private static Stream<Arguments> decisionRows() {
        return Stream.of(
                Arguments.of(
                        "non-citizens are ineligible",
                        request(UsCitizenshipStatus.NON_CITIZEN, bornAgeOnElectionDay(30), ELECTION_DAY,
                                UsElectionType.GENERAL_ELECTION, "CA", UsIdVerificationStatus.VERIFIED, true),
                        false,
                        false,
                        "US federal elections require citizenship"),
                Arguments.of(
                        "unregistered citizens are ineligible",
                        request(UsCitizenshipStatus.CITIZEN, bornAgeOnElectionDay(30), ELECTION_DAY,
                                UsElectionType.GENERAL_ELECTION, "CA", UsIdVerificationStatus.VERIFIED, false),
                        false,
                        false,
                        "voter registration is required"),
                Arguments.of(
                        "voter under 18 on general election day is ineligible",
                        request(UsCitizenshipStatus.CITIZEN, ELECTION_DAY.minusYears(18).plusDays(1), ELECTION_DAY,
                                UsElectionType.GENERAL_ELECTION, "CA", UsIdVerificationStatus.VERIFIED, true),
                        false,
                        false,
                        "voter must be 18 on election day"),
                Arguments.of(
                        "first-time mail registrant without HAVA ID votes provisionally",
                        request(UsCitizenshipStatus.CITIZEN, bornAgeOnElectionDay(30), ELECTION_DAY,
                                UsElectionType.GENERAL_ELECTION, "CA", UsIdVerificationStatus.UNVERIFIED_HAVA_ID, true),
                        true,
                        true,
                        "HAVA ID verification requires provisional ballot"),
                Arguments.of(
                        "ordinary verified general-election voter is eligible without provisional ballot",
                        request(UsCitizenshipStatus.CITIZEN, bornAgeOnElectionDay(30), ELECTION_DAY,
                                UsElectionType.GENERAL_ELECTION, "CA", UsIdVerificationStatus.VERIFIED, true),
                        true,
                        false,
                        "eligible verified voter"),
                Arguments.of(
                        "primary voter who will turn 18 by general election is eligible in allowed states",
                        request(UsCitizenshipStatus.CITIZEN, ELECTION_DAY.minusYears(18), PRIMARY_DAY,
                                UsElectionType.PRIMARY_ELECTION, "MD", UsIdVerificationStatus.VERIFIED, true),
                        true,
                        false,
                        "eligible primary voter turning 18 by general election"),
                Arguments.of(
                        "lowercase state code still receives documented Maryland primary age exception",
                        request(UsCitizenshipStatus.CITIZEN, ELECTION_DAY.minusYears(18), PRIMARY_DAY,
                                UsElectionType.PRIMARY_ELECTION, "md", UsIdVerificationStatus.VERIFIED, true),
                        true,
                        false,
                        "eligible primary voter turning 18 by general election"),
                Arguments.of(
                        "primary voter turning 18 by general election with unverified HAVA ID votes provisionally",
                        request(UsCitizenshipStatus.CITIZEN, ELECTION_DAY.minusYears(18), PRIMARY_DAY,
                                UsElectionType.PRIMARY_ELECTION, "MD", UsIdVerificationStatus.UNVERIFIED_HAVA_ID, true),
                        true,
                        true,
                        "HAVA ID verification requires provisional ballot"),
                Arguments.of(
                        "primary voter under 18 remains ineligible in standard states",
                        request(UsCitizenshipStatus.CITIZEN, ELECTION_DAY.minusYears(18), PRIMARY_DAY,
                                UsElectionType.PRIMARY_ELECTION, "CA", UsIdVerificationStatus.VERIFIED, true),
                        false,
                        false,
                        "voter must be 18 on primary election day"));
    }

    private static UsVoterEligibilityRequest request(
            UsCitizenshipStatus citizenshipStatus,
            LocalDate birthDate,
            LocalDate electionDay,
            UsElectionType electionType,
            String stateCode,
            UsIdVerificationStatus idVerificationStatus,
            boolean registered) {
        return new UsVoterEligibilityRequest(
                citizenshipStatus,
                birthDate,
                electionDay,
                ELECTION_DAY,
                electionType,
                stateCode,
                idVerificationStatus,
                registered);
    }

    private static LocalDate bornAgeOnElectionDay(int age) {
        return ELECTION_DAY.minusYears(age);
    }
}
