# SES

A small email sending service with a REST API, inspired by Amazon SES.

This is a prototype. **No real email is sent.** Accepted emails are captured in memory, where the API can read them back, and they are lost on restart.

## Run

Requires Java 26. The API key is mandatory and must be at least 16 characters.

```bash
SES_API_KEY=change-me-to-a-long-secret ./mvnw spring-boot:run
```

The server listens on `127.0.0.1:8080`.

## API

Every request needs `Authorization: Bearer <api key>`.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/v1/emails` | Send an email. Returns `201` with `id` and `sentAt`. |
| `GET` | `/v1/emails/{id}` | Read a captured email in full. |
| `GET` | `/v1/emails?limit=50` | List summaries, newest first. `limit` is 1 to 200. |

```bash
curl -i localhost:8080/v1/emails \
  -H 'Authorization: Bearer change-me-to-a-long-secret' \
  -H 'Content-Type: application/json' \
  -d '{
    "from": "sender@example.com",
    "to": ["alice@example.com"],
    "cc": [],
    "bcc": [],
    "subject": "Hello",
    "text": "Plain text body",
    "html": "<p>HTML body</p>"
  }'
```

### Rules

- `from` and `subject` are required. `from` must be a bare address, not `Name <address>`.
- At least 1 and at most 50 recipients across `to`, `cc` and `bcc`.
- At least one of `text` and `html` is required.
- The request body can be at most 1 MiB.

### Errors

The failures below return this JSON shape, with one `details` entry per validation problem:

```json
{"error": "invalid_request", "message": "The request is invalid", "details": ["from: must be a valid email address"]}
```

| Status | Meaning |
| --- | --- |
| `400` | Invalid fields, malformed JSON or a bad `limit` |
| `401` | Missing or wrong API key |
| `404` | No such path, no email with that id, or the email was evicted |
| `413` | Request body too large |
| `405` | Method not supported on that path |
| `415` | `Content-Type` is not `application/json` |

## Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `SES_API_KEY` | none | Bearer token clients must present |
| `SES_MAX_BODY_BYTES` | `1048576` | Largest accepted request body |
| `SES_MAX_STORE_BYTES` | `67108864` | Approximate memory budget. The oldest emails are evicted beyond it. |
| `SERVER_ADDRESS` | `127.0.0.1` | Listen address |
| `SERVER_PORT` | `8080` | Listen port |

There is no TLS, so keep the service on loopback or put it behind a TLS proxy.

## Test

```bash
./mvnw test
```
