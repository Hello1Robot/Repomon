import { http } from "./axios";

export const axiosRequestRepoRank = async (page: number, search?: string) => {
  const params = {
    page,
    search,
  };

  const res = await http.get("rank/repo", { params });

  return res;
};
