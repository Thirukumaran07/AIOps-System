import {
  Activity,
  Cpu,
  HardDrive,
  Network,
  Server,
  Wifi,
} from "lucide-react";

import { useEffect, useMemo, useState } from "react";

import { getAllMetrics } from "../services/api";

function Metrics() {
  const [metrics, setMetrics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadMetrics = async () => {
      try {
        setLoading(true);
        setError("");

        const data = await getAllMetrics();

        setMetrics(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Failed to load metrics:", err);
        setError("Unable to load monitoring metrics.");
      } finally {
        setLoading(false);
      }
    };

    loadMetrics();
  }, []);

  const latestMetric = useMemo(() => {
    if (!metrics.length) {
      return null;
    }

    return [...metrics].sort(
      (a, b) =>
        new Date(b.timestamp) -
        new Date(a.timestamp)
    )[0];
  }, [metrics]);

  const averageMetrics = useMemo(() => {
    if (!metrics.length) {
      return {
        cpu: 0,
        memory: 0,
        network: 0,
        latency: 0,
        packetLoss: 0,
      };
    }

    const total = metrics.reduce(
      (acc, metric) => ({
        cpu: acc.cpu + Number(metric.cpuUsage ?? 0),
        memory:
          acc.memory + Number(metric.memoryUsage ?? 0),
        network:
          acc.network + Number(metric.networkUsage ?? 0),
        latency:
          acc.latency + Number(metric.latency ?? 0),
        packetLoss:
          acc.packetLoss +
          Number(metric.packetLoss ?? 0),
      }),
      {
        cpu: 0,
        memory: 0,
        network: 0,
        latency: 0,
        packetLoss: 0,
      }
    );

    return {
      cpu: total.cpu / metrics.length,
      memory: total.memory / metrics.length,
      network: total.network / metrics.length,
      latency: total.latency / metrics.length,
      packetLoss:
        total.packetLoss / metrics.length,
    };
  }, [metrics]);

  const formatValue = (value, suffix = "") => {
    if (value == null || Number.isNaN(Number(value))) {
      return "--";
    }

    return `${Number(value).toFixed(2)}${suffix}`;
  };

  const getAnomalyClass = (status) => {
    switch (status?.toUpperCase()) {
      case "NORMAL":
      case "HEALTHY":
        return "healthy";

      case "WARNING":
        return "warning";

      case "ANOMALY":
      case "CRITICAL":
        return "critical";

      default:
        return "unknown";
    }
  };

  if (loading) {
    return (
      <div className="dashboard">
        <div className="api-status">
          Loading monitoring metrics...
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
          <h1>Metrics</h1>
          <p>
            Real-time infrastructure performance metrics
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
            <Activity size={24} />
          </div>

          <div>
            <span>Total Metrics</span>
            <strong>{metrics.length}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon">
            <Cpu size={24} />
          </div>

          <div>
            <span>Latest CPU</span>
            <strong>
              {formatValue(
                latestMetric?.cpuUsage,
                "%"
              )}
            </strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon">
            <HardDrive size={24} />
          </div>

          <div>
            <span>Latest Memory</span>
            <strong>
              {formatValue(
                latestMetric?.memoryUsage,
                "%"
              )}
            </strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon ai">
            <Network size={24} />
          </div>

          <div>
            <span>Latest Network</span>
            <strong>
              {formatValue(
                latestMetric?.networkUsage,
                "%"
              )}
            </strong>
          </div>
        </div>
      </section>

      <section className="dashboard-grid">
        <div className="dashboard-card">
          <div className="card-header">
            <div>
              <h2>Average Resource Usage</h2>
              <p>
                Average values across collected metrics
              </p>
            </div>

            <Cpu size={22} />
          </div>

          <div className="metric-item">
            <div>
              <span>CPU Usage</span>
              <strong>
                {formatValue(
                  averageMetrics.cpu,
                  "%"
                )}
              </strong>
            </div>

            <div className="progress">
              <div
                className="progress-bar"
                style={{
                  width: `${Math.min(
                    Math.max(
                      averageMetrics.cpu,
                      0
                    ),
                    100
                  )}%`,
                }}
              ></div>
            </div>
          </div>

          <div className="metric-item">
            <div>
              <span>Memory Usage</span>
              <strong>
                {formatValue(
                  averageMetrics.memory,
                  "%"
                )}
              </strong>
            </div>

            <div className="progress">
              <div
                className="progress-bar"
                style={{
                  width: `${Math.min(
                    Math.max(
                      averageMetrics.memory,
                      0
                    ),
                    100
                  )}%`,
                }}
              ></div>
            </div>
          </div>

          <div className="metric-item">
            <div>
              <span>Network Usage</span>
              <strong>
                {formatValue(
                  averageMetrics.network,
                  "%"
                )}
              </strong>
            </div>

            <div className="progress">
              <div
                className="progress-bar"
                style={{
                  width: `${Math.min(
                    Math.max(
                      averageMetrics.network,
                      0
                    ),
                    100
                  )}%`,
                }}
              ></div>
            </div>
          </div>
        </div>

        <div className="dashboard-card">
          <div className="card-header">
            <div>
              <h2>Network Performance</h2>
              <p>
                Average network quality indicators
              </p>
            </div>

            <Wifi size={22} />
          </div>

          <div className="metric-highlight">
            <div className="metric-highlight-icon">
              <Activity size={20} />
            </div>

            <div>
              <span>Average Latency</span>
              <strong>
                {formatValue(
                  averageMetrics.latency,
                  " ms"
                )}
              </strong>
            </div>
          </div>

          <div className="metric-highlight">
            <div className="metric-highlight-icon">
              <Network size={20} />
            </div>

            <div>
              <span>Average Packet Loss</span>
              <strong>
                {formatValue(
                  averageMetrics.packetLoss,
                  "%"
                )}
              </strong>
            </div>
          </div>

          <div className="metric-highlight">
            <div className="metric-highlight-icon">
              <Server size={20} />
            </div>

            <div>
              <span>Latest Device</span>
              <strong>
                {latestMetric?.deviceName || "--"}
              </strong>
            </div>
          </div>
        </div>
      </section>

      <section className="dashboard-card metrics-table-card">
        <div className="card-header">
          <div>
            <h2>Metric History</h2>
            <p>
              Latest collected monitoring data
            </p>
          </div>

          <Activity size={22} />
        </div>

        <div className="metrics-table-container">
          <table className="metrics-table">
            <thead>
              <tr>
                <th>Device</th>
                <th>CPU</th>
                <th>Memory</th>
                <th>Disk</th>
                <th>Network</th>
                <th>Latency</th>
                <th>Packet Loss</th>
                <th>Anomaly</th>
                <th>Timestamp</th>
              </tr>
            </thead>

            <tbody>
              {metrics.length === 0 ? (
                <tr>
                  <td
                    colSpan="9"
                    className="empty-table"
                  >
                    No metrics found.
                  </td>
                </tr>
              ) : (
                [...metrics]
                  .sort(
                    (a, b) =>
                      new Date(b.timestamp) -
                      new Date(a.timestamp)
                  )
                  .map((metric) => {
                    const anomalyClass =
                      getAnomalyClass(
                        metric.anomalyStatus
                      );

                    return (
                      <tr key={metric.id}>
                        <td>
                          <div className="metric-device">
                            <div className="device-icon">
                              <Server size={17} />
                            </div>

                            <div>
                              <strong>
                                {metric.deviceName ||
                                  `Device #${metric.deviceId}`}
                              </strong>

                              <small>
                                ID: {metric.deviceId}
                              </small>
                            </div>
                          </div>
                        </td>

                        <td>
                          {formatValue(
                            metric.cpuUsage,
                            "%"
                          )}
                        </td>

                        <td>
                          {formatValue(
                            metric.memoryUsage,
                            "%"
                          )}
                        </td>

                        <td>
                          {formatValue(
                            metric.diskUsage,
                            "%"
                          )}
                        </td>

                        <td>
                          {formatValue(
                            metric.networkUsage,
                            "%"
                          )}
                        </td>

                        <td>
                          {formatValue(
                            metric.latency,
                            " ms"
                          )}
                        </td>

                        <td>
                          {formatValue(
                            metric.packetLoss,
                            "%"
                          )}
                        </td>

                        <td>
                          <div
                            className={`metric-anomaly-badge ${anomalyClass}`}
                          >
                            {metric.anomalyStatus ||
                              "UNKNOWN"}
                          </div>

                          <small className="anomaly-score-text">
                            Score:{" "}
                            {formatValue(
                              metric.anomalyScore
                            )}
                          </small>
                        </td>

                        <td>
                          <span className="metric-time">
                            {metric.timestamp
                              ? new Date(
                                  metric.timestamp
                                ).toLocaleString()
                              : "--"}
                          </span>
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

export default Metrics;