package com.medicine.sales.service;

import com.medicine.sales.entity.FeedbackInfo;

public interface FeedbackService {
    void submit(FeedbackInfo feedback, Long purchaserId);
}
