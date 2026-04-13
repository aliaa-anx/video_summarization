package com.backend_microservices.audit_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Data
@Document(indexName = "audit_logs")   // ELK annotation, it is composed of indices, and we will create one index to search for our logs inside
public class AuditLog {

    @Id
    private String id;

    private String userId;          // the user that is responsible for this log
    private String action;          // USER_LOGIN, UPLOAD_SUMMARIZE_MEETING, ASK_QUESTION
    private String serviceName;     // the service name, duh -_- : ai-service, api-gateway
    private String endpoint;        // the api request: /api/auth/login
    private String method;          // GET or POST or WHATEVER
    private String status;          // FAIL or 200 or 400 or any other
    private String details;         // the error in case of failure, the time taken in case of success, any useless filler

    @Field(type = FieldType.Date)   // this one was really annoying >:(, explicit elk datatype to be able to sort the logs according to their timestamp
    private Instant timestamp;
}