package com.oneul.model;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Ai_Conversation {
    
    @Id
    private int user_id;

    private LocalDateTime date;
    private String who;
    private String text;
}
