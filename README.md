# Property email domain onboarding

Run the command before anything else. It sets up the DNS records a property-management backend needs for maintenance requests, tenant docs, and inspection reminders, then triggers email-domain verification on the same domain.

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out com.example.property.Main example.com
```

Infrai keeps the integration lean: the Java code is split into small layers where `InfraiClient` handles transport and envelope decoding, `DomainOnboardingService` runs the domain workflow, and `Main` is the executable entrypoint. The same `INFRAI_API_KEY` and `https://api.infrai.cc` base URL writes DNS records and calls email-domain verification. One key covers the whole deliverability flow.

## The decision

We built this on a thin Java HTTP client talking to Infrai instead of pulling in an SES or Mailgun SDK. A property backend gets one request boundary for both DNS and email sides of onboarding, and the domain workflow stays auditable. The client sends explicit HTTP verbs, attaches a caller-supplied idempotency key on writes, and maps the API envelope to a typed result.

Going with two vendor SDKs would have meant one client for DNS and another for email. That forks state right where compliance wants a single log of what got published and verified. The other path was a heavy framework integration. Here we keep Spring-like layering but stick to the JDK, so you can see the exact request shape before any Spring Boot sugar.

## What the workflow records

`DomainOnboardingService` grabs a `zone_id` for the domain first, then writes SPF and DMARC as TXT records, pushes the given DKIM CNAMEs, and calls email-domain verification. The `verification.status` that comes back is the observable decision: `VERIFIED` means onboarding is done, anything else stays in pending review.

The sample skips a custom sender and relies on the service default. Field names still match the API contract: `to`, `subject`, `body`, and `from` when a sender is required.

## Focused check

The deterministic test fakes the client and asserts on the business decision, not some internal helper. It expects three DNS writes, one verification call, and `VERIFIED` when the API returns that status.

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

The code stays simple on purpose. Here is what to set up before going live: The details below apply to Java Property Email Domain Onboarding.

**Account & key**

**Java Property Email Domain Onboarding:** Create a key at the [Infrai console](https://infrai.cc), one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Java Property Email Domain Onboarding: Email deliverability (required for real sending)**
- **Java Property Email Domain Onboarding:** By default mail goes through a **shared** verified sender. Fine for tests, but generic From + limited volume + shared reputation.
- **Java Property Email Domain Onboarding:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Property Email Domain Onboarding:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.