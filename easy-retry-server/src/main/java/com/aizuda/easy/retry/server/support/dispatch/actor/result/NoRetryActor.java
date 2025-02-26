package com.aizuda.easy.retry.server.support.dispatch.actor.result;

import akka.actor.AbstractActor;
import cn.hutool.core.lang.Assert;
import com.aizuda.easy.retry.common.core.log.LogUtils;
import com.aizuda.easy.retry.server.exception.EasyRetryServerException;
import com.aizuda.easy.retry.server.support.RetryContext;
import com.aizuda.easy.retry.server.support.WaitStrategy;
import com.aizuda.easy.retry.server.support.retry.RetryExecutor;
import com.aizuda.easy.retry.template.datasource.access.AccessTemplate;
import com.aizuda.easy.retry.template.datasource.persistence.po.RetryTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 不重试,只更新下次触发时间
 *
 * @author: www.byteblogs.com
 * @date : 2022-04-14 16:11
 * @since 1.0.0
 */
@Component(NoRetryActor.BEAN_NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class NoRetryActor extends AbstractActor {

    public static final String BEAN_NAME = "NoRetryActor";

    @Autowired
    protected AccessTemplate accessTemplate;

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(RetryExecutor.class, retryExecutor -> {

            RetryContext retryContext = retryExecutor.getRetryContext();
            RetryTask retryTask = retryContext.getRetryTask();
            WaitStrategy waitStrategy = retryContext.getWaitStrategy();
            retryTask.setNextTriggerAt(waitStrategy.computeRetryTime(retryContext));

            // 不更新重试次数
            retryTask.setRetryCount(null);
            try {
                retryTask.setUpdateDt(LocalDateTime.now());
                Assert.isTrue(1 == accessTemplate.getRetryTaskAccess()
                        .updateById(retryTask.getGroupName(),retryTask), () ->
                    new EasyRetryServerException("更新重试任务失败. groupName:[{}] uniqueId:[{}]",
                        retryTask.getGroupName(),  retryTask.getUniqueId()));
            }catch (Exception e) {
                LogUtils.error(log,"更新重试任务失败", e);
            } finally {
                // 更新DB状态
                getContext().stop(getSelf());
            }

        }).build();
    }

}
