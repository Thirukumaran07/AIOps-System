import {
  AlertTriangle,
  Brain,
  CheckCircle,
  CircleHelp,
  Cpu,
  HardDrive,
  Network,
  Server,
  ShieldAlert,
} from "lucide-react";

import { useEffect, useMemo, useState } from "react";

import {
  getDevices,
  getAllMetrics,
  predictMetric,
} from "../services/api";

function Predictions() {
  const [devices, setDevices] = useState([]);
  const [predictions, setPredictions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState("");

  const runPredictions = async () => {
    try {
      setRunning(true);
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

      const results = await Promise.all(
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
              deviceName: device.name,
              deviceType: device.type,
              status: "UNKNOWN",
              anomalyScore: null,
              metric: null,
            };
          }

          const latestMetric = deviceMetrics[0];

          try {
            const prediction = await predictMetric({
              cpuUsage: Number(
                latestMetric.cpuUsage ?? 0
              ),
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
              deviceName: device.name,
              deviceType: device.type,
              status:
                prediction?.status || "UNKNOWN",
              anomalyScore:
                prediction?.anomalyScore ?? null,
              metric: latestMetric,
            };
          } catch (predictionError) {
            console.error(
              `Prediction failed for ${device.name}:`,
              predictionError
            );

            return {
              deviceId: device.id,
              deviceName: device.name,
              deviceType: device.type,
              status: "ERROR",
              anomalyScore: null,
              metric: latestMetric,
            };
          }
        })
      );

      setPredictions(results);
    } catch (err) {
      console.error(
        "Failed to generate predictions:",
        err
      );

      setError(
        "Unable to connect to the monitoring or ML service."
      );
    } finally {
      setLoading(false);
      setRunning(false);
    }
  };

  useEffect(() => {
    runPredictions();
  }, []);

  const statistics = useMemo(() => {
    return {
      total: predictions.length,
      healthy: predictions.filter(
        (item) =>
          item.status?.toUpperCase() === "HEALTHY"
      ).length,
      warning: predictions.filter(
        (item) =>
          item.status?.toUpperCase() === "WARNING"
      ).length,
      critical: predictions.filter(
        (item) =>
          item.status?.toUpperCase() === "CRITICAL"
      ).length,
      unknown: predictions.filter(
        (item) =>
          item.status?.toUpperCase() === "UNKNOWN"
      ).length,
      errors: predictions.filter(
        (item) =>
          item.status?.toUpperCase() === "ERROR"
      ).length,
    };
  }, [predictions]);

  const highestRisk = useMemo(() => {
    if (!predictions.length) {
      return null;
    }

    const priority = {
      CRITICAL: 4,
      WARNING: 3,
      ERROR: 2,
      UNKNOWN: 1,
      HEALTHY: 0,
    };

    return [...predictions].sort((a, b) => {
      const priorityDifference =
        (priority[b.status?.toUpperCase()] || 0) -
        (priority[a.status?.toUpperCase()] || 0);

      if (priorityDifference !== 0) {
        return priorityDifference;
      }

      return (
        Number(b.anomalyScore ?? 0) -
        Number(a.anomalyScore ?? 0)
      );
    })[0];
  }, [predictions]);

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
        return <CheckCircle size={18} />;

      case "WARNING":
        return <AlertTriangle size={18} />;

      case "CRITICAL":
        return <ShieldAlert size={18} />;

      case "ERROR":
        return <AlertTriangle size={18} />;

      default:
        return <CircleHelp size={18} />;
    }
  };

  const getScorePercentage = (score) => {
    if (
      score == null ||
      Number.isNaN(Number(score))
    ) {
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

  const formatMetric = (value, suffix = "") => {
    if (
      value == null ||
      Number.isNaN(Number(value))
    ) {
      return "--";
    }

    return `${Number(value).toFixed(2)}${suffix}`;
  };

  if (loading) {
    return (
      <div className="dashboard">
        <div className="api-status">
          Connecting to the AI prediction engine...
        </div>
      </div>
    );
  }

  if (error && predictions.length === 0) {
    return (
      <div className="dashboard">
        <div className="api-error">
          {error}
        </div>

        <button
          className="prediction-run-button"
          onClick={runPredictions}
        >
          <Brain size={17} />
          Retry Prediction
        </button>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <header className="topbar">
        <div>
          <h1>AI Predictions</h1>
          <p>
            Intelligent anomaly detection using the ML engine
          </p>
        </div>

        <button
          className="prediction-run-button"
          onClick={runPredictions}
          disabled={running}
        >
          <Brain
            size={17}
            className={
              running
                ? "prediction-brain-spinning"
                : ""
            }
          />

          {running
            ? "Analyzing..."
            : "Run AI Analysis"}
        </button>
      </header>

      {error && (
        <div className="api-error">
          {error}
        </div>
      )}

      <section className="overview-grid">
        <div className="stat-card">
          <div className="stat-icon ai">
            <Brain size={24} />
          </div>

          <div>
            <span>Total Predictions</span>
            <strong>{statistics.total}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon healthy">
            <CheckCircle size={24} />
          </div>

          <div>
            <span>Healthy</span>
            <strong>{statistics.healthy}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon warning">
            <AlertTriangle size={24} />
          </div>

          <div>
            <span>Warnings</span>
            <strong>{statistics.warning}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon critical">
            <ShieldAlert size={24} />
          </div>

          <div>
            <span>Critical</span>
            <strong>{statistics.critical}</strong>
          </div>
        </div>
      </section>

      <section className="dashboard-grid">
        <div className="dashboard-card">
          <div className="card-header">
            <div>
              <h2>AI Engine Status</h2>
              <p>
                Current prediction engine summary
              </p>
            </div>

            <Brain size={22} />
          </div>

          <div className="ai-engine-status">
            <div className="ai-engine-icon">
              <Brain size={30} />
            </div>

            <div>
              <strong>ML Prediction Engine</strong>

              <span>
                {running
                  ? "Analyzing monitoring data..."
                  : "Prediction engine ready"}
              </span>
            </div>

            <div className="ai-engine-online">
              <span className="status-dot"></span>
              ONLINE
            </div>
          </div>

          <div className="prediction-summary">
            <div>
              <span>Devices analyzed</span>
              <strong>{statistics.total}</strong>
            </div>

            <div>
              <span>Unknown</span>
              <strong>{statistics.unknown}</strong>
            </div>

            <div>
              <span>Errors</span>
              <strong>{statistics.errors}</strong>
            </div>
          </div>
        </div>

        <div className="dashboard-card">
          <div className="card-header">
            <div>
              <h2>Highest Risk</h2>
              <p>
                Device requiring the most attention
              </p>
            </div>

            <ShieldAlert size={22} />
          </div>

          {highestRisk ? (
            <div
              className={`highest-risk ${getStatusClass(
                highestRisk.status
              )}`}
            >
              <div className="highest-risk-header">
                <div>
                  <span>Device</span>
                  <strong>
                    {highestRisk.deviceName}
                  </strong>
                </div>

                <div
                  className={`prediction-status ${getStatusClass(
                    highestRisk.status
                  )}`}
                >
                  {highestRisk.status}
                </div>
              </div>

              <div className="highest-risk-score">
                <span>Anomaly Score</span>

                <strong>
                  {formatMetric(
                    highestRisk.anomalyScore
                  )}
                </strong>
              </div>

              <div className="anomaly-progress">
                <div
                  className={`anomaly-progress-bar ${getStatusClass(
                    highestRisk.status
                  )}`}
                  style={{
                    width: `${getScorePercentage(
                      highestRisk.anomalyScore
                    )}%`,
                  }}
                ></div>
              </div>
            </div>
          ) : (
            <div className="empty-alerts">
              <CircleHelp size={30} />
              <strong>
                No prediction available
              </strong>
            </div>
          )}
        </div>
      </section>

      <section className="dashboard-card prediction-console">
        <div className="card-header">
          <div>
            <h2>Device Prediction Analysis</h2>
            <p>
              Live ML predictions generated from the latest device metrics
            </p>
          </div>

          <Brain size={22} />
        </div>

        <div className="prediction-console-list">
          {predictions.length === 0 ? (
            <div className="empty-alerts">
              <Brain size={30} />

              <strong>
                No prediction data available
              </strong>

              <p>
                Run the AI analysis after monitoring metrics
                are available.
              </p>
            </div>
          ) : (
            predictions.map((prediction) => {
              const statusClass =
                getStatusClass(
                  prediction.status
                );

              const scorePercentage =
                getScorePercentage(
                  prediction.anomalyScore
                );

              return (
                <div
                  className={`prediction-console-card ${statusClass}`}
                  key={prediction.deviceId}
                >
                  <div className="prediction-console-header">
                    <div className="prediction-device">
                      <div className="device-icon">
                        <Server size={19} />
                      </div>

                      <div>
                        <strong>
                          {prediction.deviceName}
                        </strong>

                        <small>
                          {prediction.deviceType ||
                            "DEVICE"}{" "}
                          • ID #{prediction.deviceId}
                        </small>
                      </div>
                    </div>

                    <div
                      className={`prediction-status ${statusClass}`}
                    >
                      {getStatusIcon(
                        prediction.status
                      )}

                      {prediction.status}
                    </div>
                  </div>

                  <div className="prediction-metrics">
                    <div>
                      <Cpu size={14} />
                      <span>CPU</span>
                      <strong>
                        {formatMetric(
                          prediction.metric
                            ?.cpuUsage,
                          "%"
                        )}
                      </strong>
                    </div>

                    <div>
                      <HardDrive size={14} />
                      <span>Memory</span>
                      <strong>
                        {formatMetric(
                          prediction.metric
                            ?.memoryUsage,
                          "%"
                        )}
                      </strong>
                    </div>

                    <div>
                      <Network size={14} />
                      <span>Network</span>
                      <strong>
                        {formatMetric(
                          prediction.metric
                            ?.networkUsage,
                          "%"
                        )}
                      </strong>
                    </div>

                    <div>
                      <ActivityIcon />
                      <span>Latency</span>
                      <strong>
                        {formatMetric(
                          prediction.metric
                            ?.latency,
                          " ms"
                        )}
                      </strong>
                    </div>
                  </div>

                  <div className="prediction-score-section">
                    <div className="prediction-score-header">
                      <span>
                        Anomaly Score
                      </span>

                      <strong>
                        {formatMetric(
                          prediction.anomalyScore
                        )}
                      </strong>
                    </div>

                    <div className="anomaly-progress">
                      <div
                        className={`anomaly-progress-bar ${statusClass}`}
                        style={{
                          width: `${scorePercentage}%`,
                        }}
                      ></div>
                    </div>
                  </div>

                  <div className="prediction-footer">
                    <span>
                      Latest metric:
                    </span>

                    <span>
                      {prediction.metric?.timestamp
                        ? new Date(
                            prediction.metric.timestamp
                          ).toLocaleString()
                        : "No metric available"}
                    </span>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </section>
    </div>
  );
}

function ActivityIcon() {
  return <Network size={14} />;
}

export default Predictions;