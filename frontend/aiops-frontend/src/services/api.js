import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080/api/v1",
  headers: {
    "Content-Type": "application/json",
  },
});

export const getDevices = async () => {
  const response = await api.get("/devices");
  return response.data;
};

export const getAllAlerts = async () => {
  const response = await api.get("/alerts");
  return response.data;
};

export const getOpenAlerts = async () => {
  const response = await api.get("/alerts/open");
  return response.data;
};

export const getAllMetrics = async () => {
  const response = await api.get("/metrics");
  return response.data;
};

export const predictMetric = async (data) => {
  const response = await api.post(
    "/predictions/predict",
    null,
    {
      params: data,
    }
  );

  return response.data;
};

export const getAllRootCauses = async () => {
  const response = await axios.get(
    "http://localhost:8080/api/root-causes"
  );

  return response.data;
};

export const analyzeRootCause = async (metricId) => {
  const response = await axios.post(
    `http://localhost:8080/api/root-causes/analyze/${metricId}`
  );

  return response.data;
};

export const getRootCausesByMetric = async (metricId) => {
  const response = await axios.get(
    `http://localhost:8080/api/root-causes/metric/${metricId}`
  );

  return response.data;
};

export const executeHealing = async (
  deviceId,
  rootCause
) => {
  const response = await axios.post(
    "http://localhost:8080/api/healing/execute",
    null,
    {
      params: {
        deviceId,
        rootCause,
      },
    }
  );

  return response.data;
};

export const getHealingHistory = async () => {
  const response = await axios.get(
    "http://localhost:8080/api/healing/history"
  );

  return response.data;
};

export const getRecentHealingHistory = async () => {
  const response = await axios.get(
    "http://localhost:8080/api/healing/history/recent"
  );

  return response.data;
};

export const getHealingHistoryByDevice = async (
  deviceId
) => {
  const response = await axios.get(
    `http://localhost:8080/api/healing/history/device/${deviceId}`
  );

  return response.data;
};

export default api;