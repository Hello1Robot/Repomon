package com.repomon.rocketdan.domain.repomon.service;


import com.repomon.rocketdan.common.utils.SecurityUtils;
import com.repomon.rocketdan.domain.repo.entity.RepomonEntity;
import com.repomon.rocketdan.domain.repo.repository.RepoRepository;
import com.repomon.rocketdan.domain.repo.repository.RepomonRepository;
import com.repomon.rocketdan.domain.repo.service.RepoService;
import com.repomon.rocketdan.domain.repomon.app.BattleLogic;
import com.repomon.rocketdan.domain.repomon.dto.request.BattleLogRequestDto;
import com.repomon.rocketdan.domain.repomon.dto.request.RepomonNicknameRequestDto;
import com.repomon.rocketdan.domain.repomon.dto.request.RepomonStartStatusRequestDto;
import com.repomon.rocketdan.domain.repomon.dto.request.RepomonStatusRequestDto;
import com.repomon.rocketdan.domain.repomon.dto.response.BattleLogListResponseDto;
import com.repomon.rocketdan.domain.repomon.dto.response.BattleLogResponseDto;
import com.repomon.rocketdan.domain.repomon.dto.response.RepomonStatusResponseDto;
import com.repomon.rocketdan.domain.repomon.entity.BattleLogEntity;
import com.repomon.rocketdan.domain.repomon.entity.RepomonStatusEntity;
import com.repomon.rocketdan.domain.repomon.repository.BattleLogRepository;
import com.repomon.rocketdan.domain.repomon.repository.RepomonStatusRepository;
import com.repomon.rocketdan.exception.CustomException;
import com.repomon.rocketdan.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.repomon.rocketdan.exception.ErrorCode.*;


@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RepomonService {

	private final RepomonRepository repomonRepository;
	private final BattleLogRepository battleLogRepository;
	private final RepomonStatusRepository repomonStatusRepository;
	private final RepoService repoService;
	private final RepoRepository repoRepository;


	/**
	 * 해당 레포에 속한 레포몬의 상세 정보 반환
	 *
	 * @param repoId
	 * @return
	 */
	public RepomonStatusResponseDto getRepomonInfo(Long repoId) {
		RepomonStatusEntity repomonStatus = repomonStatusRepository.findById(repoId).orElseThrow(
			() -> new CustomException(NOT_FOUND_REPOSITORY)
		);
		return RepomonStatusResponseDto.fromEntity(repomonStatus);

	}


	/**
	 * 레포몬의 초기 닉네임, 스텟 정보, 레포몬 등록
	 *
	 * @param repomonStartStatusRequestDto
	 */
	public void createRepomonStatus(RepomonStartStatusRequestDto repomonStartStatusRequestDto) {

		// 중복 닉네임이 있는 지 다시 확인
		if (repoService.checkRepomonNickname(repomonStartStatusRequestDto.getRepomonNickname())) {
			throw new CustomException(DUPLICATE_RESOURCE);
		}

		// 스텟이 30을 초과할 경우 에러처리
		if (RepomonStartStatusRequestDto.isValid(repomonStartStatusRequestDto)) {
			throw new CustomException(DATA_BAD_REQUEST);
		}

		RepomonStatusEntity repomon = repomonStatusRepository.findById(
			repomonStartStatusRequestDto.getRepoId()).orElseThrow(
			() -> new CustomException(NOT_FOUND_REPOSITORY)
		);

		RepomonEntity selectedRepomon = repomonRepository.findById(
			repomonStartStatusRequestDto.getRepomonId()).orElseThrow(
			() -> new CustomException(NOT_FOUND_ENTITY)
		);

		repomon.updateNickname(repomonStartStatusRequestDto.getRepomonNickname());
		repomon.setStartStatus(repomonStartStatusRequestDto.getStartAtk(),
			repomonStartStatusRequestDto.getStartDodge(),
			repomonStartStatusRequestDto.getStartDef(),
			repomonStartStatusRequestDto.getStartCritical(),
			repomonStartStatusRequestDto.getStartHit()
		);
		repomon.updateRepomon(selectedRepomon);
		repomonStatusRepository.save(repomon);
	}


	/**
	 * 유저의 Rating을 기준으로 +-100 사이의 유저 검색
	 *
	 * @param repoId
	 * @return
	 */
	public RepomonStatusResponseDto getBattleTarget(Long repoId) {
		RepomonStatusEntity repomonStatus = repomonStatusRepository.findById(repoId).orElseThrow(
			() -> new CustomException(NOT_FOUND_REPOSITORY)
		);
		String repomonOwner = repomonStatus.getRepoOwner();
		Integer userRating = repomonStatus.getRating();
		int index = 1;
		while (index <= 10) {
			Integer startRating = userRating - (index * 100);
			Integer endRating = userRating + (index * 100);
			Optional<RepomonStatusEntity> repomon = repomonStatusRepository.findByRatingBetweenRandom(
				startRating, endRating, repomonOwner);

			if (repomon.isPresent()) {
				return RepomonStatusResponseDto.fromEntity(repomon.get());
			}
			index += 1;

		}

		throw new CustomException(NOT_FOUND_REPOSITORY);

	}


	/**
	 * 전투 결과와 로그를 반환
	 *
	 * @param repoId
	 * @param battleLogRequestDto
	 * @return
	 */
	public BattleLogResponseDto createBattleResult(Long repoId,
		BattleLogRequestDto battleLogRequestDto) {
		RepomonStatusEntity myRepomon = repomonStatusRepository.findById(repoId).orElseThrow(
			() -> new CustomException(NOT_FOUND_REPOSITORY)
		);
		RepomonStatusEntity yourRepomon = repomonStatusRepository.findById(
			battleLogRequestDto.getOpponentRepoId()).orElseThrow(
			() -> new CustomException(NOT_FOUND_REPOSITORY)
		);

		List<HashMap<String, Object>> battleLogList = new ArrayList<>();

		HashMap<String, Float> myStatus = BattleLogic.createStatus(myRepomon);
		HashMap<String, Float> yourStatus = BattleLogic.createStatus(yourRepomon);

		Random random = new Random();
		boolean startPlayer = random.nextBoolean();
		Float myHp = myStatus.get("hp");
		Float yourHp = yourStatus.get("hp");
		Integer mySkillDmg = BattleLogic.skillDamageCalc(myRepomon);
		Integer yourSkillDmg = BattleLogic.skillDamageCalc(yourRepomon);
		int turn = 1;

		//		 종료 조건 : 내가 죽거나 상대가 죽거나 20턴이 경과했을 때
		while (myHp > 0 && yourHp > 0 && turn <= 10) {
			if (startPlayer) {
				// 내 공격차례일 때
				HashMap<String, Object> battleResult = BattleLogic.battle(turn, myRepomon,
					yourRepomon, mySkillDmg);
				yourHp -= (int) battleResult.get("damage");
				battleLogList.add(battleResult);

			} else {
				HashMap<String, Object> battleResult = BattleLogic.battle(turn, yourRepomon,
					myRepomon, yourSkillDmg);
				myHp -= (int) battleResult.get("damage");
				battleLogList.add(battleResult);
			}
			turn++;
			startPlayer = !startPlayer;
		}
		boolean isLose = false;
		if (myHp <= 0 || yourHp <= 0) {
			isLose = myHp <= 0;
		} else if (turn > 10) {
			isLose = myHp / myStatus.get("hp") < yourHp / yourStatus.get("hp");
		}
		Integer point = (!isLose)
			? BattleLogic.getResultPoint(myRepomon, yourRepomon)
			: BattleLogic.getResultPoint(yourRepomon, myRepomon);

		modifyBattleResult(isLose, point, myRepomon.getRepoId());
		modifyBattleResult(!isLose, point, yourRepomon.getRepoId());

		point = (!isLose) ? point : -point;

		BattleLogEntity battleResult = BattleLogEntity.builder()
			.isWin(!isLose)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.attackPoint(point)
			.defensePoint(-point)
			.attackRepo(myRepomon)
			.defenseRepo(yourRepomon)
			.build();

		battleLogRepository.save(battleResult);

		return BattleLogResponseDto.builder()
			.isWin(!isLose)
			.startPlayer(startPlayer)
			.attackPoint(point)
			.defensePoint(-point)
			.attackRepo(RepomonStatusResponseDto.fromEntity(myRepomon))
			.defenseRepo(RepomonStatusResponseDto.fromEntity(yourRepomon))
			.battleLog(battleLogList)
			.build();

	}


	public void modifyBattleResult(Boolean isLose, Integer point, Long repoId) {
		RepomonStatusEntity repomon = repomonStatusRepository.findById(repoId).orElseThrow(
			() -> new CustomException(NOT_FOUND_REPOSITORY)
		);
		if (isLose) {
			repomon.updateLoseCnt();
			repomon.updateRating(-point);
			repomonStatusRepository.save(repomon);
		} else {
			repomon.updateWinCnt();
			repomon.updateRating(point);
			repomonStatusRepository.save(repomon);
		}
	}


	/**
	 * 최근순으로 5개의 전투결과를 조회
	 *
	 * @param repoId
	 * @return
	 */
	public BattleLogListResponseDto getBattleLogList(Long repoId) {
		List<BattleLogEntity> battleLogList = battleLogRepository.findTop10ByRepoIdOrderByDesc(
			repoId);
		if (battleLogList.isEmpty()) {
			throw new CustomException(NOT_FOUND_ENTITY);
		}
		return BattleLogListResponseDto.fromEntity(battleLogList);

	}


	public void modifyRepomonStatus(RepomonStatusRequestDto repomonStatusRequestDto) {
		RepomonStatusEntity repomon = repomonStatusRepository.findById(
			repomonStatusRequestDto.getRepoId()).orElseThrow(
			() -> new CustomException(NOT_FOUND_REPOSITORY)
		);

		// 계산된 남은스탯이 음수가 될 경우 에러처리
		if (RepomonStatusResponseDto.remainStat(
			repomon.getRepoExp(),
			repomon.getAtkPoint() + repomonStatusRequestDto.getAtkPoint(),
			repomon.getDodgePoint() + repomonStatusRequestDto.getDodgePoint(),
			repomon.getDefPoint() + repomonStatusRequestDto.getDefPoint(),
			repomon.getCriticalPoint() + repomonStatusRequestDto.getCriticalPoint(),
			repomon.getHitPoint() + repomonStatusRequestDto.getHitPoint()
		) < 0) {
			throw new CustomException(DATA_BAD_REQUEST);
		}

		repomon.updateStatus(repomonStatusRequestDto.getAtkPoint(),
			repomonStatusRequestDto.getDodgePoint(),
			repomonStatusRequestDto.getDefPoint(),
			repomonStatusRequestDto.getCriticalPoint(),
			repomonStatusRequestDto.getHitPoint());

		repomonStatusRepository.save(repomon);
	}


	/**
	 * 레포몬의 닉네임을 변경
	 *
	 * @param repomonNicknameRequestDto
	 */
	public void modifyRepomonNickname(RepomonNicknameRequestDto repomonNicknameRequestDto) {

		// 권한 검증
		String repoOwner = repoService.getRepoOwnerByRepoId(repomonNicknameRequestDto.getRepoId());
		if (!SecurityUtils.getCurrentUserId().equals(repoOwner)) {
			throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
		}

		// 중복 닉네임이 있는 지 다시 확인
		if (repoService.checkRepomonNickname(repomonNicknameRequestDto.getRepomonNickname())) {
			throw new CustomException(DUPLICATE_RESOURCE);
		}

		RepomonStatusEntity repomon = repomonStatusRepository.findById(
			repomonNicknameRequestDto.getRepoId()).orElseThrow(
			() -> new CustomException(NOT_FOUND_REPOSITORY)
		);
		repomon.updateNickname(repomonNicknameRequestDto.getRepomonNickname());
		repomonStatusRepository.save(repomon);

	}

}
