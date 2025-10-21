package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.MilestoneStatus;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class MilestoneStatusRequestDTO {

    private MilestoneStatus status;

}
