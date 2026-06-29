package com.platform.integration;

import com.platform.model.Contact;
import com.platform.model.User;
import com.platform.repository.ContactRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Contact management.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContactIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ContactRepository contactRepository;
    @Autowired private UserRepository userRepository;

    private String ownerUserId;
    private String contactUserId;
    private org.springframework.security.oauth2.jwt.Jwt ownerJwt;
    private org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken ownerAuth;

    @BeforeEach
    void setUp() {
        ownerUserId = userId();
        contactUserId = userId();
        ownerJwt = createJwt(ownerUserId, "owner-" + ownerUserId.substring(0, 8), "USER_0");
        ownerAuth = createAuth(ownerJwt, "USER_0");

        userRepository.save(User.builder().id(UUID.fromString(ownerUserId))
                .username("owner-" + ownerUserId.substring(0, 8)).role("USER_0").version(0L).build());
        userRepository.save(User.builder().id(UUID.fromString(contactUserId))
                .username("contact-" + contactUserId.substring(0, 8)).role("USER_0").version(0L).build());
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/contacts — add a contact")
    void addContact() throws Exception {
        var request = new com.platform.model.dto.ContactDto.AddRequest();
        request.setContactUserId(contactUserId);
        request.setNickname("My Friend");

        mockMvc.perform(post("/api/contacts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactUserId").value(contactUserId))
                .andExpect(jsonPath("$.contactName").value("My Friend"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/contacts — list contacts")
    void getContacts() throws Exception {
        contactRepository.save(Contact.builder()
                .id(UUID.randomUUID().toString())
                .userId(ownerUserId)
                .contactUserId(contactUserId)
                .contactName("Buddy")
                .version(0L)
                .build());

        mockMvc.perform(get("/api/contacts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Order(3)
    @DisplayName("DELETE /api/contacts/{contactId} — remove contact")
    void removeContact() throws Exception {
        // Add contact first
        mockMvc.perform(post("/api/contacts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactUserId\":\"" + contactUserId + "\"}"))
                .andExpect(status().isOk());

        // Get the contact ID
        var result = mockMvc.perform(get("/api/contacts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var contacts = objectMapper.readValue(responseBody,
                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.platform.model.dto.ContactDto>>() {});
        String contactId = contacts.get(0).getId();

        // Delete it
        mockMvc.perform(delete("/api/contacts/{contactId}", contactId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(ownerAuth)))
                .andExpect(status().isNoContent());

        // Verify empty
        mockMvc.perform(get("/api/contacts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/contacts/{contactId} — update nickname")
    void updateNickname() throws Exception {
        mockMvc.perform(post("/api/contacts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactUserId\":\"" + contactUserId + "\",\"nickname\":\"Old Name\"}"))
                .andExpect(status().isOk());

        // Find the contact ID
        var result = mockMvc.perform(get("/api/contacts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var contacts = objectMapper.readValue(responseBody,
                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.platform.model.dto.ContactDto>>() {});
        String contactId = contacts.get(0).getId();

        mockMvc.perform(put("/api/contacts/{contactId}", contactId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactName").value("New Name"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/contacts?search= — search contacts")
    void searchContacts() throws Exception {
        contactRepository.save(Contact.builder()
                .id(UUID.randomUUID().toString())
                .userId(ownerUserId)
                .contactUserId(contactUserId)
                .contactName("Searchable Friend")
                .version(0L)
                .build());

        mockMvc.perform(get("/api/contacts")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(ownerAuth))
                        .param("search", "Searchable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
