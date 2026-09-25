import {
  AlertTriangle,
  Brain,
  CheckCircle,
  CircleHelp,
  Server,
  XCircle,
} from "lucide-react";

import { useEffect, useState } from "react";

import {
  getDevices,
  getAllMetrics,
  predictMetric,
} from "../services/api";

function Devices() {
  const [devices, setDevices] = useState([]);
  const [predictions, setPredictions] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadDevices = async () => {
      try {
        setLoading(true);
        setError("");

        const [deviceData, metricData] = await Promise.all([
          getDevices(),
          getAllMetrics(),
        ]);

        const safeDevices = Array.isArray(deviceData)
          ? deviceData
          : [];

        const safeMetrics = Array.isArray(metricData)
          ? metricData
          : [];

        setDevices(safeDevices);

        const predictionResults = await Promise.all(
          safeDevices.map(async (device) => {
            const deviceMetrics = safeMetrics
              .filter(
                (metric) =>
                  Number(metric.deviceId) === Number(device.id)
              )
              .sort(
                (a, b) =>
                  new Date(b.timestamp) -
                  new Date(a.timestamp)
              );

            if (deviceMetrics.length === 0) {
              return {
                deviceId: device.id,
                status: "UNKNOWN",
                anomalyScore: null,
              };
            }

            const latestMetric = deviceMetrics[0];

            try {
              const prediction = await predictMetric({
                cpuUsage: Number(latestMetric.cpuUsage ?? 0),
                memoryUsage: Number(
                  latestMetric.memoryUsage ?? 0
                ),
                diskUsage: Number(
                  latestMetric.diskUsage ?? 0
                ),
                latency: Number(
                  latestMetric.latency ?? 0
                ),
                packetLoss: Number(
                  latestMetric.packetLoss ?? 0
                ),
              });

              return {
                deviceId: device.id,
                status: prediction?.status || "UNKNOWN",
                anomalyScore:
                  prediction?.anomalyScore ?? null,
              };
            } catch (predictionError) {
              console.error(
                `Prediction failed for ${device.name}:`,
                predictionError
              );

              return {
                deviceId: device.id,
                status: "ERROR",
                anomalyScore: null,
              };
            }
          })
        );

        const predictionMap = {};

        predictionResults.forEach((prediction) => {
          predictionMap[prediction.deviceId] = prediction;
        });

        setPredictions(predictionMap);
      } catch (err) {
        console.error("Failed to load devices:", err);
        setError("Unable to load device information.");
      } finally {
        setLoading(false);
      }
    };

    loadDevices();
  }, []);

  const getStatusClass = (status) => {
    switch (status?.toUpperCase()) {
      case "HEALTHY":
        return "healthy";

      case "WARNING":
        return "warning";

      case "CRITICAL":
        return "critical";

      case "ERROR":
        return "error";

      default:
        return "unknown";
    }
  };

  const getStatusIcon = (status) => {
    switch (status?.toUpperCase()) {
      case "HEALTHY":
        return <CheckCircle size={16} />;

      case "WARNING":
        return <AlertTriangle size={16} />;

      case "CRITICAL":
        return <XCircle size={16} />;

      default:
        return <CircleHelp size={16} />;
    }
  };

  const getAnomalyPercentage = (score) => {
    if (score == null || Number.isNaN(Number(score))) {
      return 0;
    }

    const numericScore = Number(score);

    if (numericScore <= 1) {
      return Math.min(
        Math.max(numericScore * 100, 0),
        100
      );
    }

    return Math.min(
      Math.max(numericScore, 0),
      100
    );
  };

  if (loading) {
    return (
      <div className="dashboard">
        <div className="api-status">
          Loading devices and AI predictions...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard">
        <div className="api-error">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <header className="topbar">
        <div>
          <h1>Devices</h1>
          <p>
            Monitor all registered infrastructure devices
          </p>
        </div>

        <div className="system-status">
          <span className="status-dot"></span>
          Monitoring Active
        </div>
      </header>

      <section className="overview-grid">
        <div className="stat-card">
          <div className="stat-icon">
            <Server size={24} />
          </div>

          <div>
            <span>Total Devices</span>
            <strong>{devices.length}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon healthy">
            <CheckCircle size={24} />
          </div>

          <div>
            <span>Healthy</span>
            <strong>
              {
                devices.filter(
                  (device) =>
                    device.status?.toUpperCase() ===
                    "HEALTHY"
                ).length
              }
            </strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon warning">
            <AlertTriangle size={24} />
          </div>

          <div>
            <span>Warning</span>
            <strong>
              {
                devices.filter(
                  (device) =>
                    device.status?.toUpperCase() ===
                    "WARNING"
                ).length
              }
            </strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon ai">
            <Brain size={24} />
          </div>

          <div>
            <span>Critical</span>
            <strong>
              {
                devices.filter(
                  (device) =>
                    device.status?.toUpperCase() ===
                    "CRITICAL"
                ).length
              }
            </strong>
          </div>
        </div>
      </section>

      <section className="dashboard-card devices-table-card">
        <div className="card-header">
          <div>
            <h2>Device Inventory</h2>
            <p>
              Registered devices and their current AI health status
            </p>
          </div>

          <Server size={22} />
        </div>

        <div className="devices-table-container">
          <table className="devices-table">
            <thead>
              <tr>
                <th>Device</th>
                <th>IP Address</th>
                <th>Type</th>
                <th>Status</th>
                <th>Health Score</th>
                <th>AI Prediction</th>
                <th>Anomaly Score</th>
              </tr>
            </thead>

            <tbody>
              {devices.length === 0 ? (
                <tr>
                  <td
                    colSpan="7"
                    className="empty-table"
                  >
                    No devices found.
                  </td>
                </tr>
              ) : (
                devices.map((device) => {
                  const prediction =
                    predictions[device.id];

                  const predictionStatus =
                    prediction?.status || "UNKNOWN";

                  const statusClass =
                    getStatusClass(
                      predictionStatus
                    );

                  const anomalyPercentage =
                    getAnomalyPercentage(
                      prediction?.anomalyScore
                    );

                  return (
                    <tr key={device.id}>
                      <td>
                        <div className="device-name">
                          <div className="device-icon">
                            <Server size={18} />
                          </div>

                          <div>
                            <strong>
                              {device.name}
                            </strong>

                            <small>
                              Device #{device.id}
                            </small>
                          </div>
                        </div>
                      </td>

                      <td>
                        <span className="ip-address">
                          {device.ipAddress}
                        </span>
                      </td>

                      <td>
                        <span className="device-type">
                          {device.type}
                        </span>
                      </td>

                      <td>
                        <div
                          className={`device-status-badge ${getStatusClass(
                            device.status
                          )}`}
                        >
                          {getStatusIcon(
                            device.status
                          )}

                          {device.status || "UNKNOWN"}
                        </div>
                      </td>

                      <td>
                        <div className="health-score-cell">
                          <strong>
                            {device.healthScore != null
                              ? Number(
                                  device.healthScore
                                ).toFixed(2)
                              : "--"}
                            %
                          </strong>

                          <div className="table-progress">
                            <div
                              className="table-progress-bar"
                              style={{
                                width: `${Math.min(
                                  Math.max(
                                    Number(
                                      device.healthScore ??
                                        0
                                    ),
                                    0
                                  ),
                                  100
                                )}%`,
                              }}
                            ></div>
                          </div>
                        </div>
                      </td>

                      <td>
                        <div
                          className={`device-status-badge ${statusClass}`}
                        >
                          <Brain size={15} />
                          {predictionStatus}
                        </div>
                      </td>

                      <td>
                        <div className="anomaly-cell">
                          <div className="anomaly-value">
                            {prediction?.anomalyScore != null
                              ? Number(
                                  prediction.anomalyScore
                                ).toFixed(2)
                              : "--"}
                          </div>

                          <div className="table-progress">
                            <div
                              className={`table-progress-bar ${statusClass}`}
                              style={{
                                width: `${anomalyPercentage}%`,
                              }}
                            ></div>
                          </div>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

export default Devices;