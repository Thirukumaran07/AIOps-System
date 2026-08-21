from flask import Flask, request, jsonify

from model import AnomalyDetector


app = Flask(__name__)

detector = AnomalyDetector()


@app.route("/health", methods=["GET"])
def health():

    return jsonify({
        "status": "ML service is running"
    })


@app.route("/predict", methods=["POST"])
def predict():

    data = request.get_json()

    required_fields = [
        "cpuUsage",
        "memoryUsage",
        "diskUsage",
        "latency",
        "packetLoss"
    ]

    for field in required_fields:

        if field not in data:

            return jsonify({
                "error": f"Missing field: {field}"
            }), 400

    result = detector.predict(
        data["cpuUsage"],
        data["memoryUsage"],
        data["diskUsage"],
        data["latency"],
        data["packetLoss"]
    )

    return jsonify(result)


if __name__ == "__main__":

    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True
    )