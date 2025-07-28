package com.tianji.learning.service;

import com.tianji.learning.domain.po.SignRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.SignRecordVO;
import com.tianji.learning.domain.vo.SignResultVO;

/**
 * <p>
 * 签到记录表 服务类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-28
 */
public interface ISignRecordService extends IService<SignRecord> {

    SignResultVO addSignRecords();
}
