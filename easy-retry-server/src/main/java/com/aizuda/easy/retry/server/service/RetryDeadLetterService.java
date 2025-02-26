package com.aizuda.easy.retry.server.service;

import com.aizuda.easy.retry.server.web.model.base.PageResult;
import com.aizuda.easy.retry.server.web.model.request.BatchDeleteRetryDeadLetterVO;
import com.aizuda.easy.retry.server.web.model.request.BatchRollBackRetryDeadLetterVO;
import com.aizuda.easy.retry.server.web.model.request.RetryDeadLetterQueryVO;
import com.aizuda.easy.retry.server.web.model.response.RetryDeadLetterResponseVO;

import java.util.List;

/**
 * @author: www.byteblogs.com
 * @date : 2022-02-28 09:34
 */
public interface RetryDeadLetterService {

    PageResult<List<RetryDeadLetterResponseVO>> getRetryDeadLetterPage(RetryDeadLetterQueryVO queryVO);

    RetryDeadLetterResponseVO getRetryDeadLetterById(String groupName, Long id);

    int rollback(BatchRollBackRetryDeadLetterVO rollBackRetryDeadLetterVO);

    int batchDelete(BatchDeleteRetryDeadLetterVO deadLetterVO);
}
