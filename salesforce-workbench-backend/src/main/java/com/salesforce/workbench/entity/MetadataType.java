package com.salesforce.workbench.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "metadata_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataType {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "connection_id", nullable = false)
    private String connectionId;

    @Column(name = "xml_name", nullable = false)
    private String xmlName;

    @Column(name = "directory_name")
    private String directoryName;

    @Column(name = "in_folder")
    private boolean inFolder;

    @Column(name = "meta_file")
    private boolean metaFile;

    @Column(name = "suffix")
    private String suffix;
}
