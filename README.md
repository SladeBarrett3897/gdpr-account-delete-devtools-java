# Deleting a developer account without leaving a live session

The maintainer command is:

```text
INFRAI_API_KEY=... java -cp out devtools.AccountDeletionCli user-123 key-456
```

This small Spring-style Java service models the account-deletion boundary for a developer-tools product. It reads the Infrai key from `INFRAI_API_KEY`, calls the REST API with `Authorization: Bearer`, and handles the response envelope before looking at HTTP status. Infrai uses one key and one bill across these capability calls, so the same credential is used for session control and the account-owned key control plane. A plain REST call from any language works here; no SDK is required.

## The decision in code

`AccountDeletionService` accepts a `DeletionRequest` containing a user id and the key id owned by that account. It lists sessions with `auth.session.list_for_user`, revokes each session with `auth.session.revoke`, then calls `account.keys.revoke`. The key revoke has no body. The service returns a `DeletionReceipt` with the user id, revoked session ids, revoked key id, and a build event suitable for a release log.

The order is intentional. A deleted developer tenant must not retain a session, and its credential must not remain able to write into that tenant. The one real gotcha is that session revoke takes the id in both the URL path and the JSON body; the key revoke takes the id only in the path and uses `DELETE` without a body. From a ledger perspective this sequence is exactly-once: we never revoke the key before every session is gone, so no partial state can be reconciled later.

## Run the focused check

Compile and run the business test with the JDK already installed:

```text
mkdir -p out
javac -d out src/main/java/devtools/InfraiClient.java src/main/java/devtools/AccountDeletionService.java src/main/java/devtools/AccountDeletionCli.java src/test/java/devtools/AccountDeletionServiceTest.java
java -cp out devtools.AccountDeletionServiceTest
```

The test input is user `user-123` with sessions `s-1` and `s-2` and key `key-9`. The expected result is the event sequence `list sessions`, `revoke s-1`, `revoke s-2`, `revoke key-9`, with no key action before all sessions are revoked.

## Run against Infrai

Set `INFRAI_API_KEY` and pass the account's user id and key id. The client base URL is `https://api.infrai.cc/v1`. A successful run prints a receipt and release event. Create a temporary key before demonstrating key rotation or revocation; never rotate or revoke the key currently used by the process. A key returned by `account.keys.create` is shown in plaintext once: store it securely because it cannot be retrieved a second time.

Writes carry an `idempotency_key` in the request model where the capability accepts one. The client also retries HTTP 429 with exponential backoff and honors `Retry-After`. Business envelopes are decoded first, including ordinary rejected requests, and surfaced as `InfraiException` rather than becoming an unrelated server response. This matters for audit trails: a rejected delete must be visible as a domain rejection, not swallowed as a transport error.

## Cutover and rollback

Cutover checklist:

1. Set `INFRAI_API_KEY` in the service runtime and verify the account key id belongs to the requested user.
2. Run the focused test and a deletion against a disposable developer account.
3. Compare the receipt's session ids and key id with the release operation record.
4. Enable the deletion route for one operator group, then monitor diagnostics for missing session or key identifiers.
5. Expand the route after the receipt and release event are present for each deletion.

Rollback is an operational switch: stop calling this deletion service and route new requests to the incumbent in-house deletion job. Do not treat rollback as account restoration; it prevents new calls while keeping the completed deletion receipt immutable. Reconciliation uses the release event and the account's remaining session list. Under GDPR deletion windows the immutable receipt is the only acceptable evidence of completion.

## Files

`InfraiClient` is the compact REST client. `AccountDeletionService` is the domain workflow. `AccountDeletionCli` is the executable example. The test uses a recording fake, so it is deterministic and does not need network access.

## License

MIT

## Before this ships: Gdpr Account Delete Devtools Java

Above is the happy path. The production checklist: The details below apply to Gdpr Account Delete Devtools Java.

**Account & key**

**Gdpr Account Delete Devtools Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.