package com.sabbpe.admin.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_validations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentValidationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "validation_type", nullable = false)
    private String validationType;

    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = false;

    @Column(columnDefinition = "JSON")
    private String extractedData;

    @Column(columnDefinition = "TEXT")
    private String validationNotes;

    private Integer score = 0;

    @CreationTimestamp
    @Column(name = "validated_at", updatable = false)
    private LocalDateTime validatedAt;
}
