package com.oneul.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiaryUpdateRequest { 
    private int diary_id;
    private String title;
    private List<String> tags;
    private String text;
    private String emotion;
    private List<String> imgs;
}
