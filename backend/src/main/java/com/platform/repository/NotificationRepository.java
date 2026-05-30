package com.platform.repository;

import com.platform.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndReadFalse(String userId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
}