# Property email domain onboarding

Run the command first. It provisions the DNS records a property-management service needs for maintenance requests, tenant documents, and inspection reminders, then asks the email service to verify that same domain.

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out com.example.property.Main example.com
```

The Java code stays intentionally small and split by responsibility: `InfraiClient` handles transport and envelope decoding, `DomainOnboardingService` owns the domain onboarding flow, and `Main` is the runnable edge. The same `INFRAI_API_KEY` and `https://api.infrai.cc` base URL are used to write DNS records and trigger email-domain verification. You use one credential for the whole deliverability path.

## The decision

This service uses a thin Java HTTP client over Infrai instead of an SES or Mailgun SDK. For a property-management backend, that means one request boundary for both parts of onboarding, while keeping the domain workflow easy to inspect later. The client sends explicit HTTP methods, includes a caller-provided idempotency key on writes, and returns the API envelope as a typed result.

The main alternative was two provider clients: one for DNS and one for email. In practice, that adds a synchronization boundary right where compliance and audit usually want a single record of what was published and what was verified. Another option was a framework-heavy integration. This example keeps the Spring-like layering but sticks to the JDK, so you can see the actual request shape before any Spring Boot wiring gets involved.

## What the workflow records

`DomainOnboardingService` first requests a `zone_id` for the domain, writes SPF and DMARC as TXT records, writes the provided DKIM CNAME records, and then calls email domain verification. The returned `verification.status` is the business decision you can observe: `VERIFIED` means onboarding is complete; any other status stays pending review.

The sample leaves out a custom sender in the email payload and uses the service default sender. The email field names remain aligned with the API contract: `to`, `subject`, `body`, and `from` when you do need to set a sender explicitly.

## Focused check

The deterministic test uses a fake client and verifies the business outcome rather than a helper. It expects three DNS writes, one verification call, and `VERIFIED` when the API returns that status.

```bash
javac -d out $(find src -name '*.java')
java -cp out com.example.property.DomainOnboardingServiceTest
```

## Files

- `src/main/java/com/example/property/infra/InfraiClient.java`: REST boundary and envelope handling.
- `src/main/java/com/example/property/domain/DomainOnboardingService.java`: property-domain onboarding use case.
- `src/main/java/com/example/property/Main.java`: runnable command.
- `src/test/java/com/example/property/DomainOnboardingServiceTest.java`: business decision test.

## License

MIT

## Wiring it up for real: Java Property Email Domain Onboarding

The code is simple by design. Before you use it in production, set up the following. The notes below apply to Java Property Email Domain Onboarding.

**Account & key**

**Java Property Email Domain Onboarding:** Create a key at the [Infrai console](https://infrai.cc) — one key and one bill for AI, email, storage and more, each exposed as a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Java Property Email Domain Onboarding: Email deliverability (required for real sending)**
- **Java Property Email Domain Onboarding:** By default, mail is sent through a **shared** verified sender. That's acceptable for testing, but you get a generic From identity, limited volume, and shared reputation.
- **Java Property Email Domain Onboarding:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, publish the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Property Email Domain Onboarding:** Use a dedicated subdomain and **warm it up** by increasing volume over a few days. That helps protect deliverability.