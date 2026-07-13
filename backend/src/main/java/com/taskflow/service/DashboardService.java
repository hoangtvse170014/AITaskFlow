package com.taskflow.service;

import com.taskflow.dto.response.DashboardResponse;

import java.util.UUID;

public interface DashboardService {

    DashboardResponse getDashboardStats(UUID workspaceId);
}
