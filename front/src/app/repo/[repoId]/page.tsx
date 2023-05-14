"use client";

import React, { useState, useEffect } from "react";
import { StarIcon, ShareIcon } from "@heroicons/react/24/outline";
import { ArrowPathIcon } from "@heroicons/react/24/solid";
import DetailRepomon from "@/components/Detail/DetailRepomon";
import ProgressBar from "@/components/Detail/ProgressBar";
import styles from "./page.module.scss";
import {
  PresentationChartLineIcon,
  ClipboardDocumentListIcon,
  ChartPieIcon,
} from "@heroicons/react/24/outline";
import DetailAnalysis from "@/components/Detail/DetailAnalysis";
import DetailBattle from "@/components/Detail/DetailBattle";
import {
  BattleRecordType,
  RepoDetailConventionInfoType,
  RepoDetailBattleType,
  RepoDetailResearchType,
  RepoDetailType,
  RepoDetailContributionInfoType,
} from "@/types/repoDetail";
import {
  axiosRequestBattleRanking,
  axiosRequestBattleRecord,
  axiosRequestRepoDetail,
  axiosRequestRepoDetailBattleInfo,
  axiosRequestRepoDetailContribution,
  axiosRequestRepoDetailConvention,
  axiosRequestRepoDetailUpdate,
} from "@/api/repoDetail";
import { axiosRequestRepoDetailResearch } from "@/api/repoDetail";
import { pretreatDate } from "@/app/utils/PretreatDate";
import DatePickerModal from "@/components/Detail/DatePickerModal";
import Modal from "react-modal";
import RenameModal from "@/components/Detail/RenameModal";
import Loading from "@/app/loading";
import DetailConvention from "@/components/Detail/DetailConvention";
import DetailContribution from "@/components/Detail/DetailContribution";
import { languageColor } from "@/styles/colors";
import { customAlert } from "@/app/utils/CustomAlert";
import LoadingSpinner from "@/components/Skeletons/LoadingSpinner";
import ExportModal from "@/components/Detail/ExportModal";

function Page({ params }: { params: { repoId: string } }) {
  const loginUserId: string | null =
    sessionStorage && sessionStorage.getItem("userId");
  const [repoDetailInfo, setRepoDetailInfo] = useState<RepoDetailType>();
  const [tabIndex, setTabIndex] = useState<number>(0);
  const [isUpdated, setIsUpdated] = useState<boolean>(false);
  const [repoDetailResearchInfo, setRepoDetailResearchInfo] =
    useState<RepoDetailResearchType>();
  const [repoDetailBattleInfo, setRepoDetailBattleInfo] =
    useState<RepoDetailBattleType>();
  const [battleRank, setBattleRank] = useState<number>();
  const [battleRecordInfo, setBattleRecordInfo] =
    useState<BattleRecordType[]>();
  const [showPage, setShowPage] = useState<boolean>(false);
  const [statUpdated, setStatUpdated] = useState<boolean>(false);
  const [conventionUpdated, setConventionUpdated] = useState<boolean>(false);
  const [repoDetailConventionInfo, setRepoDetailConventionInfo] =
    useState<RepoDetailConventionInfoType>();
  const [repoDetailContributionInfo, setRepoDetailContributionInfo] =
    useState<RepoDetailContributionInfoType>();
  const [updateLoading, setUpdateLoading] = useState<boolean>(false);
  const [isTeam, setIsTeam] = useState<boolean>(false);
  const [conventionLoading, setConventionLoading] = useState<boolean>(false);

  /** =============================================== useEffect =============================================== */

  /** 레포 기본 정보 불러오기 + 레포몬 닉네임, 기간 업데이트 시 정보 재요청 */
  useEffect(() => {
    requestRepoDetail(
      parseInt(params.repoId, 10),
      parseInt(loginUserId as string, 10)
    );
  }, [isUpdated]);

  /** 배틀 정보 불러오기 + 스탯 변경 시 재요청 */
  useEffect(() => {
    requestRepoDetailBattleInfo(parseInt(params.repoId, 10));
  }, [statUpdated]);

  /** 컨벤션 정보 불러오기 + 컨벤션 수정 시 재요청 */
  useEffect(() => {
    requestRepoDetailConvention(parseInt(params.repoId, 10));
  }, [conventionUpdated]);

  /** 레포 정보 불러오기 */
  useEffect(() => {
    requestRepoDetailResearch(parseInt(params.repoId, 10));
    requestBattleRanking(parseInt(params.repoId, 10));
    requestBattleRecord(parseInt(params.repoId, 10));
    requestRepoDetailContribution(parseInt(params.repoId, 10));
  }, []);

  useEffect(() => {
    if (
      repoDetailInfo &&
      repoDetailResearchInfo &&
      repoDetailBattleInfo &&
      battleRank &&
      battleRecordInfo &&
      repoDetailConventionInfo &&
      repoDetailContributionInfo
    ) {
      setShowPage(true);
      if (
        repoDetailContributionInfo.committers &&
        Object.keys(repoDetailContributionInfo.committers).length > 1
      ) {
        setIsTeam(true);
      }
    }
  }, [
    repoDetailInfo,
    repoDetailResearchInfo,
    repoDetailBattleInfo,
    battleRank,
    battleRecordInfo,
    repoDetailConventionInfo,
    repoDetailContributionInfo,
  ]);

  useEffect(() => {
    if (showPage) {
      Modal.setAppElement("#repo-detail");
    }
  }, [showPage]);

  /** 탭 인덱스 정보 */
  useEffect(() => {
    if (document.referrer !== window.location.href) {
      setTabIndex(1);
      sessionStorage.setItem("tabIndex", "1");
    } else {
      setTabIndex(parseInt(sessionStorage.getItem("tabIndex") as string, 10));
    }
  }, []);

  /** =============================================== Event Hadler =============================================== */
  const onClickTabBtn = (e: React.MouseEvent) => {
    const target = e.target as HTMLElement;

    console.log(e.target);

    setTabIndex(parseInt(target.id, 10));
    sessionStorage.setItem("tabIndex", target.id);
  };

  const onClickUpdateBtn = () => {
    requestRepoDetailUpdate(parseInt(params.repoId, 10));
  };

  /** =============================================== Axios =============================================== */
  /** 레포 디테일 기본 정보 */
  const requestRepoDetail = async (repoId: number, userId: number) => {
    try {
      const res = await axiosRequestRepoDetail(repoId, userId);
      console.log("레포 디테일 기본: ", res);
      setRepoDetailInfo(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  /** 레포 디테일 분석 정보 */
  const requestRepoDetailResearch = async (repoId: number) => {
    try {
      const res = await axiosRequestRepoDetailResearch(repoId);
      console.log("레포 디테일 분석: ", res);
      setRepoDetailResearchInfo(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  /** 레포 디테일 배틀 정보 */
  const requestRepoDetailBattleInfo = async (repoId: number) => {
    try {
      const res = await axiosRequestRepoDetailBattleInfo(repoId);
      console.log("레포 디테일 배틀: ", res);
      setRepoDetailBattleInfo(res.data.data);
    } catch (err) {
      console.error(err);
    }
  };

  /** 레포 디테일 배틀 랭킹 정보 */
  const requestBattleRanking = async (repoId: number) => {
    try {
      const res = await axiosRequestBattleRanking(repoId);
      console.log("레포 디테일 배틀 랭킹: ", res.data.rank);
      setBattleRank(res.data.rank);
    } catch (err) {
      console.error(err);
    }
  };

  /** 레포 디테일 배틀 전적 정보 */
  const requestBattleRecord = async (repoId: number) => {
    try {
      const res = await axiosRequestBattleRecord(repoId);
      console.log("레포 디테일 배틀 전적: ", res.data.data.battleLogList);
      setBattleRecordInfo(res.data.data.battleLogList);
    } catch (err: any) {
      console.error(err);
      if (err.response.data.status === 404) {
        setBattleRecordInfo([]);
      }
    }
  };

  /** 레포 디테일 컨벤션 정보 */
  const requestRepoDetailConvention = async (repoId: number) => {
    setConventionLoading(true);
    try {
      const res = await axiosRequestRepoDetailConvention(repoId);
      console.log("레포 디테일 컨벤션: ", res);
      setRepoDetailConventionInfo(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setConventionLoading(false);
    }
  };

  /** 레포 디테일 기여도 정보 */
  const requestRepoDetailContribution = async (repoId: number) => {
    try {
      const res = await axiosRequestRepoDetailContribution(repoId);
      console.log("레포 디테일 기여도: ", res);
      setRepoDetailContributionInfo(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  /** 레포 갱신 */
  const requestRepoDetailUpdate = async (repoId: number) => {
    try {
      setUpdateLoading(true);
      const res = await axiosRequestRepoDetailUpdate(repoId);
      console.log("레포 정보 갱신: ", res);
      setIsUpdated(!isUpdated);
    } catch (err) {
      console.error(err);
      customAlert("잠시후 다시 시도해주세요.");
    } finally {
      setUpdateLoading(false);
    }
  };

  return (
    <div id="repo-detail">
      {!showPage && <Loading />}
      {repoDetailInfo && showPage && (
        <div className={styles.pageContainer}>
          <div className={styles.info}>
            <div className={styles["repo-mon-card-div"]}>
              <div className={styles["repo-mon-card"]}>
                <DetailRepomon
                  repomonUrl={repoDetailInfo.repomonUrl}
                  repomonTier={
                    repoDetailBattleInfo?.repomon.repomonTier as number
                  }
                />
                <div className={styles["repo-mon-info"]}>
                  <p>
                    <span className={styles.lv}>
                      LV{" "}
                      <span style={{ fontStyle: "normal" }}>
                        {Math.floor(repoDetailInfo.repoExp / 100) + 1}
                      </span>
                    </span>
                    <p className={styles["repo-mon-name"]}>
                      {repoDetailInfo.repomonName}
                      {repoDetailInfo.myRepo && (
                        <RenameModal
                          repoId={params.repoId}
                          setIsUpdated={setIsUpdated}
                        />
                      )}
                    </p>
                  </p>
                  <ProgressBar restExp={repoDetailInfo.repoExp % 100} />
                </div>
              </div>
            </div>
            <div className={styles["default-info-div"]}>
              <div className={styles.first}>
                <p className={styles.title}>
                  {repoDetailInfo.repoName}
                  <ArrowPathIcon />
                </p>
                <div className={styles["icon-div"]}>
                  <span className={styles.star}>
                    <StarIcon />
                    {repoDetailInfo.starCnt}
                  </span>
                  <span className={styles.share}>
                    <ShareIcon />
                    {repoDetailInfo.forkCnt}
                  </span>
                </div>
              </div>
              <div className={styles["btn-div"]}>
                <button onClick={onClickUpdateBtn} className={styles.update}>
                  {updateLoading ? (
                    <LoadingSpinner ml={4} mr={4} size={6} />
                  ) : (
                    <span>레포지토리 갱신</span>
                  )}
                </button>
                {loginUserId && repoDetailInfo.myRepo && (
                  <ExportModal
                    repoId={parseInt(params.repoId, 10)}
                    userId={parseInt(loginUserId, 10)}
                    isTeam={isTeam}
                    lans={repoDetailInfo.languages}
                  />
                )}
              </div>
              <p className={styles.date}>
                <span>{pretreatDate(repoDetailInfo.repoStart)}</span>
                <span style={{ margin: "0 0.5rem" }}>~</span>
                {repoDetailInfo.repoEnd && (
                  <span> {pretreatDate(repoDetailInfo.repoEnd)}</span>
                )}
                {!repoDetailInfo.repoEnd && repoDetailInfo.myRepo && (
                  <span className={styles.end}>
                    프로젝트 기간을 설정해주세요.
                  </span>
                )}
                {repoDetailInfo.myRepo && (
                  <DatePickerModal
                    repoId={params.repoId}
                    setIsUpdated={setIsUpdated}
                  />
                )}
              </p>
              <div className={styles["lan-div"]}>
                {repoDetailInfo.languages &&
                  repoDetailInfo.languages.slice(0, 7).map((lan, index) => (
                    <span
                      key={index}
                      style={{
                        backgroundColor: languageColor[lan].color
                          ? (languageColor[lan].color as string)
                          : "gray",
                      }}
                    >
                      {lan}
                    </span>
                  ))}
                {repoDetailInfo.languages &&
                  repoDetailInfo.languages.length > 7 && <span>...</span>}
              </div>
              <p className={styles.des}>
                {repoDetailInfo.repoDescription ? (
                  repoDetailInfo.repoDescription
                ) : (
                  <span style={{ color: "gray" }}>프로젝트 설명이 없어요.</span>
                )}
              </p>
              <div className={styles.tab}>
                <button
                  id="1"
                  onClick={onClickTabBtn}
                  className={tabIndex === 1 ? styles.selected : ""}
                >
                  <PresentationChartLineIcon id="1" onClick={onClickTabBtn} />
                  성장
                </button>
                <button
                  id="2"
                  onClick={onClickTabBtn}
                  className={tabIndex === 2 ? styles.selected : ""}
                >
                  <p id="2" className={styles.vs} onClick={onClickTabBtn}>
                    VS
                  </p>
                  배틀
                </button>
                <button
                  id="3"
                  onClick={onClickTabBtn}
                  className={tabIndex === 3 ? styles.selected : ""}
                >
                  <ClipboardDocumentListIcon id="3" onClick={onClickTabBtn} />
                  컨벤션
                </button>
                <button
                  id="4"
                  onClick={onClickTabBtn}
                  className={tabIndex === 4 ? styles.selected : ""}
                >
                  <ChartPieIcon id="4" onClick={onClickTabBtn} />
                  기여도
                </button>
              </div>
            </div>
          </div>
          <div className={styles["tab-content"]}>
            {tabIndex === 1 && (
              <DetailAnalysis researchInfo={repoDetailResearchInfo} />
            )}
            {tabIndex === 2 && (
              <DetailBattle
                battleInfo={repoDetailBattleInfo!}
                rank={battleRank!}
                battleRecords={battleRecordInfo!}
                myRepomonNickname={repoDetailInfo.repomonName}
                repoId={params.repoId}
                setStatUpdated={setStatUpdated}
                myRepo={repoDetailInfo.myRepo}
              />
            )}
            {tabIndex === 3 && (
              <DetailConvention
                conventionInfo={repoDetailConventionInfo!}
                setConventionUpdated={setConventionUpdated}
                repoId={params.repoId}
                myRepo={repoDetailInfo.myRepo}
                loading={conventionLoading}
              />
            )}
            {tabIndex === 4 && (
              <DetailContribution
                contributionInfo={repoDetailContributionInfo!}
              />
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default Page;
