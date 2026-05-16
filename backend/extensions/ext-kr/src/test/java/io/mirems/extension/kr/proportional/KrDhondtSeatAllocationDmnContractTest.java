package io.mirems.extension.kr.proportional;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class KrDhondtSeatAllocationDmnContractTest {
    private static final Path DMN = Path.of("src/main/resources/decisions/kr/KrDhondtSeatAllocation.dmn");

    @Test
    void dmnDefinesDhondtSeatAllocationDecisionContract() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(DMN.toFile());
        String xml = Files.readString(DMN);

        assertThat(document.getDocumentElement().getAttribute("name")).isEqualTo("KrDhondtSeatAllocation");
        assertThat(xml).contains("name=\"totalSeats\"");
        assertThat(xml).contains("name=\"partyVotes\"");
        assertThat(xml).contains("name=\"seatAllocations\"");
        assertThat(xml).contains("count(partyVotes) = 0");
        assertThat(xml).contains("some vote in partyVotes satisfies vote.votes &lt; 0");
        assertThat(xml).contains("sort(flatten(for vote in partyVotes return for divisor in 1..totalSeats");
        assertThat(xml).contains("left.value &gt; right.value");
        assertThat(xml).contains("left.votes &gt; right.votes");
        assertThat(xml).contains("left.partyId &lt; right.partyId");
        assertThat(xml).contains("D'Hondt highest quotients");
        assertThat(xml).doesNotContain("valid party vote list");
        assertThat(xml).doesNotContain("empty or contains negative votes");
        assertThat(document.getElementsByTagNameNS("*", "rule").getLength()).isEqualTo(3);
    }
}
