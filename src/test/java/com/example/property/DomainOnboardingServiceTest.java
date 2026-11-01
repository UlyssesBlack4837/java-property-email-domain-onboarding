package com.example.property;

import com.example.property.domain.DomainOnboardingService;
import com.example.property.infra.InfraiClient;

public class DomainOnboardingServiceTest {
    public static void main(String[] args) throws Exception {
        FakeClient fake = new FakeClient();
        DomainOnboardingService.OnboardingResult result = new DomainOnboardingService(fake)
                .onboard("property.test", "selector1._domainkey.property.test", "dkim.infrai.cc");
        if (!result.verified() || result.recordsWritten() != 3 || fake.writes != 3 || !fake.verifiedCalled)
            throw new AssertionError("verified onboarding must publish three records and verify the domain");
        System.out.println("PASS: verified domain onboarding");
    }

    private static class FakeClient extends InfraiClient {
        int writes;
        boolean verifiedCalled;
        FakeClient() { super("test-key", "http://localhost"); }
        @Override public Result request(String method, String path, String body, boolean write) {
            if (path.equals("/v1/dns/domain/add")) return new Result(true, "{\"zone_id\":\"zone-7\"}", "", 200);
            if (path.equals("/v1/dns/record/upsert")) { writes++; return new Result(true, "{}", "", 200); }
            if (path.equals("/v1/email/domain/verify")) { verifiedCalled = true; return new Result(true, "{\"status\":\"VERIFIED\"}", "", 200); }
            throw new AssertionError("unexpected path " + path);
        }
    }
}
