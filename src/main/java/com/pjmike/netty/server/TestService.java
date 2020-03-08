package com.pjmike.netty.server;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author jack-cooper
 * @version 1.0.0
 * @ClassName TestService.java
 * @Description TODO
 * @createTime 2020年03月08日 16:36:00
 */
@Service
public class TestService {

    public String doSomthing(String msg){
        return msg + "***********";
    }
}
