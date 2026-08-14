import time
import psutil
import requests
from ping3 import ping

from config import (
    METRICS_ENDPOINT,
    DEVICE_ID,
    TARGET_IP,
    COLLECTION_INTERVAL
)


def get_cpu_usage():
    return psutil.cpu_percent(interval=1)


def get_memory_usage():
    memory = psutil.virtual_memory()
    return memory.percent


def get_disk_usage():
    disk = psutil.disk_usage("/")
    return disk.percent


def get_network_usage():
    network = psutil.net_io_counters()

    bytes_sent = network.bytes_sent
    bytes_received = network.bytes_recv

    total_bytes = bytes_sent + bytes_received

    # Convert to MB
    total_mb = total_bytes / (1024 * 1024)

    return round(total_mb, 2)


def get_latency():

    latency = ping(TARGET_IP, timeout=2)

    if latency is None:
        return 999.0

    return round(latency * 1000, 2)


def get_packet_loss():

    total_packets = 5
    failed_packets = 0

    for _ in range(total_packets):

        result = ping(TARGET_IP, timeout=2)

        if result is None:
            failed_packets += 1

    packet_loss = (failed_packets / total_packets) * 100

    return round(packet_loss, 2)


def collect_metrics():

    cpu = get_cpu_usage()

    memory = get_memory_usage()

    disk = get_disk_usage()

    network = get_network_usage()

    latency = get_latency()

    packet_loss = get_packet_loss()

    metrics = {
        "deviceId": DEVICE_ID,
        "cpuUsage": cpu,
        "memoryUsage": memory,
        "diskUsage": disk,
        "networkUsage": network,
        "latency": latency,
        "packetLoss": packet_loss
    }

    return metrics


def send_metrics(metrics):

    try:

        response = requests.post(
            METRICS_ENDPOINT,
            json=metrics,
            timeout=5
        )

        if response.status_code in [200, 201]:

            print("Metrics sent successfully")

            print(metrics)

        else:

            print(
                f"Failed to send metrics: "
                f"{response.status_code}"
            )

            print(response.text)

    except requests.exceptions.RequestException as e:

        print("Backend connection failed")

        print(e)


def main():

    print("===================================")
    print(" AIOps Monitoring Agent")
    print("===================================")

    print(f"Backend : {METRICS_ENDPOINT}")
    print(f"Device  : {DEVICE_ID}")
    print(f"Target  : {TARGET_IP}")
    print()

    while True:

        try:

            metrics = collect_metrics()

            print("-----------------------------------")

            print(f"CPU        : {metrics['cpuUsage']}%")
            print(f"Memory     : {metrics['memoryUsage']}%")
            print(f"Disk       : {metrics['diskUsage']}%")
            print(f"Network    : {metrics['networkUsage']} MB")
            print(f"Latency    : {metrics['latency']} ms")
            print(f"Packet Loss: {metrics['packetLoss']}%")

            send_metrics(metrics)

            print(
                f"Next collection in "
                f"{COLLECTION_INTERVAL} seconds..."
            )

            time.sleep(COLLECTION_INTERVAL)

        except KeyboardInterrupt:

            print("\nMonitoring agent stopped.")

            break

        except Exception as e:

            print("Unexpected error:", e)

            time.sleep(COLLECTION_INTERVAL)


if __name__ == "__main__":
    main()