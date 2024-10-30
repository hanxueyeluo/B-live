package com.easylive.entity.enums;

public enum UserStatusEnum {
    DISABLE(0,"禁用"),

    ENABLE(1,"启用");

    private Integer status;
    private String desc;

    UserStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static UserStatusEnum getByStatus(Integer status){
        for(UserStatusEnum statusEnum:UserStatusEnum.values()){
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum;
            }
        }
        return null;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}
