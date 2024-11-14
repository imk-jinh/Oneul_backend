package com.oneul.model;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Diary_List {

    @Id
    private int diary_id;

    private int user_id;
    private String title;
    private String text;

    private LocalDateTime date;
    private String emotion;
    
}
