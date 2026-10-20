package com.xuecheng.content.model.dto;

public enum TeachplanGrade {
    GRADE1(1), GRADE2(2);

    private Integer grade;

    TeachplanGrade(Integer grade) {
        this.grade = grade;
    }

    public Integer getGrade() {
        return grade;
    }

}
