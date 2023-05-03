package com.repomon.rocketdan.common.utils;


import com.repomon.rocketdan.common.Retries;
import com.repomon.rocketdan.domain.repo.app.GrowthFactor;
import com.repomon.rocketdan.domain.repo.entity.RepoEntity;
import com.repomon.rocketdan.domain.repo.entity.RepoHistoryEntity;

import java.util.*;
import java.util.stream.Collectors;
import org.kohsuke.github.*;
import org.kohsuke.github.GHRepositoryStatistics.CodeFrequency;
import org.kohsuke.github.GHRepositoryStatistics.CommitActivity;
import org.kohsuke.github.GHRepositoryStatistics.ContributorStats;
import org.kohsuke.github.GHRepositoryStatistics.ContributorStats.Week;
import org.kohsuke.github.GHRepositoryStatistics.Participation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import javax.annotation.PostConstruct;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitComment;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHOrganization.Role;
import org.kohsuke.github.GHPersonSet;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GHUtils {

    @Value("${github.accessToken}")
    private String accessToken;
    private GitHub gitHub;

    @PostConstruct
    private void init() throws IOException {
        gitHub = new GitHubBuilder().withOAuthToken(accessToken).build();
    }

    public Map<String, GHRepository> getRepositoriesWithName(String name){
        try {
            GHUser user = gitHub.getUser(name);
            Map<String, GHRepository> repositories = getRepositoriesInPublicOrganization(user);
            repositories.putAll(getRepositories(user));
            return repositories;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, GHRepository> getRepositories(GHUser user) throws IOException {
        String name = user.getLogin();

        Map<String, GHRepository> repositories = new HashMap<>(user.getRepositories());
        Map<String, GHRepository> repositoriesWithNodeKey = new HashMap<>();

        repositories.forEach((s, ghRepository) -> {
            if(!s.equals(name) && !ghRepository.isFork() && !s.equals(name + ".github.io")){
                repositoriesWithNodeKey.put(ghRepository.getNodeId(), ghRepository);
            }
        });

        return repositoriesWithNodeKey;
    }

    private Map<String, GHRepository> getRepositoriesInPublicOrganization(GHUser user)
        throws IOException {
        GHPersonSet<GHOrganization> organizations = user.getOrganizations();
        if(organizations != null) {
            Map<String, GHRepository> repositoriesWithNodeId = new HashMap<>();
            for(GHOrganization ghOrganization : organizations){
                Map<String, GHRepository> repositories = ghOrganization.getRepositories();
                repositories.forEach((s, ghRepository) -> repositoriesWithNodeId.put(ghRepository.getNodeId(), ghRepository));
            }
            return repositoriesWithNodeId;
        }
        return new HashMap<>();
    }


    public Collection<RepoHistoryEntity> GHCommitToHistory(GHRepository ghRepository, RepoEntity repoEntity, Date date) throws IOException {
        Map<LocalDate, RepoHistoryEntity> histories = new HashMap<>();

        PagedIterable<GHCommit> ghCommits = date == null ?
            ghRepository.queryCommits().list() :
            ghRepository.queryCommits()
                .since(date)
                .list();

        for(GHCommit commit : ghCommits){
            if(commit.getParentSHA1s().size() > 1) continue;
            LocalDate commitDate = commit.getCommitDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            configureRepoInfo(histories, commitDate, repoEntity, GrowthFactor.COMMIT);
        }
        return histories.values();
    }

    public Collection<RepoHistoryEntity> GHPullRequestToHistory(GHRepository ghRepository, RepoEntity repoEntity, Date date)
        throws IOException {
        Map<LocalDate, RepoHistoryEntity> histories = new HashMap<>();

        LocalDate localDate = date == null ? null : date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();

        PagedIterable<GHPullRequest> pullRequests = ghRepository.queryPullRequests().list();
        for(GHPullRequest pr : pullRequests){
            LocalDate prDate = pr.getMergedAt().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            if(localDate == null || prDate.isAfter(localDate)) {
                configureRepoInfo(histories, prDate, repoEntity, GrowthFactor.MERGE);

                List<GHPullRequestReviewComment> reviewComments = pr.listReviewComments()
                    .toList();

                for(GHPullRequestReviewComment reviewComment : reviewComments){
                    LocalDate reviewDate = reviewComment.getCreatedAt().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                    configureRepoInfo(histories, reviewDate, repoEntity, GrowthFactor.REVIEW);
                }
            }
        }

        return histories.values();
    }

    public Collection<RepoHistoryEntity> GHIssueToHistory(GHRepository ghRepository, RepoEntity repoEntity, Date date){
        Map<LocalDate, RepoHistoryEntity> histories = new HashMap<>();

        PagedIterable<GHIssue> issues = date == null ?
            ghRepository.queryIssues().list() :
            ghRepository.queryIssues()
                .since(date)
                .list();

        for(GHIssue issue : issues){
            Date closedAt = issue.getClosedAt();
            if(closedAt != null){
                LocalDate issueCloseDate = closedAt.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
                configureRepoInfo(histories, issueCloseDate, repoEntity, GrowthFactor.ISSUE);
            }
        }

        return histories.values();
    }


    private void configureRepoInfo(Map<LocalDate, RepoHistoryEntity> histories, LocalDate date, RepoEntity repoEntity, GrowthFactor factor) {
        if (histories.containsKey(date)) {
            RepoHistoryEntity repoHistoryEntity = histories.get(date);
            repoHistoryEntity.updateExp(factor.getExp());
        } else {
            histories.put(date, RepoHistoryEntity.ofGHInfo(date, repoEntity, factor));
        }
    }


    public Map<String, String> getUser(String userName) {
        try {
            GHUser githubUser = gitHub.getUser(userName);
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("username", githubUser.getLogin());
            userInfo.put("avatarUrl", githubUser.getAvatarUrl());
            userInfo.put("nickname", githubUser.getName());
            return userInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Retries
    public long getTotalLineCount(GHRepositoryStatistics statistics) throws IOException, InterruptedException {
        long totalLineCount = 0L;
        List<CodeFrequency> codeFrequencies = statistics.getCodeFrequency();
        for (CodeFrequency codeFrequency : codeFrequencies) {
            totalLineCount += codeFrequency.getAdditions();
            totalLineCount += codeFrequency.getDeletions();
        }

        return totalLineCount;
    }

    @Retries
    public Map<String, Integer> getCommitterInfoMap(GHRepositoryStatistics statistics) throws IOException, InterruptedException {
        Map<String, Integer> commitCountMap = new HashMap<>();
        List<ContributorStats> contributorStatList = statistics.getContributorStats().toList();
        for (ContributorStats contributorStats : contributorStatList) {
            String author = contributorStats.getAuthor().getLogin();
            int authorCommitCnt = contributorStats.getTotal();
            commitCountMap.put(author, authorCommitCnt);
        }

        return commitCountMap;
    }


    /**
     * 유저 이름의 총 라인 수
     * @param statistics
     * @param userName
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Retries
    public long getLineCountWithUser(GHRepositoryStatistics statistics, String userName) throws IOException, InterruptedException {
        long lineCount = 0L;
        List<ContributorStats> contributorStatList = statistics.getContributorStats().toList();
        for (ContributorStats contributorStats : contributorStatList) {
            String author = contributorStats.getAuthor().getLogin();
            if(author.equals(userName)){
                for(Week week : contributorStats.getWeeks()){
                    lineCount += week.getNumberOfAdditions();
                    lineCount += week.getNumberOfDeletions();
                }
            }
        }
        return lineCount;
    }

    /**
     * 유저 이름의 총 커밋 수
     * @param statistics
     * @param userName
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Retries
    public int getCommitCountWithUser(GHRepositoryStatistics statistics, String userName)
        throws IOException, InterruptedException {
        int commitCount = 0;
        List<ContributorStats> contributorStatList = statistics.getContributorStats().toList();
        for (ContributorStats contributorStats : contributorStatList) {
            String author = contributorStats.getAuthor().getLogin();
            if (author.equals(userName)) {
                commitCount += contributorStats.getTotal();
            }
        }

        return commitCount;
    }

    /**
     * 유저 이름의 총 이슈 수
     */
    public Integer getMyIssueToHistory(GHRepository ghRepository, Date date, String username) throws IOException {

        List<GHIssue> issues = new ArrayList<>();
        if (date == null) {
            issues = ghRepository.queryIssues().list().toList();
        } else {
            PagedIterable<GHIssue> allIssues = ghRepository.queryIssues().list();
            for (GHIssue issue : allIssues) {
                if (issue.getCreatedAt().after(date)) {
                    issues.add(issue);
                }
            }
        }

        int issueCount = 0;
        for(GHIssue issue : issues){
            if (issue.getUser().getName().equals(username)){
                issueCount += 1;
            }
        }
        return issueCount;
    }

    /**
     * 유저 이름의 총 머지, 리뷰 수
     */
    public List<Integer> getMyMergeToHistory(GHRepository ghRepository, Date date, String username) throws IOException {

        LocalDate localDate = date == null ? null : date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        int mymerge = 0;
        int myreview = 0;

        PagedIterable<GHPullRequest> pullRequests = ghRepository.queryPullRequests().list();
        for(GHPullRequest pr : pullRequests){
            LocalDate prDate = pr.getMergedAt().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            if(localDate != null && prDate.isBefore(localDate)) {
                if (pr.getUser().getName().equals(username)){
                    mymerge += 1;
                }

                List<GHPullRequestReviewComment> reviewComments = pr.listReviewComments()
                        .toList();

                for(GHPullRequestReviewComment reviewComment : reviewComments){
                    if (reviewComment.getUser().getName().equals(username)){
                        myreview += 1;
                    }
                }
            }
        }
        List<Integer> response = new ArrayList<>();
        response.add(mymerge);
        response.add(myreview);

        return response;
    }

}