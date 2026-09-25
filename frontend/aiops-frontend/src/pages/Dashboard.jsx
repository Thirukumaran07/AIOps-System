import {
    Activity,
    AlertTriangle,
    Brain,
    Cpu,
    HeartPulse,
    Network,
    Server,
    ShieldCheck,
    Wrench,
    RefreshCw,
} from "lucide-react";

import { useCallback, useEffect, useMemo, useState } from "react";

import {
    getDevices,
    getOpenAlerts,
    getAllMetrics,
    predictMetric,
} from "../services/api";

const REFRESH_INTERVAL = 30000;

function Dashboard() {
    const [devices, setDevices] = useState([]);
    const [alerts, setAlerts] = useState([]);
    const [metrics, setMetrics] = useState([]);
    const [predictions, setPredictions] = useState([]);

    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState("");
    const [lastUpdated, setLastUpdated] = useState(null);

    const loadDashboardData = useCallback(async (isRefresh = false) => {
        try {
            if (isRefresh) {
                setRefreshing(true);
            } else {
                setLoading(true);
            }

            setError("");

            const [deviceData, alertData, metricData] =
                await Promise.all([
                    getDevices(),
                    getOpenAlerts(),
                    getAllMetrics(),
                ]);

            const safeDevices = Array.isArray(deviceData)
                ? deviceData
                : [];

            const safeAlerts = Array.isArray(alertData)
                ? alertData
                : [];

            const safeMetrics = Array.isArray(metricData)
                ? metricData
                : [];

            setDevices(safeDevices);
            setAlerts(safeAlerts);
            setMetrics(safeMetrics);

            if (safeDevices.length > 0) {
                const devicePredictions = await Promise.all(
                    safeDevices.map(async (device) => {
                        const deviceMetrics = safeMetrics
                            .filter(
                                (metric) =>
                                    Number(metric.deviceId) ===
                                    Number(device.id)
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
                                status: "UNKNOWN",
                                anomalyScore: null,
                            };
                        }

                        const latestMetric = deviceMetrics[0];

                        try {
                            const predictionData =
                                await predictMetric({
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
                                status:
                                    predictionData?.status ||
                                    "UNKNOWN",
                                anomalyScore:
                                    predictionData?.anomalyScore ??
                                    null,
                            };
                        } catch (predictionError) {
                            console.error(
                                `Prediction failed for ${device.name}:`,
                                predictionError
                            );

                            return {
                                deviceId: device.id,
                                deviceName: device.name,
                                status: "ERROR",
                                anomalyScore: null,
                            };
                        }
                    })
                );

                setPredictions(devicePredictions);
            } else {
                setPredictions([]);
            }

            setLastUpdated(new Date());
        } catch (err) {
            console.error(
                "Failed to load dashboard data:",
                err
            );

            setError("Unable to connect to backend");

            if (!isRefresh) {
                setDevices([]);
                setAlerts([]);
                setMetrics([]);
                setPredictions([]);
            }
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, []);

    useEffect(() => {
        loadDashboardData(false);

        const refreshTimer = setInterval(() => {
            loadDashboardData(true);
        }, REFRESH_INTERVAL);

        return () => {
            clearInterval(refreshTimer);
        };
    }, [loadDashboardData]);

    const deviceStats = useMemo(() => {
        const healthy = devices.filter(
            (device) =>
                device.status?.toUpperCase() === "HEALTHY"
        ).length;

        const warning = devices.filter(
            (device) =>
                device.status?.toUpperCase() === "WARNING"
        ).length;

        const critical = devices.filter(
            (device) =>
                device.status?.toUpperCase() === "CRITICAL"
        ).length;

        const healthScores = devices
            .map((device) => Number(device.healthScore))
            .filter((score) => !Number.isNaN(score));

        const averageHealth =
            healthScores.length > 0
                ? (
                    healthScores.reduce(
                        (total, score) => total + score,
                        0
                    ) / healthScores.length
                ).toFixed(2)
                : "0.00";

        return {
            total: devices.length,
            healthy,
            warning,
            critical,
            averageHealth,
        };
    }, [devices]);

    const predictionStats = useMemo(() => {
        const healthy = predictions.filter(
            (prediction) =>
                prediction.status?.toUpperCase() === "HEALTHY"
        ).length;

        const warning = predictions.filter(
            (prediction) =>
                prediction.status?.toUpperCase() === "WARNING"
        ).length;

        const critical = predictions.filter(
            (prediction) =>
                prediction.status?.toUpperCase() === "CRITICAL"
        ).length;

        return {
            total: predictions.length,
            healthy,
            warning,
            critical,
        };
    }, [predictions]);

    const latestMetric = useMemo(() => {
        if (!metrics.length) {
            return {
                cpuUsage: null,
                memoryUsage: null,
                networkUsage: null,
            };
        }

        const sortedMetrics = [...metrics].sort(
            (a, b) =>
                new Date(b.timestamp) -
                new Date(a.timestamp)
        );

        return sortedMetrics[0];
    }, [metrics]);

    const cpuUsage =
        latestMetric.cpuUsage != null
            ? Number(latestMetric.cpuUsage)
            : null;

    const memoryUsage =
        latestMetric.memoryUsage != null
            ? Number(latestMetric.memoryUsage)
            : null;

    const networkUsage =
        latestMetric.networkUsage != null
            ? Number(latestMetric.networkUsage)
            : null;

    const highestPrediction = useMemo(() => {
        if (!predictions.length) {
            return null;
        }

        const priority = {
            CRITICAL: 3,
            WARNING: 2,
            ERROR: 1,
            HEALTHY: 1,
            UNKNOWN: 0,
        };

        return [...predictions].sort(
            (a, b) =>
                (priority[b.status?.toUpperCase()] || 0) -
                (priority[a.status?.toUpperCase()] || 0)
        )[0];
    }, [predictions]);

    const aiRecommendation = useMemo(() => {
        if (!predictions.length) {
            return {
                title: "Waiting for AI predictions",
                message:
                    "The monitoring data is being analyzed by the ML engine.",
            };
        }

        if (predictionStats.critical > 0) {
            return {
                title: "Critical condition detected",
                message:
                    `${predictionStats.critical} device(s) require immediate investigation. Check system resources, network conditions and active alerts.`,
            };
        }

        if (predictionStats.warning > 0) {
            return {
                title: "Potential anomalies detected",
                message:
                    `${predictionStats.warning} device(s) are showing warning-level behavior. Monitor CPU, memory, disk, latency and packet loss.`,
            };
        }

        return {
            title: "Infrastructure appears stable",
            message:
                "The AI model did not detect significant abnormal behavior across the monitored devices.",
        };
    }, [predictions, predictionStats]);

    const getStatusClass = (status) => {
        const normalizedStatus = status?.toUpperCase();

        if (normalizedStatus === "HEALTHY") {
            return "healthy";
        }

        if (normalizedStatus === "WARNING") {
            return "warning";
        }

        if (normalizedStatus === "CRITICAL") {
            return "critical";
        }

        if (normalizedStatus === "ERROR") {
            return "error";
        }

        return "unknown";
    };

    const getAnomalyPercentage = (score) => {
        if (score == null || Number.isNaN(Number(score))) {
            return 0;
        }

        const numericScore = Number(score);

        if (numericScore <= 1) {
            return Math.min(Math.max(numericScore * 100, 0), 100);
        }

        return Math.min(Math.max(numericScore, 0), 100);
    };

    const formattedLastUpdated = lastUpdated
        ? lastUpdated.toLocaleTimeString()
        : "--";

    return (
        <div className="dashboard">
            <header className="topbar">
                <div>
                    <h1>AIOps Network Monitoring</h1>
                    <p>
                        Real-time infrastructure monitoring
                        and intelligent operations
                    </p>
                </div>

                <div className="system-status">
                    <span className="status-dot"></span>
                    System Online
                </div>
            </header>

            {loading && (
                <div className="api-status">
                    Connecting to backend and AI engine...
                </div>
            )}

            {error && (
                <div className="api-error">
                    {error}
                </div>
            )}

            {!loading && !error && (
                <div className="api-success">
                    Backend connected — {devices.length} devices,{" "}
                    {alerts.length} active alerts,{" "}
                    {metrics.length} metrics and{" "}
                    {predictionStats.total} AI predictions generated
                </div>
            )}

            <section className="overview-grid">
                <div className="stat-card">
                    <div className="stat-icon">
                        <Server size={24} />
                    </div>

                    <div>
                        <span>Total Devices</span>
                        <strong>{deviceStats.total}</strong>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-icon healthy">
                        <HeartPulse size={24} />
                    </div>

                    <div>
                        <span>Healthy Devices</span>
                        <strong>{deviceStats.healthy}</strong>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-icon warning">
                        <AlertTriangle size={24} />
                    </div>

                    <div>
                        <span>Active Alerts</span>
                        <strong>{alerts.length}</strong>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-icon ai">
                        <Brain size={24} />
                    </div>

                    <div>
                        <span>AI Predictions</span>
                        <strong>{predictionStats.total}</strong>

                        {predictionStats.total > 0 && (
                            <small>
                                {predictionStats.critical > 0
                                    ? `${predictionStats.critical} critical`
                                    : predictionStats.warning > 0
                                        ? `${predictionStats.warning} warning`
                                        : "All stable"}
                            </small>
                        )}
                    </div>
                </div>
            </section>

            <section className="dashboard-grid">
                <div className="dashboard-card large">
                    <div className="card-header">
                        <div>
                            <h2>Network Health</h2>
                            <p>
                                Overall infrastructure health status
                            </p>
                        </div>

                        <Activity size={22} />
                    </div>

                    <div className="health-display">
                        <div className="health-score">
                            {deviceStats.averageHealth}%
                        </div>

                        <div className="health-label">
                            Overall Health Score
                        </div>
                    </div>
                </div>

                <div className="dashboard-card">
                    <div className="card-header">
                        <div>
                            <h2>System Metrics</h2>
                            <p>
                                Current resource utilization
                            </p>
                        </div>

                        <Cpu size={22} />
                    </div>

                    <div className="metric-item">
                        <div>
                            <span>CPU Usage</span>

                            <strong>
                                {cpuUsage != null
                                    ? `${cpuUsage.toFixed(2)}%`
                                    : "--"}
                            </strong>
                        </div>

                        <div className="progress">
                            <div
                                className="progress-bar"
                                style={{
                                    width:
                                        cpuUsage != null
                                            ? `${Math.min(
                                                Math.max(
                                                    cpuUsage,
                                                    0
                                                ),
                                                100
                                            )}%`
                                            : "0%",
                                }}
                            ></div>
                        </div>
                    </div>

                    <div className="metric-item">
                        <div>
                            <span>Memory Usage</span>

                            <strong>
                                {memoryUsage != null
                                    ? `${memoryUsage.toFixed(2)}%`
                                    : "--"}
                            </strong>
                        </div>

                        <div className="progress">
                            <div
                                className="progress-bar"
                                style={{
                                    width:
                                        memoryUsage != null
                                            ? `${Math.min(
                                                Math.max(
                                                    memoryUsage,
                                                    0
                                                ),
                                                100
                                            )}%`
                                            : "0%",
                                }}
                            ></div>
                        </div>
                    </div>

                    <div className="metric-item">
                        <div>
                            <span>Network Usage</span>

                            <strong>
                                {networkUsage != null
                                    ? `${networkUsage.toFixed(2)}%`
                                    : "--"}
                            </strong>
                        </div>

                        <div className="progress">
                            <div
                                className="progress-bar"
                                style={{
                                    width:
                                        networkUsage != null
                                            ? `${Math.min(
                                                Math.max(
                                                    networkUsage,
                                                    0
                                                ),
                                                100
                                            )}%`
                                            : "0%",
                                }}
                            ></div>
                        </div>
                    </div>
                </div>

                <div className="dashboard-card">
                    <div className="card-header">
                        <div>
                            <h2>Device Status</h2>
                            <p>
                                Infrastructure availability
                            </p>
                        </div>

                        <Network size={22} />
                    </div>

                    <div className="device-status">
                        <div>
                            <ShieldCheck size={20} />
                            <span>Healthy</span>
                            <strong>{deviceStats.healthy}</strong>
                        </div>

                        <div>
                            <AlertTriangle size={20} />
                            <span>Warning</span>
                            <strong>{deviceStats.warning}</strong>
                        </div>

                        <div>
                            <Wrench size={20} />
                            <span>Critical</span>
                            <strong>{deviceStats.critical}</strong>
                        </div>
                    </div>
                </div>

                <div className="dashboard-card large">
                    <div className="card-header">
                        <div>
                            <h2>AI Recommendations</h2>
                            <p>
                                Intelligent insights from the
                                monitoring system
                            </p>
                        </div>

                        <Brain size={22} />
                    </div>

                    <div className="recommendation">
                        <Brain size={20} />

                        <div>
                            <strong>
                                {highestPrediction
                                    ? `AI Prediction: ${highestPrediction.status}`
                                    : "AI Prediction: --"}
                            </strong>

                            <p>
                                {highestPrediction
                                    ? `Highest priority device: ${highestPrediction.deviceName}`
                                    : "No AI prediction is currently available."}
                            </p>
                        </div>
                    </div>

                    <div className="recommendation">
                        <Activity size={20} />

                        <div>
                            <strong>
                                {aiRecommendation.title}
                            </strong>

                            <p>
                                {aiRecommendation.message}
                            </p>
                        </div>
                    </div>
                </div>
            </section>

            <section className="dashboard-card prediction-section">
                <div className="card-header">
                    <div>
                        <h2>AI Device Predictions</h2>
                        <p>
                            Latest ML prediction for each monitored device
                        </p>
                    </div>

                    <Brain size={22} />
                </div>

                <div className="prediction-list">
                    {predictions.length === 0 ? (
                        <div className="recommendation">
                            <Brain size={20} />

                            <div>
                                <strong>
                                    No predictions available
                                </strong>

                                <p>
                                    Device predictions will appear when
                                    monitoring metrics are available.
                                </p>
                            </div>
                        </div>
                    ) : (
                        predictions.map((item) => {
                            const statusClass =
                                getStatusClass(item.status);

                            const anomalyPercentage =
                                getAnomalyPercentage(
                                    item.anomalyScore
                                );

                            return (
                                <div
                                    className={`prediction-card ${statusClass}`}
                                    key={item.deviceId}
                                >
                                    <div className="recommendation">
                                        <Brain size={20} />

                                        <div>
                                            <strong>
                                                {item.deviceName}

                                                <span
                                                    className={`prediction-status ${statusClass}`}
                                                >
                                                    {item.status}
                                                </span>
                                            </strong>

                                            <p>
                                                Latest AI prediction
                                                generated from the
                                                device's monitoring
                                                metrics.
                                            </p>
                                        </div>
                                    </div>

                                    <div className="anomaly-score">
                                        <div className="anomaly-score-header">
                                            <span>
                                                Anomaly Score
                                            </span>

                                            <span className="anomaly-score-value">
                                                {item.anomalyScore != null
                                                    ? Number(
                                                        item.anomalyScore
                                                    ).toFixed(2)
                                                    : "--"}
                                            </span>
                                        </div>

                                        <div className="anomaly-progress">
                                            <div
                                                className={`anomaly-progress-bar ${statusClass}`}
                                                style={{
                                                    width: `${anomalyPercentage}%`,
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

            <div className="dashboard-refresh">
                <div>
                    <RefreshCw
                        size={15}
                        className={refreshing ? "refresh-spinning" : ""}
                    />

                    <span>
                        {refreshing
                            ? "Updating monitoring data..."
                            : `Last updated: ${formattedLastUpdated}`}
                    </span>
                </div>

                <span>
                    Auto-refresh: 30 seconds
                </span>
            </div>
        </div>
    );
}

export default Dashboard;