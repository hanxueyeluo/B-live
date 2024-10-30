package com.easylive.entity.vo;


import lombok.Data;

@Data
public class CheckCodeVO {
    private String checkCode;

    private String checkCodeKey;



    public String getCheckCode() {
        return checkCode;
    }

    public void setCheckCode(String checkCode) {
        this.checkCode = checkCode;
    }

    public String getCheckCodeKey() {
        return checkCodeKey;
    }

    public void setCheckCodeKey(String checkCodeKey) {
        this.checkCodeKey = checkCodeKey;
    }
}
