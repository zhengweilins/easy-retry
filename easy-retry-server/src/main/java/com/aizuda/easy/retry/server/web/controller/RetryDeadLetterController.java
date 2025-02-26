package com.aizuda.easy.retry.server.web.controller;

import com.aizuda.easy.retry.server.service.RetryDeadLetterService;
import com.aizuda.easy.retry.server.web.model.base.PageResult;
import com.aizuda.easy.retry.server.web.model.request.BatchDeleteRetryDeadLetterVO;
import com.aizuda.easy.retry.server.web.model.request.BatchRollBackRetryDeadLetterVO;
import com.aizuda.easy.retry.server.web.model.request.RetryDeadLetterQueryVO;
import com.aizuda.easy.retry.server.web.annotation.LoginRequired;
import com.aizuda.easy.retry.server.web.model.response.RetryDeadLetterResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 死信队列接口
 *
 * @author: www.byteblogs.com
 * @date : 2022-02-28 15:38
 */
@RestController
@RequestMapping("/retry-dead-letter")
public class RetryDeadLetterController {

    @Autowired
    private RetryDeadLetterService retryDeadLetterService;

    @LoginRequired
    @GetMapping("list")
    public PageResult<List<RetryDeadLetterResponseVO>> getRetryDeadLetterPage(RetryDeadLetterQueryVO queryVO) {
        return retryDeadLetterService.getRetryDeadLetterPage(queryVO);
    }

    @LoginRequired
    @GetMapping("{id}")
    public RetryDeadLetterResponseVO getRetryDeadLetterById(@RequestParam("groupName") String groupName,
                                                            @PathVariable("id") Long id) {
        return retryDeadLetterService.getRetryDeadLetterById(groupName, id);
    }

    @LoginRequired
    @PostMapping("/batch/rollback")
    public int rollback(@RequestBody @Validated BatchRollBackRetryDeadLetterVO rollBackRetryDeadLetterVO) {
        return retryDeadLetterService.rollback(rollBackRetryDeadLetterVO);
    }

    @LoginRequired
    @DeleteMapping("/batch")
    public int batchDelete(@RequestBody @Validated BatchDeleteRetryDeadLetterVO deadLetterVO) {
        return retryDeadLetterService.batchDelete(deadLetterVO);
    }
}
