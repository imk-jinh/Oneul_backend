package com.oneul.model;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Member {
    @Id
    private int user_id;
    private String email;
    private String age;
    private String gender;
    private String image;
    private String token;
    private String nickname;
    private String language;
    private boolean kakao;
    private boolean google;
    private String viewMode;
}