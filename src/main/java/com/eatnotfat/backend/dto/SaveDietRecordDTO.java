package com.eatnotfat.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class SaveDietRecordDTO {

    private Long userId;

    private Integer mealType;

    private String remark;

    private List<DietRecordItemDTO> items;

    @Data
    public static class DietRecordItemDTO {
        private Integer foodType;
        private Long foodId;
        private String foodName;
        private Double eatWeight;
        private Double calorie;
        private Double carbs;
        private Double protein;
        private Double fat;
    }
}