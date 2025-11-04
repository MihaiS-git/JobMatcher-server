package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class JobFeedProjectFilterDTO {
    Long categoryId;
    Long subcategoryId;
    String searchTerm;
}
