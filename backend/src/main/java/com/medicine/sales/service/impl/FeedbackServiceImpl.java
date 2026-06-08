package com.medicine.sales.service.impl;

import com.medicine.sales.entity.FeedbackInfo;
import com.medicine.sales.exception.BusinessException;
import com.medicine.sales.mapper.FeedbackMapper;
import com.medicine.sales.service.FeedbackService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Resource
    private FeedbackMapper feedbackMapper;

    @Override
    public void submit(FeedbackInfo feedback, Long purchaserId) {
        if (feedback.getTitle() == null || feedback.getTitle().trim().isEmpty()) {
            throw new BusinessException("请输入反馈标题");
        }
        if (feedback.getContent() == null || feedback.getContent().trim().isEmpty()) {
            throw new BusinessException("请输入反馈内容");
        }
        feedback.setPurchaserId(purchaserId);
        if (feedback.getType() == null) feedback.setType(1);
        if (feedback.getStatus() == null) feedback.setStatus(0);
        feedbackMapper.insert(feedback);
    }
}
