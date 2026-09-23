package com.powerpuff.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
class PurchaseTermsServiceTest {
    @TempDir Path dir;
    @Test void absentTermsAreExplicitlyUnverified() {
        assertThat(new PurchaseTermsService(new ObjectMapper(), "").get().path("verified").asBoolean()).isFalse();
    }
    @Test void configuredSourceIsRequiredAndEmptyTermsAreRejected() throws Exception {
        Path file=dir.resolve("terms.json");
        Files.writeString(file,"{\"verified\":true,\"source\":\"TEST FIXTURE ONLY\",\"payment\":\"test payment\",\"delivery\":\"test delivery\",\"minimumOrder\":\"test minimum\"}");
        var service=new PurchaseTermsService(new ObjectMapper(),file.toString());
        assertThat(service.get().path("verified").asBoolean()).isTrue();
        Files.writeString(file,"{\"verified\":true,\"source\":\"TEST FIXTURE ONLY\",\"payment\":\"\",\"delivery\":\"test\",\"minimumOrder\":\"test\"}");
        assertThatThrownBy(service::get).isInstanceOf(com.powerpuff.backend.commerce.BusinessException.class);
    }
}
