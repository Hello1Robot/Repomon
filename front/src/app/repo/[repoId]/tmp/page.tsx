"use client";

import DetailRepomon from "@/components/Detail/DetailRepomon";
import React, { useEffect, useState } from "react";
import styles from "./page.module.scss";
import Modal from "react-modal";
import ExportModal from "@/components/Detail/ExportModal";
import Spinner from "@/components/Spinner";
import LoadingSpinner from "@/components/Skeletons/LoadingSpinner";
import DetailConvention from "@/components/Detail/DetailConvention";
import { RepoDetailConventionInfoType } from "@/types/repoDetail";
import { axiosRequestRepoDetailConvention } from "@/api/repoDetail";

function Page() {
  const [repomonUrl, setRepomonUrl] = useState(
    "https://repomon.s3.ap-northeast-2.amazonaws.com/models/Dora_1.glb"
  );
  const [repomonTier, setrepomonTier] = useState(1);
  const [repoDetailConventionInfo, setRepoDetailConventionInfo] =
    useState<RepoDetailConventionInfoType>();

  useEffect(() => {
    Modal.setAppElement("#repo-detail");
    requestRepoDetailConvention(4);
  }, []);

  const requestRepoDetailConvention = async (repoId: number) => {
    try {
      const res = await axiosRequestRepoDetailConvention(repoId);
      console.log("레포 디테일 컨벤션: ", res);
      setRepoDetailConventionInfo(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className={styles.pageContainer} id="repo-detail">
      {/* <div className={styles.info}>
        <div className={styles["repo-mon-card-div"]}>
          <div className={styles["repo-mon-card"]}> */}
      {/* <DetailRepomon repomonUrl={repomonUrl} repomonTier={repomonTier} /> */}
      {/* </div>
        </div>
      </div> */}
      {/* <button className={styles.btn}>
        <LoadingSpinner ml={4} mr={4} size={6} />
      </button> */}
      {/* <ExportModal
        repoId={4}
        userId={3}
        isTeam={true}
        lans={[
          "TypeScript",
          "Java",
          "Dockerfile",
          "CSS",
          "Shell",
          "SCSS",
          "JavaScript",
          "HTML",
        ]}
      /> */}
    </div>
  );
}

export default Page;