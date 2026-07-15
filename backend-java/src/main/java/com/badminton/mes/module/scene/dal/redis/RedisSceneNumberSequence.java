package com.badminton.mes.module.scene.dal.redis;

import java.time.*;
import java.time.format.DateTimeFormatter;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis 日流水现场单号生成器。 @author 刘涵 */
@Component
public class RedisSceneNumberSequence implements SceneNumberSequence {
    private static final DateTimeFormatter DATE=DateTimeFormatter.BASIC_ISO_DATE;
    private final StringRedisTemplate redisTemplate;
    public RedisSceneNumberSequence(StringRedisTemplate redisTemplate){this.redisTemplate=redisTemplate;}
    @Override public String nextTaskNo(){return next("task","PT");}
    @Override public String nextDispatchNo(){return next("dispatch","PD");}
    private String next(String type,String prefix){
        String date=LocalDate.now().format(DATE);String key="mes:scene:"+type+":sequence:"+date;
        Long value=redisTemplate.opsForValue().increment(key);
        if(value==null) throw new ServiceException(SceneErrorCodeConstants.NUMBER_GENERATE_FAILED);
        if(value==1L) redisTemplate.expire(key,Duration.ofDays(2));
        return prefix+date+String.format("%06d",value);
    }
}
