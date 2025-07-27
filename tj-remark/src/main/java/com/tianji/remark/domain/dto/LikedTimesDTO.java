package com.tianji.remark.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikedTimesDTO {
    /**
     * 点赞的业务id
     */
    private Long bizId;
    /**
     * 总的点赞次数
     */
    private Integer likedTimes;

    public static LikedTimesDTO of(Long bizId, Integer likedTimes) {
        LikedTimesDTO dto = new LikedTimesDTO();
        dto.setBizId(bizId);
        dto.setLikedTimes(likedTimes);
        return dto;
    }
}
