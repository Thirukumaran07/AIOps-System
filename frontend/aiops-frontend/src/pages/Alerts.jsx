import {
  AlertTriangle,
  Bell,
  CheckCircle,
  CircleAlert,
  Clock,
  Server,
  ShieldAlert,
  XCircle,
} from "lucide-react";

import { useEffect, useMemo, useState } from "react";

import {
  getAllAlerts,
  getOpenAlerts,
} from "../services/api";

function Alerts() {
  const [alerts, setAlerts] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadAlerts = async () => {
      try {
        setLoading(true);
        setError("");

        const [allAlerts, openAlerts] = await Promise.all([
          getAllAlerts(),
          getOpenAlerts(),
        ]);

        const safeAllAlerts = Array.isArray(allAlerts)
          ? allAlerts
          : [];

        const safeOpenAlerts = Array.isArray(openAlerts)
          ? openAlerts
          : [];

        const openIds = new Set(
          safeOpenAlerts.map((alert) => alert.id)
        );

        const normalizedAlerts = safeAllAlerts.map(
          (alert) => ({
            ...alert,
            isOpen: openIds.has(alert.id),
          })
        );

        setAlerts(normalizedAlerts);
      } catch (err) {
        console.error("Failed to load alerts:", err);
        setError("Unable to load alerts.");
      } finally {
        setLoading(false);
      }
    };

    loadAlerts();
  }, []);

  const statistics = useMemo(() => {
    const critical = alerts.filter(
      (alert) =>
        alert.severity?.toUpperCase() === "CRITICAL"
    ).length;

    const warning = alerts.filter(
      (alert) =>
        alert.severity?.toUpperCase() === "WARNING"
    ).length;

    const open = alerts.filter(
      (alert) => alert.isOpen
    ).length;

    return {
      total: alerts.length,
      open,
      critical,
      warning,
    };
  }, [alerts]);

  const filteredAlerts = useMemo(() => {
    if (filter === "OPEN") {
      return alerts.filter((alert) => alert.isOpen);
    }

    if (filter === "CRITICAL") {
      return alerts.filter(
        (alert) =>
          alert.severity?.toUpperCase() === "CRITICAL"
      );
    }

    if (filter === "WARNING") {
      return alerts.filter(
        (alert) =>
          alert.severity?.toUpperCase() === "WARNING"
      );
    }

    if (filter === "CLOSED") {
      return alerts.filter((alert) => !alert.isOpen);
    }

    return alerts;
  }, [alerts, filter]);

  const getSeverityClass = (severity) => {
    switch (severity?.toUpperCase()) {
      case "CRITICAL":
        return "critical";

      case "WARNING":
        return "warning";

      case "INFO":
      case "LOW":
        return "info";

      default:
        return "unknown";
    }
  };

  const getSeverityIcon = (severity) => {
    switch (severity?.toUpperCase()) {
      case "CRITICAL":
        return <ShieldAlert size={17} />;

      case "WARNING":
        return <AlertTriangle size={17} />;

      default:
        return <CircleAlert size={17} />;
    }
  };

  const getAlertMessage = (alert) => {
    return (
      alert.message ||
      alert.description ||
      alert.alertMessage ||
      alert.title ||
      "Alert detected"
    );
  };

  const getDeviceName = (alert) => {
    return (
      alert.deviceName ||
      alert.device?.name ||
      `Device #${alert.deviceId ?? "--"}`
    );
  };

  const getTimestamp = (alert) => {
    const timestamp =
      alert.createdAt ||
      alert.updatedAt ||
      alert.timestamp;

    if (!timestamp) {
      return "--";
    }

    return new Date(timestamp).toLocaleString();
  };

  if (loading) {
    return (
      <div className="dashboard">
        <div className="api-status">
          Loading alerts...
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
          <h1>Alerts</h1>
          <p>
            Monitor and investigate infrastructure alerts
          </p>
        </div>

        <div className="system-status">
          <span className="status-dot"></span>
          Alert Monitoring Active
        </div>
      </header>

      <section className="overview-grid">
        <div className="stat-card">
          <div className="stat-icon">
            <Bell size={24} />
          </div>

          <div>
            <span>Total Alerts</span>
            <strong>{statistics.total}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon warning">
            <Clock size={24} />
          </div>

          <div>
            <span>Open Alerts</span>
            <strong>{statistics.open}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon critical">
            <XCircle size={24} />
          </div>

          <div>
            <span>Critical</span>
            <strong>{statistics.critical}</strong>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon warning">
            <AlertTriangle size={24} />
          </div>

          <div>
            <span>Warning</span>
            <strong>{statistics.warning}</strong>
          </div>
        </div>
      </section>

      <section className="dashboard-card alerts-card">
        <div className="card-header">
          <div>
            <h2>Alert Management</h2>
            <p>
              Active and historical infrastructure alerts
            </p>
          </div>

          <Bell size={22} />
        </div>

        <div className="alert-filters">
          {[
            ["ALL", "All Alerts"],
            ["OPEN", "Open"],
            ["CRITICAL", "Critical"],
            ["WARNING", "Warning"],
            ["CLOSED", "Closed"],
          ].map(([value, label]) => (
            <button
              key={value}
              className={`alert-filter ${
                filter === value ? "active" : ""
              }`}
              onClick={() => setFilter(value)}
            >
              {label}
            </button>
          ))}
        </div>

        <div className="alerts-list">
          {filteredAlerts.length === 0 ? (
            <div className="empty-alerts">
              <CheckCircle size={32} />

              <strong>
                No alerts found
              </strong>

              <p>
                There are no alerts matching the selected filter.
              </p>
            </div>
          ) : (
            filteredAlerts.map((alert) => {
              const severityClass =
                getSeverityClass(alert.severity);

              return (
                <div
                  className={`alert-item ${severityClass}`}
                  key={alert.id}
                >
                  <div className="alert-icon">
                    {getSeverityIcon(
                      alert.severity
                    )}
                  </div>

                  <div className="alert-content">
                    <div className="alert-title-row">
                      <strong>
                        {getAlertMessage(alert)}
                      </strong>

                      <span
                        className={`alert-severity ${severityClass}`}
                      >
                        {alert.severity || "UNKNOWN"}
                      </span>
                    </div>

                    <div className="alert-details">
                      <span>
                        <Server size={13} />
                        {getDeviceName(alert)}
                      </span>

                      <span>
                        <Clock size={13} />
                        {getTimestamp(alert)}
                      </span>
                    </div>
                  </div>

                  <div
                    className={`alert-status ${
                      alert.isOpen
                        ? "open"
                        : "closed"
                    }`}
                  >
                    {alert.isOpen ? (
                      <>
                        <CircleAlert size={14} />
                        OPEN
                      </>
                    ) : (
                      <>
                        <CheckCircle size={14} />
                        CLOSED
                      </>
                    )}
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

export default Alerts;