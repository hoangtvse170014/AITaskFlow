package com.taskflow.service;

import com.taskflow.dto.response.GoogleUserInfo;

public interface GoogleService {
    GoogleUserInfo verifyIdToken(String idToken, String clientId);

    GoogleUserInfo fetchUserInfo(String accessToken);
}
