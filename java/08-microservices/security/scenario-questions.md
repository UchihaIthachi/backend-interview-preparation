# Scenario-based Questions

**Scenario/Question:** How will you design secure communication between services?



# Advanced Interview Questions & Answers

**Scenario/Question:** 76. How would you ensure personally identifiable information (PII) never appears in application logs?

**Answer:** Implement custom log appenders/filters that mask or redact fields like SSN, card numbers, or passwords. Avoid logging raw HTTP request/response payloads containing PII.

**Scenario/Question:** 77. A developer accidentally commits production database credentials to Git. What immediate actions would you take?

**Answer:** Immediately revoke and rotate the compromised credentials. Invalidate active database connections. Remove the secret from Git history (BFG repo cleaner). Conduct an audit to check if the credentials were used.

**Scenario/Question:** 78. How would you implement the "maker-checker" approval workflow for high-value banking operations?

**Answer:** Design the system with two distinct roles. The 'maker' initiates the transaction, storing it in a 'PENDING_APPROVAL' state. The 'checker' (a different user) reviews and transitions state to 'APPROVED' or 'REJECTED'.

**Scenario/Question:** 79. How would you prevent internal employees from accessing customer data without authorization?

**Answer:** Implement Role-Based Access Control (RBAC), enforce least privilege, mandate MFA, and log all data access events. Use data masking in internal dashboards to obscure PII.

**Scenario/Question:** 80. A regulator requires that deleted customer records remain recoverable for seven years. How would you design this?

**Answer:** Implement "Soft Deletes" (e.g., setting an `is_deleted` flag). Move historical data to an archiving database or cold storage (like S3 Glacier) periodically, maintaining the required retention policy.

**Scenario/Question:** 81. How would you enforce password rotation for service accounts without causing downtime?

**Answer:** Use a secret management system (like HashiCorp Vault) to dynamically generate short-lived database credentials, or support dual active passwords, rotating them one at a time across application instances.

**Scenario/Question:** 82. How would you secure communication between internal banking services beyond HTTPS?

**Answer:** Implement Mutual TLS (mTLS) for service-to-service authentication, use a service mesh (e.g., Istio), and enforce network policies to restrict traffic flow between specific pods/containers.

**Scenario/Question:** 83. A penetration test reveals predictable account identifiers in your APIs. How would you fix this?

**Answer:** Replace sequential IDs (1, 2, 3) with UUIDs or cryptographically secure opaque identifiers in API requests, while keeping integer IDs strictly internal to the database.

**Scenario/Question:** 84. How would you detect unusual transaction patterns that may indicate fraud without blocking legitimate customers?

**Answer:** Use machine learning models or rule engines to calculate a risk score based on velocity, location, and behavior. Flag risky transactions for manual review or challenge the user with MFA instead of outright blocking.

**Scenario/Question:** 85. How would you ensure that audit logs themselves cannot be tampered with?

**Answer:** Ship logs immediately to an isolated, append-only centralized logging server. Use WORM (Write Once Read Many) storage, and mathematically hash/chain log entries to detect modifications.

**Scenario/Question:** 86. A privileged administrator needs temporary access to production for troubleshooting. How would you manage and audit this access?

**Answer:** Use Just-In-Time (JIT) access granting temporary, time-bound credentials. Record all terminal sessions (via jump hosts/bastions), and require a linked Jira/ServiceNow ticket for the access request.

**Scenario/Question:** 87. How would you design APIs to comply with data minimization principles?

**Answer:** Return only the specific data fields requested by the client (e.g., using GraphQL or sparse fieldsets in REST). Avoid "select *" mentalities and do not return full objects if only a status is needed.

**Scenario/Question:** 88. How would you verify that sensitive fields remain encrypted throughout processing?

**Answer:** Use field-level encryption at the application layer before sending data to the DB. Ensure data in memory is zeroed out after use (using `char[]` instead of `String` for passwords).

**Scenario/Question:** 89. A customer exercises the "right to be forgotten," but banking regulations require transaction retention. How would you reconcile these requirements?

**Answer:** Anonymize/pseudonymize the transaction records by deleting the link to the PII, effectively "forgetting" the user while preserving the statistical and financial integrity of the transaction ledger.

**Scenario/Question:** 90. How would you demonstrate compliance during an external security audit?

**Answer:** Provide comprehensive architecture diagrams, automated security scan reports (SAST/DAST/SCA), penetration test results, incident response playbooks, and immutable audit logs proving access controls.

