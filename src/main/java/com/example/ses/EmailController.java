package com.example.ses;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/v1/emails")
public class EmailController {

    private static final int MAX_LIST_LIMIT = 200;

    public record SendEmailResponse(String id, Instant sentAt) {
    }

    public record EmailList(List<EmailSummary> emails) {
    }

    private final EmailService service;
    private final JsonMapper jsonMapper;
    private final int maxBodyBytes;

    public EmailController(EmailService service, JsonMapper jsonMapper, SesProperties properties) {
        this.service = service;
        this.jsonMapper = jsonMapper;
        this.maxBodyBytes = properties.maxBodyBytes();
    }

    /** Reads the raw stream so the size limit also covers chunked requests, which have no Content-Length. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SendEmailResponse> send(InputStream body) throws IOException {
        byte[] bytes = body.readNBytes(maxBodyBytes + 1);
        if (bytes.length > maxBodyBytes) {
            throw new ApiException.TooLarge(maxBodyBytes);
        }
        Email email = service.send(jsonMapper.readValue(bytes, SendEmailRequest.class));
        return ResponseEntity.created(URI.create("/v1/emails/" + email.id()))
                .body(new SendEmailResponse(email.id(), email.sentAt()));
    }

    @GetMapping("/{id}")
    public Email get(@PathVariable String id) {
        return service.get(id);
    }

    @GetMapping
    public EmailList list(@RequestParam(defaultValue = "50") int limit) {
        if (limit < 1 || limit > MAX_LIST_LIMIT) {
            throw new ApiException.Invalid(List.of("limit: must be between 1 and " + MAX_LIST_LIMIT));
        }
        return new EmailList(service.list(limit));
    }
}
