from dataclasses import dataclass
import json
import bisect
from typing import List, Dict

SAMPLE_OUTPUT_LOG_FILE = "sample_one_device_simulation_log"


@dataclass
class LogEntry:
    timestamp: int
    position: List[float]

    def __lt__(self, other):
        return self.timestamp < other.timestamp


class SimulationLog:
    def __init__(self):
        self.devices_histories: Dict[str, OneDeviceSimulationLog] = {}

    def put(self, device_id, timestamp, position):
        if device_id not in self.devices_histories:
            self.devices_histories[device_id] = OneDeviceSimulationLog(device_id)
        self.devices_histories[device_id].append(timestamp, position)

    def __iter__(self):
        return iter(self.devices_histories)

    def __getitem__(self, key):
        return self.devices_histories.get(key)

    def items(self):
        return self.devices_histories.items()


class OneDeviceSimulationLog:
    def __init__(self, device_id):
        self.device_id = device_id
        self.history: List[LogEntry] = []

    def append(self, timestamp, position):
        bisect.insort(self.history, LogEntry(timestamp, position))

    def get_id(self):
        return self.device_id

    def get_history(self):
        return self.history

    def __str__(self):
        return f"Device log for {self.device_id}: {str(self.history[:5])} ... and more"


def parse_one_line(line):
    data = json.loads(line)
    device_id = data['id']
    sent_time = int(data['time'])
    device_pos = data['coordinates']
    return device_id, sent_time, device_pos


def generate_simulation_log_from_file(filename):
    simulation_log = SimulationLog()
    with open(filename) as file:
        content = file.readlines()
        for line in content:
            device_id, sent_time, device_pos = parse_one_line(line)
            simulation_log.put(device_id, sent_time, device_pos)
    return simulation_log


def main():
    simulation_log = generate_simulation_log_from_file(SAMPLE_OUTPUT_LOG_FILE)
    print(simulation_log)


if __name__ == '__main__':
    main()
