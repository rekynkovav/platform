package com.platform.repository;

import com.platform.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, String> {

    List<Contact> findByUserIdOrderByContactNameAsc(String userId);

    List<Contact> findByUserIdAndContactNameContainingIgnoreCase(String userId, String contactName);

    Optional<Contact> findByUserIdAndContactUserId(String userId, String contactUserId);

    boolean existsByUserIdAndContactUserId(String userId, String contactUserId);

    void deleteByUserIdAndContactUserId(String userId, String contactUserId);
}
