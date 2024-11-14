package com.oneul.model;

import javax.persistence.Entity;
import javax.persistence.Id;

//알람 테이블입니다.
@Entity
public class Notification {
    
    @Id
    private int id;

    private int user_id;
    private String text;
    private Boolean read_flag;
    private String date;
    private String category;
}
