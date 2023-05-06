import { StatType } from "@/types/repoDetail";
import { http } from "./axios";

export const axiosRequestRepoDetail = async (
  repoId: number,
  userId?: number
) => {
  if (userId) {
    const res = await http.get(`repo/${repoId}/info/${userId}`);
    return res;
  } else {
    const res = await http.get(`repo/${repoId}/info`);
    return res;
  }
};

export const axiosRequestRepoDetailResearch = async (repoId: number) => {
  const res = await http.get(`repo/${repoId}/info/research`);

  return res;
};

export const axiosRequestRepoDetailBattleInfo = async (repoId: number) => {
  const res = await http.get(`repomon/${repoId}`);

  return res;
};

export const axiosRequestSetPeriod = async (
  repoId: number,
  startedAt: string,
  endAt: string
) => {
  const data = {
    startedAt,
    endAt,
  };

  const res = await http.put(`repo/${repoId}/info/period`, data);

  return res;
};

export const axiosRequestCheckNicknameDuplicated = async (
  repomonNickname: string
) => {
  const data = {
    repomonNickname,
  };

  const res = await http.post(`repo/nickname`, data);

  return res;
};

export const axiosRequestChangeNickname = async (
  repoId: number,
  repomonNickname: string
) => {
  const data = {
    repoId,
    repomonNickname,
  };

  const res = await http.put(`repomon/nickname`, data);

  return res;
};

export const axiosRequestBattleRanking = async (repoId: number) => {
  const res = await http.get(`repo/${repoId}/info/battle`);

  return res;
};

export const axiosRequestBattleRecord = async (repoId: number) => {
  const res = await http.get(`repomon/${repoId}/match/result`);

  return res;
};

export const axiosRequestUpStat = async (
  repoId: number,
  increaseInfo: StatType
) => {
  let data = {
    repoId,
  };

  for (let statPoint in increaseInfo) {
    data = { ...data, [statPoint]: increaseInfo[statPoint] };
  }

  const res = await http.put("repomon/stat", { data });

  return res;
};

export const axiosRequestRepoDetailConvention = async (repoId: number) => {
  const res = await http.get(`repo/${repoId}/info/convention`);

  return res;
};

export const axiosRequestRepoDetailContribution = async (repoId: number) => {
  const res = await http.get(`repo/${repoId}/info/contribute`);

  return res;
};
