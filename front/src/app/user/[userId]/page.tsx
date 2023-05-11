"use client";

import React, { useEffect, useRef, useState } from "react";
import { ArrowPathIcon } from "@heroicons/react/24/solid";
import styles from "./page.module.scss";
import RepositoryCard from "@/components/RepositoryCard";
import { useRouter } from "next/navigation";
import {
  getTotalRepoList,
  getUserDetail,
  refreshAllRepo,
} from "@/api/userRepo";
import { RepoListType, UserInfoType } from "@/types/repoInfo";
import Paging from "@/components/UI/Pagination";
import Modal from "react-modal";
import LoadingSpinner from "@/components/Skeletons/LoadingSpinner";
import Ballon from "public/static/lotties/balloon.json";
import Lottie from "react-lottie-player";

const Page = ({ params }: { params: { userId: string } }) => {
  // 레포지토리 유저 정보 GET
  const [userInfo, setUserInfo] = useState<UserInfoType>();
  const [repoInfo, setRepoInfo] = useState<RepoListType>({
    repoListItems: [],
    totalElements: 0,
    totalPages: 0,
  });
  const [isLoaded, setIsLoaded] = useState<boolean>(false);
  const [isReloaded, setIsReloaded] = useState<boolean>(false);
  const [isListLoaded, setIsListLoaded] = useState<boolean>(true);
  const arrowRef = useRef<SVGSVGElement>(null);
  const [currentPage, setCurrentPage] = useState<number>(1);

  function getUserInfo(userId: string) {
    return getUserDetail(Number(userId));
  }
  useEffect(() => {
    const data = getUserInfo(params.userId)
      .then((response) => {
        const res = response.data.data;
        setUserInfo(res);
      })
      .catch((error) => {
        console.error(error);
      });
  }, []);

  // 레포지토리 리스트 GET
  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await getTotalRepoList(
          Number(params.userId),
          currentPage - 1,
          6
        );
        const data = response.data;
        setRepoInfo(data);
        setTimeout(() => {
          setIsLoaded(true);
        }, 1500);
      } catch (error) {
        console.error(error);
      }
    };

    fetchData();
  }, [isReloaded, currentPage]);

  // 동일 유저 체크
  const [isSameUser, setIsSameUser] = useState<boolean>();

  return (
    <div id="pageContainer">
      <div className={styles.pageContainer}>
        <div className={styles.bannerBack}>
          <Lottie
            loop
            animationData={Ballon}
            play
            style={{ width: 400, height: 400 }}
          />
        </div>
        <div className={styles.bodyContainer}>
          <div className={styles.sideProfile}>
            <div>
              <div
                className={styles.chracter}
                style={{
                  backgroundImage: `url('${userInfo?.avatarUrl}')`,
                }}
              />
              <p className={styles.boxTitle}>
                {userInfo?.nickname}({userInfo?.username})
              </p>
              <p className={styles.boxContent}>
                총 경험치 : {userInfo?.totalExp}({userInfo?.userRank}위)
              </p>
            </div>
            <div>
              <p className={styles.boxTitle}>대표 레포몬</p>
              <p
                className={styles.boxTitle}
                style={{ fontWeight: "800", marginBottom: "1em" }}
              >
                도슴고치
              </p>
              <div className={styles.charaterDiv} />
              <p className={styles.boxContent}>경험치 : 18502 (14위)</p>
              <p className={styles.boxContent}>배틀 레이팅 : 1850 (12위)</p>
            </div>
          </div>
          <div className={styles.bodyList}>
            <div className={styles.listTitle} style={{ display: "block" }}>
              <div
                style={{
                  display: "flex",
                  width: "50%",
                  alignItems: "center",
                  marginLeft: "10%",
                }}
              >
                <p style={{ width: "7em" }} id={styles.repoListTitle}>
                  레포지토리 목록
                </p>
                {isListLoaded === false ? (
                  <p style={{ fontSize: "1em", opacity: "0.5" }}>
                    레포 리스트 로딩중...
                  </p>
                ) : (
                  <ArrowPathIcon
                    width="2rem"
                    style={{ marginLeft: "2%" }}
                    className={styles.arrow}
                    ref={arrowRef}
                    onClick={async () => {
                      setIsListLoaded(false);
                      userInfo?.userId &&
                        (await refreshAllRepo(userInfo.userId));
                      setIsListLoaded(true);
                      setIsLoaded(false);
                      setIsReloaded(!isReloaded);
                    }}
                  />
                )}
              </div>
              <p
                style={{
                  fontSize: "1em",
                  opacity: "0.7",
                  marginLeft: "10%",
                  marginBlock: "1%",
                }}
                id={styles.reposubtitle}
              >
                <span style={{ color: "red", fontWeight: "800" }}>*</span> 최초
                로드 시, 리스트가 보이지 않을 때 갱신 버튼을 눌러주세요
              </p>
            </div>
            <div className={styles.listCards}>
              <div className="grid grid-cols-2 gap-4">
                {Array(repoInfo?.totalElements)
                  .fill(null)
                  .map((items, i) =>
                    repoInfo?.repoListItems &&
                    i < repoInfo.repoListItems.length ? (
                      <RepositoryCard
                        key={i}
                        title={repoInfo.repoListItems.at(i)?.repoName}
                        desc={repoInfo.repoListItems.at(i)?.repoDescription}
                        exp={repoInfo.repoListItems.at(i)?.repoExp}
                        rating={repoInfo.repoListItems.at(i)?.repoRating}
                        isActive={repoInfo.repoListItems.at(i)?.isActive}
                        userId={params.userId}
                        repoId={repoInfo.repoListItems.at(i)?.repoId || 0}
                        isSameUser={isSameUser}
                        setIsSameUser={setIsSameUser}
                        isLoaded={isLoaded}
                        repomonId={repoInfo.repoListItems.at(i)?.repomonId || 0}
                        repomonUrl={
                          repoInfo.repoListItems.at(i)?.repomonUrl || ""
                        }
                      />
                    ) : null
                  )}
              </div>

              <div className={styles.paginations}>
                <Paging
                  size={6}
                  currentPage={currentPage}
                  setCurrentPage={setCurrentPage}
                  totalPage={repoInfo?.totalPages + 1}
                  totalElement={repoInfo?.totalElements}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Page;
