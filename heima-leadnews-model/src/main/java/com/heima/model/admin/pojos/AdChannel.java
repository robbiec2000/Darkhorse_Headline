package com.heima.model.admin.pojos;

import lombok.Data;

import java.util.Date;

@Data
public class AdChannel {

    private Integer id;

    private String description;

    private Date createdTime;

    private Boolean isDefault;

    private String name;

    private Integer ord;

    private Boolean status;
}
