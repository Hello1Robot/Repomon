import { RepoInitType } from "@/types/repoRegist";
import { http } from "./axios";

// 유저 정보 조회
// GET
export const getUserDetail = async (userId: number) => {
  const res = await http.get(`/user/${userId}`);

  return res;
};

// 개별 레포 디테일 조회
// GET
export const getRepoDetail = async (repoId: number) => {
  const res = await http.get(`/repo/${repoId}/info`);

  return res;
};

// 개별 레포 정보 갱신
// PUT
export const refreshDetail = async (repoId: number) => {
  const res = await http.put(`/repo/${repoId}/info`);

  return res;
};

// 모든 레포 갱신
// PUT
export const refreshAllRepo = async (userId: number) => {
  const res = await http.put(`/repo/${userId}/reload`);

  return res.data.resultCode;
};

// 레포지토리 활성화 설정
// PUT
export const setRepoActive = async (repoId: number) => {
  const res = await http.put(`/repo/${repoId}/info/active`);

  return res;
};

// 개별 레포 랭킹정보 조회
// GET
export const getRankingInfo = async (repoId: number) => {
  const res = await http.get(`/repo/${repoId}/info/battle`);
  return res;
};

// 개별 레포 기여도 조회
// GET
export const getRepoConts = async (repoId: number) => {
  const res = await http.get(`/repo/${repoId}/info/contribute`);
  return res;
};

// 개별 레포 컨벤션 정보 조회
// GET
export const getRepoConvenInfo = async (repoId: number) => {
  const res = await http.get(`/repo/${repoId}/info/convention`);
  return res;
};

// 전체 레포 리스트 조회
// GET
export const getTotalRepoList = async (
  userId: number,
  pageNum: number,
  sizeNum: number
) => {
  const data = {
    page: pageNum,
    size: sizeNum,
  };

  const res = await http.get(`/repo/${userId}`, { data });

  return res;
};

// 레포몬 초기 등록
export const setRepoInit = async (data: RepoInitType) => {
  const res = await http.post(`/repomon/start`, data);
  return res;
};

// 레포몬 랜덤 3개 요청
export const getRandomRepo = async () => {
  const res = await http.get(`/repo/repomon`);
  return res;
};

// 레포지터리 Active 변경
export const setActiveRepo = async (repoId: number) => {
  const res = await http.put(`/repo/${repoId}/info/active`);
  return res;
};

// User Ranking 조회
export const getUserRanking = async (userId: number) => {
  const fetchData = {};
  const res = await http.get(`/rank/user`);
};
