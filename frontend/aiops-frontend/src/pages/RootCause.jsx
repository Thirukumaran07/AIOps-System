import {
  Activity,
  AlertTriangle,
  Brain,
  CheckCircle,
  Clock,
  Search,
  ShieldAlert,
  Wrench,
} from "lucide-react";

import { useEffect, useMemo, useState } from "react";

import {
  getAllMetrics,
  getAllRootCauses,
  analyzeRootCause,
} from "../services/api";

function RootCause() {
  const [rootCauses, setRootCauses] = useState([]);
  const [metrics, setMetrics] = useState([]);
  const [selectedMetricId, setSelectedMetricId] = useState("");
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");

      const [rootCauseData, metricData] =
        await Promise.all([
          getAllRootCauses(),
          getAllMetrics(),
        ]);

      setRootCauses(
        Array.isArray(rootCauseData)
          ? rootCauseData
          : []
      );

      setMetrics(
        Array.isArray(metricData)
          ? metricData
          : []
      );
    } catch (err) {
      console.error(
        "Failed to load root cause data:",
        err
      );

      setError(
        "Unable to load root cause analysis data."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleAnalyze = async () => {
    if (!selectedMetricId) {
      setError("Please select a metric first.");
      return;
    }

    try {
      setAnalyzing(true);
      setError("");
      setSuccess("");

      await analyzeRootCause(selectedMetricId);

      setSuccess(
        "Root cause analysis completed successfully."
      );

      await loadData();
    } catch (err) {
      console.error(
        "Root cause analysis failed:",
        err
      );

      setError(
        "Unable to perform root cause analysis."
      );
    } finally {
      setAnalyzing(false);
    }
  };

  const stats = useMemo(() => {
    const high = rootCauses.filter(
      (item) =>
        item.severity?.toUpperCase() === "HIGH"
    ).length;

    const medium = rootCauses.filter(
      (item) =>
        item.severity?.toUpperCase() === "MEDIUM"
    ).length;

    const low = rootCauses.filter(
      (item) =>
        item.severity?.toUpperCase() === "LOW"
    ).length;

    const averageConfidence =
      rootCauses.length > 0
        ? (
            rootCauses.reduce(
              (total, item) =>
                total +
                Number(item.confidence ?? 0),
              0
            ) / rootCauses.length
          ).toFixed(2)
        : "0.00";

    return {
      total: rootCauses.length,
      high,
      medium,
      low,
      averageConfidence,
    };
  }, [rootCauses]);

  const getSeverityClass = (severity) => {
    switch (severity?.toUpperCase()) {
      case "HIGH":
        return "critical";

      case "MEDIUM":
        return "warning";

      case "LOW":
        return "healthy";

      default:
        return "unknown";
    }
  };

  const getSeverityIcon = (severity) => {
    switch (severity?.toUpperCase()) {
      case "HIGH":
        return <ShieldAlert size={16} />;

      case "MEDIUM":
        return <AlertTriangle size={16} />;

      case "LOW":
        return <CheckCircle size={16} />;

      default:
        return <Activity size={16} />;
    }
  };

  const formatDate = (date) => {
    if (!date) {
      return "--";
    }

    const parsedDate = new Date(date);

    if (Number.isNaN(parsedDate.getTime())) {
      return date;
    }

    return parsedDate.toLocaleString();
  };

  if (loading) {
    return (
      <div className="dashboard">
        <div className="api-status">
          Loading root cause analysis...
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <header className="topbar">
        <div>
          <h1>Root Cause Analysis</h1>
          <p>
            Identify the probable cause of infrastructure
            anomalies using monitoring metrics
          </p>
        </div>

        <div className="system-status">
          <span className="status-dot"></span>
          AI Analysis Active
        </div>
      </header>

      {error && (
        <div className="api-error">
          {error}
        </div>
      )}

      {success && (
        <div className="api-success">
          {success}
        </div>
      )}

      <section className="overview-grid">
        <div className="stat-card">
          <div className="stat-icon ai">
            <Brain size={24} />
          </div>

          <div>
            <span>Total Analyses</span>
            <strong>{stats.total}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon critical">
            <ShieldAlert size={24} />
          </div>

          <div>
            <span>High Severity</span>
            <strong>{stats.high}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon warning">
            <AlertTriangle size={24} />
          </div>

          <div>
            <span>Medium Severity</span>
            <strong>{stats.medium}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon healthy">
            <CheckCircle size={24} />
          </div>

          <div>
            <span>Avg Confidence</span>
            <strong>
              {stats.averageConfidence}%
            </strong>
          </div>
        </div>
      </section>

      <section className="dashboard-card root-cause-analyzer">
        <div className="card-header">
          <div>
            <h2>Run Root Cause Analysis</h2>
            <p>
              Select a monitoring metric and analyze the
              probable cause of the detected anomaly.
            </p>
          </div>

          <Search size={22} />
        </div>

        <div className="root-cause-analyzer-controls">
          <div className="root-cause-select-wrapper">
            <label htmlFor="metric-select">
              Monitoring Metric
            </label>

            <select
              id="metric-select"
              value={selectedMetricId}
              onChange={(event) =>
                setSelectedMetricId(
                  event.target.value
                )
              }
            >
              <option value="">
                Select a metric
              </option>

              {metrics.map((metric) => (
                <option
                  key={metric.id}
                  value={metric.id}
                >
                  Metric #{metric.id} —{" "}
                  {metric.deviceName ||
                    `Device #${metric.deviceId}`}{" "}
                  —{" "}
                  {formatDate(metric.timestamp)}
                </option>
              ))}
            </select>
          </div>

          <button
            className="root-cause-analyze-button"
            onClick={handleAnalyze}
            disabled={
              analyzing || !selectedMetricId
            }
          >
            <Brain size={18} />

            {analyzing
              ? "Analyzing..."
              : "Analyze Root Cause"}
          </button>
        </div>
      </section>

      <section className="dashboard-card">
        <div className="card-header">
          <div>
            <h2>Root Cause Analysis History</h2>
            <p>
              Previously generated root cause findings
            </p>
          </div>

          <Activity size={22} />
        </div>

        <div className="root-cause-list">
          {rootCauses.length === 0 ? (
            <div className="recommendation">
              <Brain size={20} />

              <div>
                <strong>
                  No root cause analyses available
                </strong>

                <p>
                  Select a monitoring metric above to
                  generate the first analysis.
                </p>
              </div>
            </div>
          ) : (
            rootCauses.map((item) => {
              const severityClass =
                getSeverityClass(item.severity);

              return (
                <div
                  className={`root-cause-card ${severityClass}`}
                  key={item.id}
                >
                  <div className="root-cause-card-header">
                    <div className="root-cause-title">
                      <div
                        className={`root-cause-icon ${severityClass}`}
                      >
                        {getSeverityIcon(
                          item.severity
                        )}
                      </div>

                      <div>
                        <h3>
                          {item.rootCause ||
                            "Unknown root cause"}
                        </h3>

                        <span>
                          Metric #{item.metricId}
                        </span>
                      </div>
                    </div>

                    <div
                      className={`device-status-badge ${severityClass}`}
                    >
                      {getSeverityIcon(
                        item.severity
                      )}

                      {item.severity ||
                        "UNKNOWN"}
                    </div>
                  </div>

                  <div className="root-cause-details">
                    <div className="root-cause-detail">
                      <Brain size={17} />

                      <div>
                        <span>Confidence</span>

                        <strong>
                          {item.confidence != null
                            ? `${Number(
                                item.confidence
                              ).toFixed(2)}%`
                            : "--"}
                        </strong>
                      </div>
                    </div>

                    <div className="root-cause-detail">
                      <Wrench size={17} />

                      <div>
                        <span>
                          Recommended Action
                        </span>

                        <strong>
                          {item.recommendedAction ||
                            "--"}
                        </strong>
                      </div>
                    </div>

                    <div className="root-cause-detail">
                      <Clock size={17} />

                      <div>
                        <span>Detected At</span>

                        <strong>
                          {formatDate(
                            item.createdAt
                          )}
                        </strong>
                      </div>
                    </div>
                  </div>

                  <div className="confidence-bar-wrapper">
                    <div className="confidence-bar-header">
                      <span>
                        Analysis Confidence
                      </span>

                      <strong>
                        {item.confidence != null
                          ? `${Number(
                              item.confidence
                            ).toFixed(2)}%`
                          : "--"}
                      </strong>
                    </div>

                    <div className="confidence-bar">
                      <div
                        className={`confidence-progress ${severityClass}`}
                        style={{
                          width: `${Math.min(
                            Math.max(
                              Number(
                                item.confidence ?? 0
                              ),
                              0
                            ),
                            100
                          )}%`,
                        }}
                      ></div>
                    </div>
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

export default RootCause;