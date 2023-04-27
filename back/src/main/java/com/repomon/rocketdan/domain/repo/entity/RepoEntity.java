package com.repomon.rocketdan.domain.repo.entity;


import com.repomon.rocketdan.common.entity.CommonEntity;
import java.io.IOException;
import java.time.ZoneId;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.SuperBuilder;
import org.kohsuke.github.GHRepository;


@Entity
@Getter
@SuperBuilder
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "repo")
public class RepoEntity extends CommonEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "repo_id")
	private Long repoId;

	private String repoName;
	private String repoOwner;
	private String repomonNickname;
	private Long repoExp;
	private Integer repomonTier;
	private String repoKey;
	private LocalDateTime repoStart;
	private LocalDateTime repoEnd;
	private Integer rating;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="repomon_id")
	private RepomonEntity repomon;

	@OneToMany(mappedBy = "repo")
	private List<RepoConventionEntity> repoConventionList = new ArrayList<>();
	public static RepoEntity fromGHRepository(GHRepository ghRepository) {
		LocalDateTime repoCreatedAt = null;

		try {
			repoCreatedAt = ghRepository.getCreatedAt().toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		LocalDateTime now = LocalDateTime.now();

		return RepoEntity.builder()
			.repoName(ghRepository.getName())
			.repoOwner(ghRepository.getOwnerName())
			.repomonNickname(null)
			.repoExp(0L)
			.repomonTier(0)
			.repoKey(ghRepository.getNodeId())
			.repoStart(repoCreatedAt)
			.repoEnd(null)
			.createdAt(now)
			.updatedAt(now)
			.rating(1000)
			.build();
	}

	public void updateNickname(String nickname) {
		this.repomonNickname = nickname;
	}


	public void updateRating(int score) {
		this.rating += score;
	}



	public void updateRepomon(RepomonEntity repomon) {
		this.repomon = repomon;
	}


	public void update(GHRepository ghRepository) {
		this.repoName = ghRepository.getName();
	}

}