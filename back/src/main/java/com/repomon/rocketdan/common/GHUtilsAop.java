package com.repomon.rocketdan.common;

import com.repomon.rocketdan.common.utils.GHUtils;
import com.repomon.rocketdan.domain.repo.repository.RepoRepository;
import com.repomon.rocketdan.domain.user.entity.UserEntity;
import com.repomon.rocketdan.domain.user.repository.UserRepository;
import com.repomon.rocketdan.exception.CustomException;
import com.repomon.rocketdan.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRateLimit.Record;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.stereotype.Component;

@Aspect @Slf4j
@Component
@RequiredArgsConstructor
public class GHUtilsAop {
    private Set<Long> usingUsers = new HashSet<>();
    private Set<Long> usingKeys = new HashSet<>();
    private final UserRepository userRepository;
    private final RepoRepository repoRepository;
    private final GHUtils ghUtils;

    @Around("execution(* com.repomon.rocketdan.common.utils.GHUtils.*(..))")
    public Object aroundRetriesAnno(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{
        long start = System.currentTimeMillis();
        String methodName = proceedingJoinPoint.getSignature().getName();
        try {
            GHUtils ghUtils = (GHUtils) proceedingJoinPoint.getTarget();
            GitHub gitHub = ghUtils.gitHub;

            GHRateLimit rateLimit = gitHub.getRateLimit();
            Record core = rateLimit.getCore();

            log.info("remaining 개수 => {}", core.getRemaining());
            if (core.getRemaining() == 0) {
                LocalDateTime resetDate = LocalDateTime.ofInstant(core.getResetDate().toInstant(),
                    ZoneId.systemDefault());
                Duration between = Duration.between(LocalDateTime.now(), resetDate);
                long seconds = between.getSeconds();
                Thread.sleep(seconds);
            }

            return proceedingJoinPoint.proceed();
        }finally{
            long finish = System.currentTimeMillis();
            long timeMs = finish - start;
            log.info("END: " + methodName + " " + timeMs+"ms");
        }
    }

    @Around("execution(* com.repomon.rocketdan.domain.repo.service.RepoService.modifyAllRepo(..))))")
    public Object useRepoIoInSearchAllRepo(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{
        log.info("사용중인 modifyAllRepo -> repoId 체크시작!!");
        List<Object> params = Arrays.asList((proceedingJoinPoint.getArgs()));
        log.info("Params => {}", params);

        Long userId = (Long) params.get(0);

        if(usingUsers.contains(userId)){
            log.warn("이미 탐색중인 유저입니다.");
            throw new CustomException(ErrorCode.NO_ACCESS);
        }

        usingUsers.add(userId);

        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> {
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        });

        Map<String, GHRepository> repositories = ghUtils.getRepositoriesWithName(
            userEntity.getUserName());

        Set<Long> repoIds = new HashSet<>();
        repositories.keySet().forEach(repoKey -> {
            repoRepository.findByRepoKey(repoKey).ifPresent(repoEntity -> {
                Long repoId = repoEntity.getRepoId();
                if(usingKeys.contains(repoId)){
                    log.warn("이미 사용중인 repoId 입니다.");
                    throw new CustomException(ErrorCode.NO_ACCESS);
                }

                repoIds.add(repoId);
            });
        });

        try {
            usingKeys.addAll(repoIds);

            return proceedingJoinPoint.proceed();
        }finally {
            usingKeys.removeAll(repoIds);
            usingUsers.remove(userId);
            log.info("사용중인  modifyAllRepo -> repoId 체크 끝!!");
        }
    }

    @Around("execution(* com.repomon.rocketdan.domain.repo.service.RepoService.modifyRepoInfo(..)) || "
        + "execution(* com.repomon.rocketdan.domain.repo.service.RepoService.RepoCardDetail(..)) || "
        + "execution(* com.repomon.rocketdan.domain.repo.service.RepoService.RepoPersonalCardDetail(..))")
    public Object useRepoIoInSearch(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        log.info("사용중인 repoId 체크시작!!");
        List<Object> params = Arrays.asList((proceedingJoinPoint.getArgs()));
        log.info("Params => {}", params);

        Long repoId = (Long) params.get(0);

        if(usingKeys.contains(repoId)){
            log.warn("이미 사용중인 repoId 입니다.");
            throw new CustomException(ErrorCode.NO_ACCESS);
        }

        try {
            usingKeys.add(repoId);
            return proceedingJoinPoint.proceed();
        }finally {
            usingKeys.remove(repoId);
            log.info("사용중인 repoId 체크 끝!!");
        }
    }
}
