package com.jobmatcher.server.domain;

public enum ProposalStatus {
    PENDING,   // Awaiting client decision
    ACCEPTED,  // Client accepted proposal -> triggers contract creation
    REJECTED,  // Client declined
    WITHDRAWN; // Freelancer withdrew
}
