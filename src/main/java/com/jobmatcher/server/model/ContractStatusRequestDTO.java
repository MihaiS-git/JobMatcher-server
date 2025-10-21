package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ContractStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractStatusRequestDTO {

    private ContractStatus status;

}
