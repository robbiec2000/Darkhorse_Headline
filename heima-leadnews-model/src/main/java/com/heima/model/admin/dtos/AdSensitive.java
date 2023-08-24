package com.heima.model.admin.dtos;

import lombok.Data;

import java.util.Date;

@Data
public class AdSensitive {

    private Integer id;

    private String sensitives;

    private Date createdTime;
}
