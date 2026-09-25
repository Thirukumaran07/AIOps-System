import {
  AlertTriangle,
  CheckCircle,
  Clock,
  History,
  RefreshCw,
  ShieldCheck,
  Wrench,
  XCircle,
} from "lucide-react";

import { useEffect, useMemo, useState } from "react";

import {
  executeHealing,
  getDevices,
  getHealingHistory,
} from "../services/api";

function SelfHealing() {
  const [devices, setDevices] = useState([]);
  const [history, setHistory] = useState([]);

  const [selectedDeviceId, setSelectedDeviceId] =
    useState("");

  const [rootCause, setRootCause] = useState("");

  const [loading, setLoading] = useState(true);
  const [healing, setHealing] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadData = async (showRefresh = false) => {
    try {
      if (showRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError("");

      const [deviceData, historyData] =
        await Promise.all([
          getDevices(),
          getHealingHistory(),
        ]);

      setDevices(
        Array.isArray(deviceData)
          ? deviceData
          : []
      );

      setHistory(
        Array.isArray(historyData)
          ? historyData
          : []
      );
    } catch (err) {
      console.error(
        "Failed to load self-healing data:",
        err
      );

      setError(
        "Unable to load self-healing information."
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();

    const timer = setInterval(() => {
      loadData(true);
    }, 30000);

    return () => {
      clearInterval(timer);
    };
  }, []);

  const handleHealing = async () => {
    if (!selectedDeviceId) {
      setError("Please select a device.");
      return;
    }

    if (!rootCause.trim()) {
      setError("Please enter the root cause.");
      return;
    }

    try {
      setHealing(true);
      setError("");
      setSuccess("");

      const result = await executeHealing(
        selectedDeviceId,
        rootCause.trim()
      );

      if (typeof result === "object") {
        if (result.success === true) {
          setSuccess(
            "Self-healing operation completed successfully."
          );
        } else {
          setSuccess(
            "Healing operation was executed but recovery was not confirmed."
          );
        }
      } else if (result === true) {
        setSuccess(
          "Self-healing operation completed successfully."
        );
      } else {
        setSuccess(
          "Healing operation completed."
        );
      }

      setRootCause("");

      await loadData(true);
    } catch (err) {
      console.error(
        "Self-healing execution failed:",
        err
      );

      setError(
        err?.response?.data?.message ||
          "Unable to execute self-healing operation."
      );
    } finally {
      setHealing(false);
    }
  };

  const stats = useMemo(() => {
    const successful = history.filter(
      (item) =>
        item.success === true
    ).length;

    const failed = history.filter(
      (item) =>
        item.success === false
    ).length;

    const successRate =
      history.length > 0
        ? (
            (successful / history.length) *
            100
          ).toFixed(2)
        : "0.00";

    return {
      total: history.length,
      successful,
      failed,
      successRate,
    };
  }, [history]);

  const getSuccessClass = (success) => {
    return success === true
      ? "healthy"
      : "critical";
  };

  const getSuccessIcon = (success) => {
    return success === true
      ? <CheckCircle size={16} />
      : <XCircle size={16} />;
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
          Loading self-healing system...
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <header className="topbar">
        <div>
          <h1>Self Healing</h1>
          <p>
            Automatically recover unhealthy infrastructure
            through intelligent remediation actions
          </p>
        </div>

        <div className="system-status">
          <span className="status-dot"></span>
          Healing Engine Active
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
            <Wrench size={24} />
          </div>

          <div>
            <span>Total Healing Operations</span>
            <strong>{stats.total}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon healthy">
            <CheckCircle size={24} />
          </div>

          <div>
            <span>Successful</span>
            <strong>{stats.successful}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon critical">
            <XCircle size={24} />
          </div>

          <div>
            <span>Failed</span>
            <strong>{stats.failed}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon warning">
            <ShieldCheck size={24} />
          </div>

          <div>
            <span>Success Rate</span>
            <strong>
              {stats.successRate}%
            </strong>
          </div>
        </div>
      </section>

      <section className="dashboard-card healing-control-card">
        <div className="card-header">
          <div>
            <h2>Manual Self-Healing</h2>
            <p>
              Trigger the healing engine for a selected
              device and root cause.
            </p>
          </div>

          <Wrench size={22} />
        </div>

        <div className="healing-controls">
          <div className="healing-field">
            <label htmlFor="healing-device">
              Device
            </label>

            <select
              id="healing-device"
              value={selectedDeviceId}
              onChange={(event) =>
                setSelectedDeviceId(
                  event.target.value
                )
              }
            >
              <option value="">
                Select a device
              </option>

              {devices.map((device) => (
                <option
                  key={device.id}
                  value={device.id}
                >
                  {device.name} —{" "}
                  {device.ipAddress}
                </option>
              ))}
            </select>
          </div>

          <div className="healing-field">
            <label htmlFor="healing-root-cause">
              Root Cause
            </label>

            <input
              id="healing-root-cause"
              type="text"
              value={rootCause}
              onChange={(event) =>
                setRootCause(event.target.value)
              }
              placeholder="Example: High CPU utilization"
            />
          </div>

          <button
            className="healing-button"
            onClick={handleHealing}
            disabled={healing}
          >
            <Wrench size={18} />

            {healing
              ? "Healing..."
              : "Execute Healing"}
          </button>
        </div>

        <div className="healing-warning">
          <AlertTriangle size={17} />

          <span>
            Healing actions are subject to the backend
            60-second cooldown for each device.
          </span>
        </div>
      </section>

      <section className="dashboard-card">
        <div className="card-header">
          <div>
            <h2>Recovery History</h2>
            <p>
              Previous self-healing operations and recovery
              results
            </p>
          </div>

          <History size={22} />
        </div>

        <div className="healing-history-list">
          {history.length === 0 ? (
            <div className="recommendation">
              <Wrench size={20} />

              <div>
                <strong>
                  No healing operations found
                </strong>

                <p>
                  Self-healing operations will appear here
                  after the healing engine is executed.
                </p>
              </div>
            </div>
          ) : (
            history.map((item) => {
              const resultClass =
                getSuccessClass(item.success);

              return (
                <div
                  className={`healing-history-card ${resultClass}`}
                  key={item.id}
                >
                  <div className="healing-history-header">
                    <div className="healing-history-title">
                      <div
                        className={`healing-result-icon ${resultClass}`}
                      >
                        {getSuccessIcon(
                          item.success
                        )}
                      </div>

                      <div>
                        <h3>
                          {item.healingAction ||
                            "Healing operation"}
                        </h3>

                        <span>
                          Device #{item.deviceId}
                        </span>
                      </div>
                    </div>

                    <div
                      className={`device-status-badge ${resultClass}`}
                    >
                      {getSuccessIcon(
                        item.success
                      )}

                      {item.success
                        ? "SUCCESS"
                        : "FAILED"}
                    </div>
                  </div>

                  <div className="healing-history-details">
                    <div className="healing-history-detail">
                      <AlertTriangle size={17} />

                      <div>
                        <span>Root Cause</span>

                        <strong>
                          {item.rootCause ||
                            "--"}
                        </strong>
                      </div>
                    </div>

                    <div className="healing-history-detail">
                      <Wrench size={17} />

                      <div>
                        <span>Healing Action</span>

                        <strong>
                          {item.healingAction ||
                            "--"}
                        </strong>
                      </div>
                    </div>

                    <div className="healing-history-detail">
                      <Clock size={17} />

                      <div>
                        <span>Executed At</span>

                        <strong>
                          {formatDate(
                            item.executedAt
                          )}
                        </strong>
                      </div>
                    </div>
                  </div>

                  {item.details && (
                    <div className="healing-details">
                      <span>Details</span>

                      <p>
                        {item.details}
                      </p>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </section>

      <div className="dashboard-refresh">
        <div>
          <RefreshCw
            size={15}
            className={
              refreshing
                ? "refresh-spinning"
                : ""
            }
          />

          <span>
            {refreshing
              ? "Updating healing history..."
              : "Healing history automatically refreshes every 30 seconds"}
          </span>
        </div>
      </div>
    </div>
  );
}

export default SelfHealing;