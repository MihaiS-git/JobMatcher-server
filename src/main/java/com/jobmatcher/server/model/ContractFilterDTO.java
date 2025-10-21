package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ContractStatus;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ContractFilterDTO {
    private String customerName;
    private String freelancerName;
    private ContractStatus status;
    private String searchTerm;
}
