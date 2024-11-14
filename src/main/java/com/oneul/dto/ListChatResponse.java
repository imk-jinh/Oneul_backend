package com.oneul.dto;

import java.sql.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListChatResponse {
    private int chatRoom_id;
    private String partner_id;
    private String partner_img;
    private Date time;
    private String text;
} 
