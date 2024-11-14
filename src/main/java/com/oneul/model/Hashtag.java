package com.oneul.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Hashtag {

    @Id
    private int id;

    private int diary_id;
    private String text;
}
