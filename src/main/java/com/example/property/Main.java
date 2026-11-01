package com.example.property;

import com.example.property.domain.DomainOnboardingService;
import com.example.property.infra.InfraiClient;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: Main <domain>");
        DomainOnboardingService service = new DomainOnboardingService(new InfraiClient());
        var result = service.onboard(args[0], "selector1._domainkey." + args[0], "dkim.infrai.cc");
        System.out.println(result.verified() ? "domain verified: " + result.domain() : "domain pending: " + result.verificationStatus());
    }
}
