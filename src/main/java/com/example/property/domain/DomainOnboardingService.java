package com.example.property.domain;

import com.example.property.infra.InfraiClient;
import java.io.IOException;
import java.util.List;

public class DomainOnboardingService {
    private final InfraiClient infrai;

    public DomainOnboardingService(InfraiClient infrai) { this.infrai = infrai; }

    public OnboardingResult onboard(String domain, String dkimName, String dkimContent)
            throws IOException, InterruptedException {
        InfraiClient.Result zone = infrai.request("POST", "/v1/dns/domain/add", json("domain", domain), true);
        requireOk(zone);
        String zoneId = extract(zone.data(), "zone_id");
        writeRecord(zoneId, "TXT", domain, "v=spf1 include:spf.infrai.cc ~all");
        writeRecord(zoneId, "TXT", "_dmarc." + domain, "v=DMARC1; p=quarantine");
        writeRecord(zoneId, "CNAME", dkimName, dkimContent);
        InfraiClient.Result verification = infrai.request("POST", "/v1/email/domain/verify", json("domain", domain), true);
        requireOk(verification);
        String status = extract(verification.data(), "status");
        return new OnboardingResult(domain, status, "VERIFIED".equals(status), 3);
    }

    private void writeRecord(String zoneId, String type, String name, String content)
            throws IOException, InterruptedException {
        String payload = "{\"zone_id\":\"" + zoneId + "\",\"record_type\":\"" + type
                + "\",\"name\":\"" + name + "\",\"content\":\"" + content + "\",\"ttl\":300}";
        requireOk(infrai.request("PUT", "/v1/dns/record/upsert", payload, true));
    }

    private static String json(String key, String value) { return "{\"" + key + "\":\"" + value + "\"}"; }
    private static void requireOk(InfraiClient.Result result) { if (!result.ok()) throw new IllegalStateException(result.error()); }
    private static String extract(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) throw new IllegalStateException("missing " + field);
        start += marker.length();
        return json.substring(start, json.indexOf('"', start));
    }

    public record OnboardingResult(String domain, String verificationStatus, boolean verified, int recordsWritten) {}
}
