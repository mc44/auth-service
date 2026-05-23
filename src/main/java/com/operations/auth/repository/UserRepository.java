package com.operations.auth.repository;

import com.operations.auth.model.UserDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserDocument, String> {
  Optional<UserDocument> findByTenantIdAndEmailIgnoreCase(String tenantId, String email);
}
