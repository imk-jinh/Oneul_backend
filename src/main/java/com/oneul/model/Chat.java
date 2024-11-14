package com.oneul.model;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.springframework.data.annotation.CreatedDate;

@Entity
public class Chat {
    
    @Id
    private int chat_id;

    private int user_id;
    private int partner_id;
    private Boolean read_flag;
    private String text;

    @CreatedDate
    private LocalDateTime date;
}
