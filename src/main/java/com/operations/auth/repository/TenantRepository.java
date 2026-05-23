package com.operations.auth.repository;

import com.operations.auth.model.TenantDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TenantRepository extends MongoRepository<TenantDocument, String> {}
