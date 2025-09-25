package com.example.app.dataAccess;

import com.example.app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User,Long> {
    User findByName(String user);
}
