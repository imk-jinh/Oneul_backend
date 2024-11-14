package com.oneul.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Action_Recommendation {
    
    @Id
    private int id;

    private int diary_id;
    private String text;
}
