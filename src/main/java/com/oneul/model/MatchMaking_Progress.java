package com.oneul.model;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;

@Entity
public class MatchMaking_Progress {
    
    @Id
    private int id;

    private int user_id;
    private int diary_id;
    
    private LocalDateTime date;
}
