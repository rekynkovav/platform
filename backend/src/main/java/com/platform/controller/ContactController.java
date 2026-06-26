package com.platform.controller;

import com.platform.model.dto.ContactDto;
import com.platform.service.ContactService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ResponseEntity<List<ContactDto>> getContacts(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(contactService.getUserContacts(jwt.getSubject(), search));
    }

    @PostMapping
    @RateLimiting(name = "contact-limit")
    public ResponseEntity<ContactDto> addContact(
            @Valid @RequestBody ContactDto.AddRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(contactService.addContact(jwt.getSubject(), request.getContactUserId(), request.getNickname()));
    }

    @DeleteMapping("/{contactId}")
    @RateLimiting(name = "contact-limit")
    public ResponseEntity<Void> removeContact(
            @PathVariable String contactId,
            @AuthenticationPrincipal Jwt jwt) {

        contactService.removeContact(jwt.getSubject(), contactId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{contactId}")
    @RateLimiting(name = "contact-limit")
    public ResponseEntity<ContactDto> updateContactNickname(
            @PathVariable String contactId,
            @Valid @RequestBody ContactDto.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(contactService.updateContactNickname(jwt.getSubject(), contactId, request.getNickname()));
    }
}
