package com.jpmc.midascore.repository;

import com.jpmc.midascore.entity.UserRecord;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserRecord, Long> {
    Optional<UserRecord> findById(long id);
}
