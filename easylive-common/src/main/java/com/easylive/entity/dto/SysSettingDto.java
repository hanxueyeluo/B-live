package com.easylive.entity.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingDto implements Serializable {
    private static final long serialVersionUID=-90520876542443938L;
    private Integer registerCoinCount=10;
    private Integer postVideoCoinCount=5;
    private Integer videoSize=5;
    private Integer videoPCount=10;
    private Integer videoCount=10;
    private Integer commentCount=20;
    private Integer danmuCount=20;
}
