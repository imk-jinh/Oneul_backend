package com.oneul.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class UserDataResponse {
    private String name;
    private String img;
    private int diaryCount;
    private int poetryCount;
    private int chatCount;

    @Override
    public String toString() {
        return "DiaryDetailResponse{" +
                "name='" + name + '\'' +
                ", img=" + img +
                ", diaryCount=" + diaryCount +
                ", poetryCount='" + poetryCount + '\'' +
                ", chatCount='" + chatCount +
                '}';
    }
}
