package com.exadel.frs.commonservice.entity;

import com.vladmihalcea.hibernate.type.array.DoubleArrayType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
@Table(schema = "public")
@Builder
@AllArgsConstructor
@TypeDefs(@TypeDef(name = "double-array", typeClass = DoubleArrayType.class))
@NamedEntityGraph(name = "embedding-with-subject", attributeNodes = {@NamedAttributeNode("subject"), @NamedAttributeNode("img")})
public class Embedding {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Type(type = "double-array")
    @Column(
            name = "embedding",
            columnDefinition = "float8[]",
            nullable = false
    )
    private double[] embedding;

    @Column(nullable = false)
    private String calculator;

    // Optional.
    // There could be predefined embeddings without image (pre-inserted demo embeddings).
    // There could multiple embeddings for same image (calculated with diff calculators).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "img_id", referencedColumnName = "id")
    private Img img;
}
