package com.oneul.model;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;


@Entity
public class Chat_List {
    
    @Id
    private int id;

    private int partner_id;
    private int user_id;
    private Boolean end_flag;
    private int count;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime end_time;

}
