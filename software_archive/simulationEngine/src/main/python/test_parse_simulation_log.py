import unittest
import parseSimulationLog

SAMPLE_OUTPUT_LOG_FILE = parseSimulationLog.SAMPLE_OUTPUT_LOG_FILE


class TestDataStructure(unittest.TestCase):
    def test_can_include_logs_for_one_device(self):
        device_id = "AWS-1000"
        one_device_log = parseSimulationLog.OneDeviceSimulationLog(device_id)
        self.assertEqual(device_id, one_device_log.get_id())

        num_entries = 10
        base_timestamp = 1634131646020
        base_position = [-86.77440982202154, 36.129015825214594]
        timestamps = [base_timestamp + 100 * i for i in range(num_entries)]
        device_positions = [[base_position[0] + 0.1 * i, base_position[1]] for i in range(num_entries)]
        for timestamp, device_pos in zip(timestamps, device_positions):
            one_device_log.append(timestamp, device_pos)

        history = one_device_log.get_history()
        self.assertListEqual(
            list(
                map(lambda p: parseSimulationLog.LogEntry(p[0], p[1]),
                    zip(timestamps, device_positions))),
            history)

    def test_one_device_history_is_in_chronological_order(self):
        device_id = "AWS-1000"
        one_device_log = parseSimulationLog.OneDeviceSimulationLog(device_id)

        num_entries = 20
        base_timestamp = 1634131646020
        base_position = [-86.77440982202154, 36.129015825214594]
        timestamps_1 = [base_timestamp + 100 * i for i in range(num_entries // 2)]
        timestamps_2 = [base_timestamp + 100 * i + 50 for i in range(num_entries // 2)]
        device_positions_1 = [[base_position[0] + 0.1 * i, base_position[1]] for i in range(num_entries // 2)]
        device_positions_2 = [[base_position[0] + 0.05 * i, base_position[1]] for i in range(num_entries // 2)]
        for timestamp, device_pos in zip(timestamps_1, device_positions_1):
            one_device_log.append(timestamp, device_pos)
        for timestamp, device_pos in zip(timestamps_2, device_positions_2):
            one_device_log.append(timestamp, device_pos)
        history = one_device_log.get_history()
        timestamps = list(map(lambda e: e.timestamp, history))
        result = all(map(lambda p: p[0] < p[1], zip(timestamps[0:], timestamps[1:])))
        self.assertTrue(result)
