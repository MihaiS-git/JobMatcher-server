package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.User;

public interface IUserService {

    User getUserByEmail(String email);

}
