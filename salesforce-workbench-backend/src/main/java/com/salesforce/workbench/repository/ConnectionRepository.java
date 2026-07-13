package com.salesforce.workbench.repository;

import com.salesforce.workbench.entity.SalesforceConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConnectionRepository extends JpaRepository<SalesforceConnection, String> {
}
