package com.operations.auth.repository;

import com.operations.auth.model.RefreshTokenDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RefreshTokenRepository extends MongoRepository<RefreshTokenDocument, String> {
  Optional<RefreshTokenDocument> findByTokenHash(String tokenHash);

  List<RefreshTokenDocument> findByUserId(String userId);
}
