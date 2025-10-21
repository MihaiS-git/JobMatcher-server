package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ProposalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class ProposalStatusRequestDTO {

    @NotNull
    private ProposalStatus status;

}
