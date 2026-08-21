import numpy as np
from sklearn.ensemble import IsolationForest


class AnomalyDetector:

    def __init__(self):

        self.model = IsolationForest(
            n_estimators=200,
            contamination=0.05,
            random_state=42
        )

        self.train_model()

    def train_model(self):

        np.random.seed(42)

        # Generate realistic NORMAL network/system metrics
        cpu = np.random.normal(35, 8, 1000)
        memory = np.random.normal(45, 8, 1000)
        disk = np.random.normal(50, 10, 1000)
        latency = np.random.normal(60, 15, 1000)
        packet_loss = np.random.normal(0.5, 0.5, 1000)

        # Keep values within realistic limits
        cpu = np.clip(cpu, 5, 70)
        memory = np.clip(memory, 10, 75)
        disk = np.clip(disk, 10, 80)
        latency = np.clip(latency, 10, 120)
        packet_loss = np.clip(packet_loss, 0, 3)

        normal_data = np.column_stack((
            cpu,
            memory,
            disk,
            latency,
            packet_loss
        ))

        self.model.fit(normal_data)

    def predict(
        self,
        cpu_usage,
        memory_usage,
        disk_usage,
        latency,
        packet_loss
    ):

        data = np.array([[
            cpu_usage,
            memory_usage,
            disk_usage,
            latency,
            packet_loss
        ]])

        prediction = self.model.predict(data)[0]

        anomaly_score = self.model.decision_function(data)[0]

        if prediction == -1:
            status = "ANOMALY"
        else:
            status = "NORMAL"

        return {
            "status": status,
            "anomalyScore": round(float(anomaly_score), 4)
        }