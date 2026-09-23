package com.powerpuff.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CharacteristicDiagnosticsTest {
    private final ObjectMapper json = new ObjectMapper();
    @Test void identifies160versus250AndPreservesSources() throws Exception {
        var result = CharacteristicDiagnostics.conflicts("027228 АВ DRX250 MT 3ф 160А 18ka Legrand (1)",
                "Номинальный ток: 160 А; отключающая способность 18кА", json.readTree("{\"NOMINALNYY_TOK\":\"250 А\"}"));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().evidence()).extracting(e -> e.source()).containsExactly("name", "description", "properties.NOMINALNYY_TOK");
        assertThat(result.getFirst().evidence()).extracting(e -> e.rawValue()).containsExactly("160А", "160 А", "250 А");
        assertThat(result.getFirst().message()).contains("160", "250", "не определено");
    }
    @Test void equivalentSpacesCaseAndUnitsDoNotConflict() throws Exception {
        for (String property : new String[]{"160 А", "160A", "160,0 а", "160\u00a0А", "0,160 кА", "160"})
            assertThat(CharacteristicDiagnostics.conflicts("Автомат 160А", "Ток 160\u202fА. 18кА, 30мА", json.createObjectNode().put("NOMINALNYY_TOK", property))).isEmpty();
    }
    @Test void cardJsonContainsWarningEvidenceAndCertificateDataWithoutChangingSource() throws Exception {
        var props=json.readTree("{\"NOMINALNYY_TOK\":\"250 А\",\"CERTIFICATE\":[\"https://example.test/certificate.pdf\"]}");
        var p=new com.powerpuff.backend.dto.CatalogProductResponse(515291L,"Автомат 160А","000_","Ток 160 А",null,null,null,null,null,null,props);
        var tree=json.valueToTree(p);
        assertThat(tree.path("warnings").get(0).asText()).contains("Расхождение", "250");
        assertThat(tree.path("properties")).isEqualTo(props);
        assertThat(tree.path("certificateSources").path("properties.CERTIFICATE").get(0).asText()).endsWith("certificate.pdf");
        assertThat(tree.path("stockStatus").asText()).isEqualTo("UNKNOWN");
    }
    @Test void seriesRangeIsNotTheProductNominalCurrent() {
        assertThat(CharacteristicDiagnostics.conflicts("Автомат 20А", "В ассортимент входят выключатели с номинальным током от 6 до 63А", json.createObjectNode().put("NOMINALNYY_TOK", "20 А"))).isEmpty();
    }
    @Test void descriptionAloneCanRevealDisagreement() {
        assertThat(CharacteristicDiagnostics.conflicts("Автомат", "Номинальный ток 160 А", json.createObjectNode().put("NOMINALNYY_TOK", "250 А"))).hasSize(1);
    }
}
