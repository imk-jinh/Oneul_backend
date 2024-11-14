package com.oneul.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class AlarmResponse {
    private int id;
    private String category;
    private boolean read_flag;
    private Date date;
    private String text;
}
