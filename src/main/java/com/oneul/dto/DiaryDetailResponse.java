package com.oneul.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class DiaryDetailResponse {
    private String title;
    private Date date;
    private List<String> tags;
    private String emotion;
    private String text;
    private List<String> images;

    @Override
    public String toString() {
        return "DiaryDetailResponse{" +
                "title='" + title + '\'' +
                ", date=" + date +
                ", tags=" + tags +
                ", emotion='" + emotion + '\'' +
                ", text='" + text + '\'' +
                ", images=" + images +
                '}';
    }
}
