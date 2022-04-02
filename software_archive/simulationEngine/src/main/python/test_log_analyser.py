import unittest
import analyse_sent_and_receive
import parseSimulationLog
from src.main.python import test_util_generate_logs


def get_receive_history_AWS_1234(base_timestamp, base_position):
    device_id = "AWS-1234"
    receive_log = parseSimulationLog.OneDeviceSimulationLog(device_id)

    num_entries = 10
    receive_times = [base_timestamp + 100 * i + 50 for i in range(num_entries)]
    receive_positions = [[base_position[0] + 0.01 * i, base_position[1]] for i in range(num_entries)]
    for timestamp, position in zip(receive_times, receive_positions):
        receive_log.append(timestamp, position)
    receive_history = receive_log.get_history()
    return receive_history


def get_sent_history_AWS_1234(base_timestamp, base_position):
    device_id = "AWS-1234"
    sent_log = parseSimulationLog.OneDeviceSimulationLog(device_id)

    num_entries = 10
    sent_times = [base_timestamp + 100 * i for i in range(num_entries)]
    sent_positions = [[base_position[0] + 0.01 * i, base_position[1]] for i in range(num_entries)]
    for timestamp, position in zip(sent_times, sent_positions):
        sent_log.append(timestamp, position)
    sent_history = sent_log.get_history()
    return sent_history


class TestHelperMethods(unittest.TestCase):
    def test_can_find_entry_in_receive_log(self):
        base_timestamp = 1634131646020
        base_position = [-86.77440982202154, 36.129015825214594]

        receive_history = get_receive_history_AWS_1234(base_timestamp, base_position)
        sent_history_entry = parseSimulationLog.LogEntry(base_timestamp + 100,
                                                         [base_position[0] + 0.01 * 1, base_position[1]])

        idx = analyse_sent_and_receive.try_search_entry(receive_history, sent_history_entry, 0)
        expected = 1
        self.assertEqual(expected, idx)

    def test_can_compute_differences_in_timestamp(self):
        base_timestamp = 1634131646020
        base_position = [-86.77440982202154, 36.129015825214594]

        receive_history = get_receive_history_AWS_1234(base_timestamp, base_position)
        sent_history = get_sent_history_AWS_1234(base_timestamp, base_position)
        time_diffs = analyse_sent_and_receive.compute_timestamp_differences_for_one_device(sent_history,
                                                                                           receive_history)
        expected = [50 for _ in range(10)]
        self.assertListEqual(expected, time_diffs)


class TestOverall(unittest.TestCase):
    def setUp(self) -> None:
        # generate the log files
        test_util_generate_logs.generate_sent_and_receive_logs()

    def test_analyse_generated_logs(self):
        sent_log_filename = test_util_generate_logs.GENERATED_SENT_LOGS
        receive_log_filename = test_util_generate_logs.GENERATED_RECEIVE_LOGS
        result = analyse_sent_and_receive.analyse_logs(sent_log_filename, receive_log_filename)
        for time_diffs in result.values():
            td = time_diffs[0]
            all_same = all(map(lambda td_: td == td_, time_diffs))
            self.assertTrue(all_same)
