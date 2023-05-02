package com.repomon.rocketdan.domain.repomon.dto.response;

import com.repomon.rocketdan.common.utils.S3Utils;
import com.repomon.rocketdan.domain.repo.entity.RepomonEntity;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RepomonUrlResponseDto {
    private List<String> repomonUrls;

    public static RepomonUrlResponseDto fromEntities(List<RepomonEntity> exceptEgg) {
        List<String> urls = exceptEgg.stream()
            .map(repomon -> S3Utils.getS3Url(repomon.getRepomonUrl()))
            .collect(Collectors.toList());
        return new RepomonUrlResponseDto(urls);
    }
}
