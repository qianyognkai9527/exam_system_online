package com.joker.ai.exam.service.impl;

import com.joker.ai.exam.entity.User;
import com.joker.ai.exam.mapper.UsersMapper;
import com.joker.ai.exam.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author joker
 * @since 2026-04-09
 */
@Service
public class UserServiceImpl extends ServiceImpl<UsersMapper, User> implements UserService {

}
