package com.shf.service.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shf.common.constants.Constants;
import com.shf.common.model.wechat.WechatExceptions;
import com.shf.service.dao.WechatExceptionsDao;
import com.shf.service.service.WechatExceptionsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 *  微信异常服务实现类
 */
@Service
public class WechatExceptionsServiceImpl extends ServiceImpl<WechatExceptionsDao, WechatExceptions> implements WechatExceptionsService {

    @Resource
    private WechatExceptionsDao dao;

    /**
     * 删除历史日志
     */
    @Override
    public void autoDeleteLog() {
        String beforeDate = DateUtil.offsetDay(new Date(), -9).toString(Constants.DATE_FORMAT_DATE);
        UpdateWrapper<WechatExceptions> wrapper = Wrappers.update();
        wrapper.lt("create_time", beforeDate);
        dao.delete(wrapper);
    }
}

