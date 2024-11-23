package com.easylive.entity.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadFileDto implements Serializable {
    private static final long serialVersionUID=844272933084899283L;
    private String uploadId;
    private String fileName;
    private Integer chunkIndex;
    private Integer chunks;
    private Long fileSize=0L;
    private String filePath;
}
